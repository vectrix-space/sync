/*
 * This file is part of sync, licensed under the MIT License.
 *
 * Copyright (c) 2025 vectrix.space
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package space.vectrix.sync.collections;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Represents a hash table capable of highly concurrent retrievals and updates.
 * It works similarly to {@link java.util.concurrent.ConcurrentHashMap} and
 * shares many of the same concepts. However, the main difference is with how
 * this map carefully manages two separate tables. One table is immutable
 * providing efficient retrieval and updates of existing and frequently
 * accessed key-value pairs that is fully lock-free. Whereas, the mutable table
 * allows new key-value pairs but may involve bucket-level locking.
 *
 * <p>This map is optimized for cases where updates and retrievals for existing
 * entries is a lot faster than recently added entries. However, the
 * performance in other cases are close or equal to that of {@link
 * java.util.concurrent.ConcurrentHashMap} which is faster than a traditional
 * map paired with a read and write lock, or maps with an exclusive lock (such
 * as using {@link Collections#synchronizedMap(Map)}) in similar scenarios.</p>
 *
 * <p>Null values or keys are not accepted.</p>
 *
 * @author vectrix
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.0.0
 */
public class BucketSyncMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Hash {

  /* --- < Constants > ----------------------------------------------------- */

  /**
   * Represents the maximum capacity for the underlying tables.
   */
  private static final int MAXIMUM_CAPACITY = 1 << 30;

  /**
   * Represents the maximum number of threads that can participate in a
   * transfer operation.
   */
  private static final int MAXIMUM_TRANSFER_THREADS = 16;

  /**
   * Represents the minimum transfer stride for transferring batches of
   * nodes per thread.
   */
  private static final int MINIMUM_TRANSFER_STRIDE = 16;

  /**
   * The bitmask applied to ensure hash values are non-negative and fit within
   * the usable range of 32-bit signed integers.
   */
  private static final int HASH_BITS = 0x7FFFFFFF;

  /**
   * Represents the hash for a node that has been transferred.
   */
  private static final int NODE_MOVED = -1;

  /**
   * Represents a sentinel value for an expunged object reference.
   */
  private static final Object EXPUNGED = new Object();

  /**
   * Represents the maximum number of processors for transfer size limits.
   */
  private static final int NCPU = Runtime.getRuntime().availableProcessors();

  /**
   * A random seed used to introduce variability into hash computations,
   * reducing predictability and the likelihood of collision attacks.
   */
  private static final int HASH_SEED = ThreadLocalRandom.current().nextInt();

  /* --- < Utilities > ----------------------------------------------------- */

  /**
   * Returns the optimal table size depending on the given capacity.
   *
   * @param value the current size
   * @return the new size
   */
  private static int tableSizeFor(final int value) {
    int length = value - 1;
    length |= length >>> 1;
    length |= length >>> 2;
    length |= length >>> 4;
    length |= length >>> 8;
    length |= length >>> 16;
    return length <= 0 ? 1 : (length >= BucketSyncMap.MAXIMUM_CAPACITY ? BucketSyncMap.MAXIMUM_CAPACITY : length + 1);
  }

  /* --- < Reflection > ---------------------------------------------------- */

  /**
   * Provides atomic operations for {@link Node} tables.
   */
  private static final VarHandle NODE_ARRAY;

  /**
   * Provides atomic operations for {@link BucketSyncMap#transferIndex}.
   */
  private static final VarHandle TRANSFER_INDEX;

  /**
   * Provides atomic operations for {@link BucketSyncMap#transferProgress}.
   */
  private static final VarHandle TRANSFER_PROGRESS;

  static {
    try {
      final MethodHandles.Lookup lookup = MethodHandles.lookup();

      NODE_ARRAY = MethodHandles.arrayElementVarHandle(Node[].class);
      TRANSFER_INDEX = lookup.findVarHandle(BucketSyncMap.class, "transferIndex", int.class);
      TRANSFER_PROGRESS = lookup.findVarHandle(BucketSyncMap.class, "transferProgress", int.class);
    } catch(final Exception exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  /* --- < Spread > -------------------------------------------------------- */

  /**
   * A hash spread function optimized for raw speed with minimal transformation.
   * Provides adequate distribution for small or moderate-sized maps but may
   * degrade performance with large datasets or many similar keys.
   */
  public static final SpreadFunction FASTEST_SPREAD = x -> (x ^ (x >>> 16)) & BucketSyncMap.HASH_BITS;

  /**
   * A hash spread function that balances performance and moderate hash
   * distribution. Suitable for medium-sized maps, but may still degrade under
   * high key similarity or very large data sets.
   */
  public static final SpreadFunction FAST_SPREAD = x -> {
    x ^= (x >>> 16);
    x ^= (x >>> 13);
    return x & BucketSyncMap.HASH_BITS;
  };

  /**
   * A hash spread function that emphasizes stronger distribution while
   * maintaining good performance. Incorporates {@link #HASH_SEED} to randomize
   * results and reduce vulnerability to hash collision attacks.
   */
  public static final SpreadFunction BALANCED_SPREAD = x -> {
    x ^= BucketSyncMap.HASH_SEED;
    x ^= (x >>> 16);
    x ^= (x >>> 13);
    return x & BucketSyncMap.HASH_BITS;
  };

  /* --- < Table > --------------------------------------------------------- */

  /**
   * Returns the {@link Node} at the given {@code index}, if present, otherwise
   * {@code null}.
   *
   * @param table the node array
   * @param index the index
   * @param <K> the key type
   * @param <V> the value type
   * @return the node, or null
   */
  @SuppressWarnings("unchecked")
  private static <K, V> @Nullable Node<K, V> getNodePlain(final Node<K, V>[] table, final int index) {
    return (Node<K, V>) BucketSyncMap.NODE_ARRAY.get(table, index);
  }

  /**
   * Returns the {@link Node} at the given {@code index}, if present, otherwise
   * {@code null}.
   *
   * @param table the node array
   * @param index the index
   * @param <K> the key type
   * @param <V> the value type
   * @return the node, or null
   */
  @SuppressWarnings("unchecked")
  private static <K, V> @Nullable Node<K, V> getNode(final Node<K, V>[] table, final int index) {
    return (Node<K, V>) BucketSyncMap.NODE_ARRAY.getAcquire(table, index);
  }

  /**
   * Atomically compare and swaps the {@link Node} at the given {@code index}
   * from the {@code oldNode} to the {@code newNode}.
   *
   * @param <K>      the key type
   * @param <V>      the value type
   * @param table    the node array
   * @param index    the index
   * @param nextNode the new node
   * @return true if new node was set, otherwise false
   */
  private static <K, V> boolean replaceNode(final Node<K, V>[] table, final int index, final @Nullable Node<K, V> nextNode) {
    return BucketSyncMap.NODE_ARRAY.compareAndExchangeRelease(table, index, (Node<K, V>) null, nextNode) == null;
  }

  /**
   * Sets the given {@link Node} at the given {@code index}.
   *
   * @param table the node array
   * @param index the index
   * @param node the node
   * @param <K> the key type
   * @param <V> the value type
   */
  private static <K, V> void setNode(final Node<K, V>[] table, final int index, final @Nullable Node<K, V> node) {
    BucketSyncMap.NODE_ARRAY.setRelease(table, index, node);
  }

  /* --- < Fields > -------------------------------------------------------- */

  /**
   * Represents the spread function for distributing hashes in the map.
   */
  private final transient SpreadFunction spreadFunction;

  /**
   * Represents the load factor for resizing the map.
   */
  private final transient float loadFactor;

  /**
   * Represents an immutable hash table that allows fast retrieval and updates
   * without locking.
   */
  private transient volatile Node<K, V>[] immutableTable;

  /**
   * Represents a mutable hash table that allows updates of new nodes with
   * locking.
   */
  private transient volatile Node<K, V>@Nullable [] mutableTable;

  /**
   * Represents a temporary transfer hash table, used in transfer operations.
   */
  private transient volatile Node<K, V>@Nullable [] transferTable;

  /**
   * Represents whether the mutable table has been amended by the immutable
   * table.
   */
  private transient volatile boolean amended;

  /**
   * Represents the capacity of the mutable table and initially the length to
   * initialize the table at.
   */
  private transient volatile int capacity;

  /**
   * Represents the transfer index a thread may claim a range of when
   * participating in a transfer operation.
   */
  private transient int transferIndex;

  /**
   * Represents the transfer progress threads will add completed ranges to.
   */
  private transient int transferProgress;

  /**
   * Represents the stamp lock for the bulk operations on the table.
   */
  private transient final StampLock stampLock = new StampLock();

  /**
   * Represents the amount of times the immutable cache has been missed for
   * the mutable table.
   */
  private transient final LongAdder misses = new LongAdder();

  /**
   * Represents the amount of elements in this map, maintained by the
   * thread-safe {@link LongAdder}.
   */
  private transient final LongAdder size = new LongAdder();

  /**
   * Represents a view of the entries in this map.
   */
  private transient volatile @Nullable EntrySet entrySet;

  /* --- < Public Operations > --------------------------------------------- */

  /**
   * Initializes a new {@link BucketSyncMap} with {@link BucketSyncMap#FAST_SPREAD},
   * {@link BucketSyncMap#DEFAULT_CAPACITY} and {@link BucketSyncMap#DEFAULT_LOAD_FACTOR}.
   *
   * @since 1.0.0
   */
  public BucketSyncMap() {
    this(BucketSyncMap.FAST_SPREAD);
  }

  /**
   * Initializes a new {@link BucketSyncMap} with the given spread function,
   * {@link BucketSyncMap#DEFAULT_CAPACITY} and {@link BucketSyncMap#DEFAULT_LOAD_FACTOR}.
   *
   * @param spreadFunction the spread function
   * @since 1.0.0
   */
  public BucketSyncMap(final SpreadFunction spreadFunction) {
    this(spreadFunction, BucketSyncMap.DEFAULT_CAPACITY);
  }

  /**
   * Initializes a new {@link BucketSyncMap} with {@link BucketSyncMap#FAST_SPREAD}, the
   * given initial capacity and {@link BucketSyncMap#DEFAULT_LOAD_FACTOR}.
   *
   * @param initialCapacity the initial capacity
   * @since 1.0.0
   */
  public BucketSyncMap(final int initialCapacity) {
    this(BucketSyncMap.FAST_SPREAD, initialCapacity);
  }

  /**
   * Initializes a new {@link BucketSyncMap} with the given spread function, the given
   * initial capacity and {@link BucketSyncMap#DEFAULT_LOAD_FACTOR}.
   *
   * @param spreadFunction the spread function
   * @param initialCapacity the initial capacity
   * @since 1.0.0
   */
  public BucketSyncMap(final SpreadFunction spreadFunction, final int initialCapacity) {
    this(spreadFunction, initialCapacity, BucketSyncMap.DEFAULT_LOAD_FACTOR);
  }

  /**
   * Initializes a new {@link BucketSyncMap} with {@link BucketSyncMap#FAST_SPREAD}, the
   * given initial capacity and the given load factor.
   *
   * @param initialCapacity the initial capacity
   * @param loadFactor the load factor
   * @since 1.0.0
   */
  public BucketSyncMap(final int initialCapacity, final float loadFactor) {
    this(BucketSyncMap.FAST_SPREAD, initialCapacity, loadFactor);
  }

  /**
   * Initializes a new {@link BucketSyncMap} with the given spread function, the given
   * initial capacity and the given load factor.
   *
   * @param spreadFunction the spread function
   * @param initialCapacity the initial capacity
   * @param loadFactor the load factor
   * @since 1.0.0
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public BucketSyncMap(final SpreadFunction spreadFunction, final int initialCapacity, final float loadFactor) {
    requireNonNull(spreadFunction, "spreadFunction");

    if(initialCapacity < 0) throw new IllegalArgumentException("Initial capacity must be non-negative");
    if(loadFactor <= 0.0f || !Float.isFinite(loadFactor)) {
      throw new IllegalArgumentException("Load factor must be positive and finite");
    }

    final int capacity = initialCapacity >= BucketSyncMap.MAXIMUM_CAPACITY
      ? BucketSyncMap.MAXIMUM_CAPACITY
      : BucketSyncMap.tableSizeFor(initialCapacity);

    this.spreadFunction = spreadFunction;
    this.loadFactor = loadFactor;
    this.capacity = capacity;

    this.immutableTable = new Node[0];
  }

  @Override
  public int size() {
    final long sum = this.size.sum();
    return sum < 0L
      ? 0
      : (sum > (long) Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum);
  }

  @Override
  public boolean isEmpty() {
    return this.size.sum() <= 0L;
  }

  @Override
  public boolean containsKey(final Object key) {
    requireNonNull(key, "key");

    Node<K, V>@UnknownNullability [] table = this.immutableTable;
    int length = table.length;
    Node<K, V> node;

    final int hash = this.spreadFunction.spread(key.hashCode());

    K nodeKey;
    if(length > 0 && (node = BucketSyncMap.getNode(table, (length - 1) & hash)) != null) {
      if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
        return node.referencePlain().valueExists();
      }

      while((node = node.nextPlain()) != null) {
        if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
          return node.referencePlain().valueExists();
        }
      }
    }

    if(this.amended && (table = this.mutableTable) != null && (length = table.length) > 0) {
      boolean exists = false;
      retry: if((node = BucketSyncMap.getNode(table, (length - 1) & hash)) != null) {
        final int nodeHash;
        if((nodeHash = node.hash) == hash) {
          if((nodeKey = node.key) == key || nodeKey.equals(key)) {
            exists = node.reference().valueExists();
            break retry;
          }
        } else if(nodeHash < 0) {
          final Node<K, V> nextNode;
          if((nextNode = node.find(hash, key)) != null) {
            exists = nextNode.reference().valueExists();
            break retry;
          }
        }

        while((node = node.next()) != null) {
          if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
            exists = node.reference().valueExists();
            break retry;
          }
        }
      }

      this.miss();
      return exists;
    }

    return false;
  }

  @Override
  public @Nullable V get(final Object key) {
    requireNonNull(key, "key");

    Node<K, V>@UnknownNullability [] table = this.immutableTable;
    int length = table.length;
    Node<K, V> node;

    final int hash = this.spreadFunction.spread(key.hashCode());

    K nodeKey;
    if(length > 0 && (node = BucketSyncMap.getNode(table, (length - 1) & hash)) != null) {
      if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
        return node.referencePlain().value();
      }

      while((node = node.nextPlain()) != null) {
        if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
          return node.referencePlain().value();
        }
      }
    }

    if(this.amended && (table = this.mutableTable) != null && (length = table.length) > 0) {
      V value = null;
      retry: if((node = BucketSyncMap.getNode(table, (length - 1) & hash)) != null) {
        final int nodeHash;
        if((nodeHash = node.hash) == hash) {
          if((nodeKey = node.key) == key || nodeKey.equals(key)) {
            value = node.reference().value();
            break retry;
          }
        } else if(nodeHash < 0) {
          final Node<K, V> nextNode;
          if((nextNode = node.find(hash, key)) != null) {
            value = nextNode.reference().value();
            break retry;
          }
        }

        while((node = node.next()) != null) {
          if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
            value = node.reference().value();
            break retry;
          }
        }
      }

      this.miss();
      return value;
    }

    return null;
  }

  @Override
  public @Nullable V getOrDefault(final Object key, final @Nullable V defaultValue) {
    requireNonNull(key, "key");

    Node<K, V>@UnknownNullability [] table = this.immutableTable;
    int length = table.length;
    Node<K, V> node;

    final int hash = this.spreadFunction.spread(key.hashCode());

    K nodeKey;
    if(length > 0 && (node = BucketSyncMap.getNode(table, (length - 1) & hash)) != null) {
      if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
        return node.referencePlain().valueOr(defaultValue);
      }

      while((node = node.nextPlain()) != null) {
        if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
          return node.referencePlain().valueOr(defaultValue);
        }
      }
    }

    if(this.amended && (table = this.mutableTable) != null && (length = table.length) > 0) {
      V value = defaultValue;
      retry: if((node = BucketSyncMap.getNode(table, (length - 1) & hash)) != null) {
        final int nodeHash;
        if((nodeHash = node.hash) == hash) {
          if((nodeKey = node.key) == key || nodeKey.equals(key)) {
            value = node.reference().valueOr(defaultValue);
            break retry;
          }
        } else if(nodeHash < 0) {
          final Node<K, V> nextNode;
          if((nextNode = node.find(hash, key)) != null) {
            value = nextNode.reference().valueOr(defaultValue);
            break retry;
          }
        }

        while((node = node.next()) != null) {
          if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
            value = node.reference().valueOr(defaultValue);
            break retry;
          }
        }
      }

      this.miss();
      return value;
    }

    return defaultValue;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Under contention, the {@code mappingFunction} may be invoked more than
   * once for the same key; only one result will be stored. The function should
   * therefore be idempotent and free of externally visible side effects, and
   * it must not attempt to update this map.</p>
   *
   * <p>If the function throws, the exception is propagated and the mapping for
   * the key remains unchanged.</p>
   */
  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V computeIfAbsent(final K key, final Function<? super K, ? extends @Nullable V> mappingFunction) {
    requireNonNull(key, "key");
    requireNonNull(mappingFunction, "mappingFunction");

    V next;

    Node<K, V>[] immutable;
    Node<K, V>@org.jetbrains.annotations.Nullable [] mutable = null;
    int length;
    Node<K, V> node;

    final int hash = this.spreadFunction.spread(key.hashCode());

    K nodeKey;
    retry: for(; ; ) {
      immutable = this.immutableTable;
      length = immutable.length;

      if(length > 0) {
        // Find the node from the immutable table and update the value if the
        // node is present and the value is null or expunged.
        node = BucketSyncMap.getNode(immutable, (length - 1) & hash);
        while(node != null) {
          if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
            node = node.next();
            continue;
          }

          final ObjectReference reference = node.reference();
          Object previous = reference.get();
          for(; ; ) {
            if(previous != null && previous != BucketSyncMap.EXPUNGED) return (V) previous;

            next = mappingFunction.apply(key);
            if(next == null) return null;

            final Object witness = reference.compareAndExchange(previous, next);
            if(witness != previous) {
              previous = witness;
              Thread.onSpinWait();
              continue;
            }

            if(witness == BucketSyncMap.EXPUNGED) {
              this.amendNode(hash, key, reference);
            }

            break retry;
          }
        }
      }

      final int index;
      if(!this.amended || (mutable == null && (mutable = this.mutableTable) == null)) {
        if(length != 0 || (mutable = this.initialize()) != null) {
          this.amend();
        }

        Thread.onSpinWait();
      } else if((node = BucketSyncMap.getNode(mutable, index = (mutable.length - 1) & hash)) == null) {
        next = mappingFunction.apply(key);
        if(next == null) return null;

        if(BucketSyncMap.replaceNode(mutable, index, new Node<>(hash, key, new ObjectReference(next)))) {
          break;
        }

        Thread.onSpinWait();
      } else if(node.hash == BucketSyncMap.NODE_MOVED) {
        mutable = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(BucketSyncMap.getNodePlain(mutable, index) == node) {
            for(; ; ) {
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.referencePlain();
                Object previous = reference.get();
                for(; ; ) {
                  if(previous != null && previous != BucketSyncMap.EXPUNGED) return (V) previous;

                  next = mappingFunction.apply(key);
                  if(next == null) return null;

                  final Object witness = reference.compareAndExchange(previous, next);
                  if(witness != previous) {
                    previous = witness;
                    Thread.onSpinWait();
                    continue;
                  }

                  break retry;
                }
              }

              final Node<K, V> previousNode = node;
              if((node = node.nextPlain()) == null) {
                next = mappingFunction.apply(key);
                if(next == null) return null;

                previousNode.next(new Node<>(hash, key, new ObjectReference(next)));
                break retry;
              }
            }
          }
        }
      }
    }

    this.addCount(1L);
    return next;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Under contention, the {@code remappingFunction} may be invoked more than
   * once for the same key; only one result will be stored. The function should
   * therefore be idempotent and free of externally visible side effects, and
   * it must not attempt to update this map.</p>
   *
   * <p>If the function throws, the exception is propagated and the mapping for
   * the key remains unchanged.</p>
   */
  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends @Nullable V> remappingFunction) {
    requireNonNull(key, "key");
    requireNonNull(remappingFunction, "remappingFunction");

    V next;
    long count = 0L;

    Node<K, V>[] immutable;
    Node<K, V>@org.jetbrains.annotations.Nullable [] mutable = null;
    int length;
    Node<K, V> node;

    final int hash = this.spreadFunction.spread(key.hashCode());

    K nodeKey;
    retry: for(; ; ) {
      immutable = this.immutableTable;
      length = immutable.length;

      if(length > 0) {
        // Find the node from the immutable table and update the value if the
        // node is present.
        node = BucketSyncMap.getNode(immutable, (length - 1) & hash);
        while(node != null) {
          if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
            node = node.next();
            continue;
          }

          final ObjectReference reference = node.reference();
          Object previous = reference.get();
          for(; ; ) {
            if(previous == null || previous == BucketSyncMap.EXPUNGED) return null;
            next = remappingFunction.apply(key, (V) previous);

            final Object witness = reference.compareAndExchange(previous, next);
            if(witness != previous) {
              previous = witness;
              Thread.onSpinWait();
              continue;
            }

            if(next == null) {
              count = -1L;
            }

            break retry;
          }
        }
      }

      final int index;
      if(!this.amended || (mutable == null && (mutable = this.mutableTable) == null) || (node = BucketSyncMap.getNode(mutable, index = (mutable.length - 1) & hash)) == null) {
        return null;
      } else if(node.hash == BucketSyncMap.NODE_MOVED) {
        mutable = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(BucketSyncMap.getNodePlain(mutable, index) == node) {
            for(Node<K, V> previousNode = null; ; ) {
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.referencePlain();
                Object previous = reference.get();
                for(; ; ) {
                  if(previous == null || previous == BucketSyncMap.EXPUNGED) return null;
                  next = remappingFunction.apply(key, (V) previous);

                  final Object witness = reference.compareAndExchange(previous, next);
                  if(witness != previous) {
                    previous = witness;
                    Thread.onSpinWait();
                    continue;
                  }

                  if(next == null) {
                    if(previousNode != null) {
                      previousNode.next(node.nextPlain());
                    } else {
                      BucketSyncMap.setNode(mutable, index, node.nextPlain());
                    }

                    count = -1L;
                  }

                  break retry;
                }
              }

              previousNode = node;

              if((node = node.nextPlain()) == null) {
                return null;
              }
            }
          }
        }
      }
    }

    this.addCount(count);
    return next;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Under contention, the {@code remappingFunction} may be invoked more than
   * once for the same key; only one result will be stored. The function should
   * therefore be idempotent and free of externally visible side effects, and
   * it must not attempt to update this map.</p>
   *
   * <p>If the function throws, the exception is propagated and the mapping for
   * the key remains unchanged.</p>
   */
  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V compute(final K key, final BiFunction<? super K, ? super @Nullable V, ? extends @Nullable V> remappingFunction) {
    requireNonNull(key, "key");
    requireNonNull(remappingFunction, "remappingFunction");

    V next;
    long count = 0L;

    Node<K, V>[] immutable;
    Node<K, V>@org.jetbrains.annotations.Nullable [] mutable = null;
    int length;
    Node<K, V> node;

    final int hash = this.spreadFunction.spread(key.hashCode());

    K nodeKey;
    retry: for(; ; ) {
      immutable = this.immutableTable;
      length = immutable.length;

      if(length > 0) {
        // Find the node from the immutable table and update the value if the
        // node is present.
        node = BucketSyncMap.getNode(immutable, (length - 1) & hash);
        while(node != null) {
          if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
            node = node.next();
            continue;
          }

          final ObjectReference reference = node.reference();
          Object previous = reference.get();
          for(; ; ) {
            next = remappingFunction.apply(key, previous == BucketSyncMap.EXPUNGED ? null : (V) previous);
            if(next == null && (previous == null || previous == BucketSyncMap.EXPUNGED)) return null;

            final Object witness = reference.compareAndExchange(previous, next);
            if(witness != previous) {
              previous = witness;
              Thread.onSpinWait();
              continue;
            }

            if(next != null) {
              if(witness == null) {
                count = 1L;
              } else if(witness == BucketSyncMap.EXPUNGED) {
                this.amendNode(hash, key, reference);

                count = 1L;
              }
            } else {
              count = -1L;
            }

            break retry;
          }
        }
      }

      final int index;
      if(!this.amended || (mutable == null && (mutable = this.mutableTable) == null)) {
        if(length != 0 || (mutable = this.initialize()) != null) {
          this.amend();
        }

        Thread.onSpinWait();
      } else if((node = BucketSyncMap.getNode(mutable, index = (mutable.length - 1) & hash)) == null) {
        next = remappingFunction.apply(key, null);
        if(next == null) return null;

        if(BucketSyncMap.replaceNode(mutable, index, new Node<>(hash, key, new ObjectReference(next)))) {
          count = 1L;
          break;
        }

        Thread.onSpinWait();
      } else if(node.hash == BucketSyncMap.NODE_MOVED) {
        mutable = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(BucketSyncMap.getNodePlain(mutable, index) == node) {
            for(Node<K, V> previousNode = null; ; ) {
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.referencePlain();
                Object previous = reference.get();
                for(; ; ) {
                  next = remappingFunction.apply(key, previous == BucketSyncMap.EXPUNGED ? null : (V) previous);
                  if(next == null && (previous == null || previous == BucketSyncMap.EXPUNGED)) return null;

                  final Object witness = reference.compareAndExchange(previous, next);
                  if(witness != previous) {
                    previous = witness;
                    Thread.onSpinWait();
                    continue;
                  }

                  if(next != null && (witness == null || witness == BucketSyncMap.EXPUNGED)) {
                    count = 1L;
                  } else if(next == null && (witness != null && witness != BucketSyncMap.EXPUNGED)) {
                    if(previousNode != null) {
                      previousNode.next(node.nextPlain());
                    } else {
                      BucketSyncMap.setNode(mutable, index, node.nextPlain());
                    }

                    count = -1L;
                  }

                  break retry;
                }
              }

              previousNode = node;

              if((node = node.nextPlain()) == null) {
                next = remappingFunction.apply(key, null);
                if(next == null) return null;

                previousNode.next(new Node<>(hash, key, new ObjectReference(next)));

                count = 1L;
                break retry;
              }
            }
          }
        }
      }
    }

    this.addCount(count);
    return next;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V putIfAbsent(final K key, final V value) {
    requireNonNull(key, "key");
    requireNonNull(value, "value");

    Node<K, V>[] immutable;
    Node<K, V>@org.jetbrains.annotations.Nullable [] mutable = null;
    int length;
    Node<K, V> node;

    final int hash = this.spreadFunction.spread(key.hashCode());

    K nodeKey;
    retry: for(; ; ) {
      immutable = this.immutableTable;
      length = immutable.length;

      if(length > 0) {
        // Find the node from the immutable table and update the value if the
        // node is present and the value is null or expunged.
        node = BucketSyncMap.getNode(immutable, (length - 1) & hash);
        while(node != null) {
          if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
            node = node.next();
            continue;
          }

          final ObjectReference reference = node.reference();
          Object previous = reference.get();
          for(; ; ) {
            if(previous != null && previous != BucketSyncMap.EXPUNGED) return (V) previous;

            final Object witness = reference.compareAndExchange(previous, value);
            if(witness != previous) {
              previous = witness;
              Thread.onSpinWait();
              continue;
            }

            if(witness == BucketSyncMap.EXPUNGED) {
              this.amendNode(hash, key, reference);
            }

            break retry;
          }
        }
      }

      final int index;
      if(!this.amended || (mutable == null && (mutable = this.mutableTable) == null)) {
        if(length != 0 || (mutable = this.initialize()) != null) {
          this.amend();
        }

        Thread.onSpinWait();
      } else if((node = BucketSyncMap.getNode(mutable, index = (mutable.length - 1) & hash)) == null) {
        if(BucketSyncMap.replaceNode(mutable, index, new Node<>(hash, key, new ObjectReference(value)))) {
          break;
        }

        Thread.onSpinWait();
      } else if(node.hash == BucketSyncMap.NODE_MOVED) {
        mutable = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(BucketSyncMap.getNodePlain(mutable, index) == node) {
            for(; ; ) {
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.referencePlain();
                Object previous = reference.get();
                for(; ; ) {
                  if(previous != null && previous != BucketSyncMap.EXPUNGED) return (V) previous;

                  final Object witness = reference.compareAndExchange(previous, value);
                  if(witness != previous) {
                    previous = witness;
                    Thread.onSpinWait();
                    continue;
                  }

                  break retry;
                }
              }

              final Node<K, V> previousNode = node;
              if((node = node.nextPlain()) == null) {
                previousNode.next(new Node<>(hash, key, new ObjectReference(value)));
                break retry;
              }
            }
          }
        }
      }
    }

    this.addCount(1L);
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V put(final K key, final V value) {
    requireNonNull(key, "key");
    requireNonNull(value, "value");

    Node<K, V>[] immutable;
    Node<K, V>@org.jetbrains.annotations.Nullable [] mutable = null;
    int length;
    Node<K, V> node;

    final int hash = this.spreadFunction.spread(key.hashCode());

    K nodeKey;
    retry: for(; ; ) {
      immutable = this.immutableTable;
      length = immutable.length;

      if(length > 0) {
        // Find the node from the immutable table and update the value if the
        // node is present.
        node = BucketSyncMap.getNode(immutable, (length - 1) & hash);
        while(node != null) {
          if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
            node = node.next();
            continue;
          }

          final ObjectReference reference = node.reference();
          Object previous = reference.get();
          for(; ; ) {
            final Object witness = reference.compareAndExchange(previous, value);
            if(witness != previous) {
              previous = witness;
              Thread.onSpinWait();
              continue;
            }

            if(witness == BucketSyncMap.EXPUNGED) {
              this.amendNode(hash, key, reference);
            } else if(witness != null) {
              return (V) witness;
            }

            break retry;
          }
        }
      }

      final int index;
      if(!this.amended || (mutable == null && (mutable = this.mutableTable) == null)) {
        if(length != 0 || (mutable = this.initialize()) != null) {
          this.amend();
        }

        Thread.onSpinWait();
      } else if((node = BucketSyncMap.getNode(mutable, index = (mutable.length - 1) & hash)) == null) {
        if(BucketSyncMap.replaceNode(mutable, index, new Node<>(hash, key, new ObjectReference(value)))) {
          break;
        }

        Thread.onSpinWait();
      } else if(node.hash == BucketSyncMap.NODE_MOVED) {
        mutable = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(BucketSyncMap.getNodePlain(mutable, index) == node) {
            for(; ; ) {
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.referencePlain();
                Object previous = reference.get();
                for(; ; ) {
                  final Object witness = reference.compareAndExchange(previous, value);
                  if(witness != previous) {
                    previous = witness;
                    Thread.onSpinWait();
                    continue;
                  }

                  if(witness != null && witness != BucketSyncMap.EXPUNGED) {
                    return (V) witness;
                  }

                  break retry;
                }
              }

              final Node<K, V> previousNode = node;
              if((node = node.nextPlain()) == null) {
                previousNode.next(new Node<>(hash, key, new ObjectReference(value)));
                break retry;
              }
            }
          }
        }
      }
    }

    this.addCount(1L);
    return null;
  }

  private void amendNode(final int hash, final K key, final ObjectReference reference) {
    Node<K, V>@org.jetbrains.annotations.Nullable [] table = this.mutableTable;

    for(Node<K, V> node; ; ) {
      final int length, index;
      if(!this.amended || (table == null && (table = this.mutableTable) == null) || (length = table.length) == 0) {
        return;
      } else if((node = BucketSyncMap.getNode(table, index = (length - 1) & hash)) == null) {
        if(BucketSyncMap.replaceNode(table, index, new Node<>(hash, key, reference))) {
          return;
        }

        Thread.onSpinWait();
      } else if(node.hash == BucketSyncMap.NODE_MOVED) {
        table = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(BucketSyncMap.getNodePlain(table, index) == node) {
            for(; ; ) {
              final K nodeKey;
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                node.reference(reference);
                return;
              }

              final Node<K, V> previousNode = node;
              if((node = node.nextPlain()) == null) {
                previousNode.next(new Node<>(hash, key, reference));
                return;
              }
            }
          }
        }
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V remove(final Object key) {
    requireNonNull(key, "key");

    Object previous;

    Node<K, V>[] immutable;
    Node<K, V>@org.jetbrains.annotations.Nullable [] mutable = null;
    int length;
    Node<K, V> node;

    final int hash = this.spreadFunction.spread(key.hashCode());

    K nodeKey;
    retry: for(; ; ) {
      immutable = this.immutableTable;
      length = immutable.length;

      if(length > 0) {
        // Find the node from the immutable table and update the value if the
        // node is present.
        node = BucketSyncMap.getNode(immutable, (length - 1) & hash);
        while(node != null) {
          if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
            node = node.next();
            continue;
          }

          final ObjectReference reference = node.reference();
          Object current = reference.get();
          for(; ; ) {
            if(current == null || current == BucketSyncMap.EXPUNGED) return null;

            previous = reference.compareAndExchange(current, null);
            if(previous != current) {
              current = previous;
              Thread.onSpinWait();
              continue;
            }

            break retry;
          }
        }
      }

      final int index;
      if(!this.amended || (mutable == null && (mutable = this.mutableTable) == null) || (node = BucketSyncMap.getNode(mutable, index = (mutable.length - 1) & hash)) == null) {
        return null;
      } else if(node.hash == BucketSyncMap.NODE_MOVED) {
        mutable = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(BucketSyncMap.getNodePlain(mutable, index) == node) {
            for(Node<K, V> previousNode = null; ; ) {
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.referencePlain();
                Object current = reference.get();
                for(; ; ) {
                  if(current == null || current == BucketSyncMap.EXPUNGED) return null;

                  previous = reference.compareAndExchange(current, null);
                  if(previous != current) {
                    current = previous;
                    Thread.onSpinWait();
                    continue;
                  }

                  if(previousNode != null) {
                    previousNode.next(node.nextPlain());
                  } else {
                    BucketSyncMap.setNode(mutable, index, node.nextPlain());
                  }

                  break retry;
                }
              }

              previousNode = node;

              if((node = node.nextPlain()) == null) {
                return null;
              }
            }
          }
        }
      }
    }

    this.addCount(-1L);
    return (V) previous;
  }

  @Override
  public boolean remove(final Object key, final Object value) {
    requireNonNull(key, "key");
    requireNonNull(value, "value");

    Node<K, V>[] immutable;
    Node<K, V>@org.jetbrains.annotations.Nullable [] mutable = null;
    int length;
    Node<K, V> node;

    final int hash = this.spreadFunction.spread(key.hashCode());

    K nodeKey;
    retry: for(; ; ) {
      immutable = this.immutableTable;
      length = immutable.length;

      if(length > 0) {
        // Find the node from the immutable table and update the value if the
        // node is present.
        node = BucketSyncMap.getNode(immutable, (length - 1) & hash);
        while(node != null) {
          if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
            node = node.next();
            continue;
          }

          final ObjectReference reference = node.reference();
          Object previous = reference.get();
          for(; ; ) {
            if(previous == null || previous == BucketSyncMap.EXPUNGED) return false;
            if(!Objects.equals(value, previous)) return false;

            final Object witness = reference.compareAndExchange(previous, null);
            if(witness != previous) {
              previous = witness;
              Thread.onSpinWait();
              continue;
            }

            break retry;
          }
        }
      }

      final int index;
      if(!this.amended || (mutable == null && (mutable = this.mutableTable) == null) || (node = BucketSyncMap.getNode(mutable, index = (mutable.length - 1) & hash)) == null) {
        return false;
      } else if(node.hash == BucketSyncMap.NODE_MOVED) {
        mutable = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(BucketSyncMap.getNodePlain(mutable, index) == node) {
            for(Node<K, V> previousNode = null; ; ) {
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.referencePlain();
                Object previous = reference.get();
                for(; ; ) {
                  if(previous == null || previous == BucketSyncMap.EXPUNGED) return false;
                  if(!Objects.equals(value, previous)) return false;

                  final Object witness = reference.compareAndExchange(previous, null);
                  if(witness != previous) {
                    previous = witness;
                    Thread.onSpinWait();
                    continue;
                  }

                  if(previousNode != null) {
                    previousNode.next(node.nextPlain());
                  } else {
                    BucketSyncMap.setNode(mutable, index, node.nextPlain());
                  }

                  break retry;
                }
              }

              previousNode = node;

              if((node = node.nextPlain()) == null) {
                return false;
              }
            }
          }
        }
      }
    }

    this.addCount(-1L);
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V replace(final K key, final V value) {
    requireNonNull(key, "key");
    requireNonNull(value, "value");

    Object previous;

    Node<K, V>[] immutable;
    Node<K, V>@org.jetbrains.annotations.Nullable [] mutable = null;
    int length;
    Node<K, V> node;

    final int hash = this.spreadFunction.spread(key.hashCode());

    K nodeKey;
    retry: for(; ; ) {
      immutable = this.immutableTable;
      length = immutable.length;

      if(length > 0) {
        // Find the node from the immutable table and update the value if the
        // node is present.
        node = BucketSyncMap.getNode(immutable, (length - 1) & hash);
        while(node != null) {
          if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
            node = node.next();
            continue;
          }

          final ObjectReference reference = node.reference();
          Object current = reference.get();
          for(; ; ) {
            if(current == null || current == BucketSyncMap.EXPUNGED) return null;

            previous = reference.compareAndExchange(current, value);
            if(previous != current) {
              current = previous;
              Thread.onSpinWait();
              continue;
            }

            break retry;
          }
        }
      }

      final int index;
      if(!this.amended || (mutable == null && (mutable = this.mutableTable) == null) || (node = BucketSyncMap.getNode(mutable, index = (mutable.length - 1) & hash)) == null) {
        return null;
      } else if(node.hash == BucketSyncMap.NODE_MOVED) {
        mutable = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(BucketSyncMap.getNodePlain(mutable, index) == node) {
            for(; ; ) {
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.referencePlain();
                Object current = reference.get();
                for(; ; ) {
                  if(current == null || current == BucketSyncMap.EXPUNGED) return null;

                  previous = reference.compareAndExchange(current, value);
                  if(previous != current) {
                    current = previous;
                    Thread.onSpinWait();
                    continue;
                  }

                  break retry;
                }
              }

              if((node = node.nextPlain()) == null) {
                return null;
              }
            }
          }
        }
      }
    }

    return (V) previous;
  }

  @Override
  public boolean replace(final K key, final V oldValue, final V newValue) {
    requireNonNull(key, "key");
    requireNonNull(oldValue, "oldValue");
    requireNonNull(newValue, "newValue");

    Object previous;

    Node<K, V>[] immutable;
    Node<K, V>@org.jetbrains.annotations.Nullable [] mutable = null;
    int length;
    Node<K, V> node;

    final int hash = this.spreadFunction.spread(key.hashCode());

    K nodeKey;
    retry: for(; ; ) {
      immutable = this.immutableTable;
      length = immutable.length;

      if(length > 0) {
        // Find the node from the immutable table and update the value if the
        // node is present.
        node = BucketSyncMap.getNode(immutable, (length - 1) & hash);
        while(node != null) {
          if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
            node = node.next();
            continue;
          }

          final ObjectReference reference = node.reference();
          Object current = reference.get();
          for(; ; ) {
            if(current == null || current == BucketSyncMap.EXPUNGED) return false;
            if(!Objects.equals(current, oldValue)) return false;

            previous = reference.compareAndExchange(current, newValue);
            if(previous != current) {
              current = previous;
              Thread.onSpinWait();
              continue;
            }

            break retry;
          }
        }
      }

      final int index;
      if(!this.amended || (mutable == null && (mutable = this.mutableTable) == null) || (node = BucketSyncMap.getNode(mutable, index = (mutable.length - 1) & hash)) == null) {
        return false;
      } else if(node.hash == BucketSyncMap.NODE_MOVED) {
        mutable = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(BucketSyncMap.getNodePlain(mutable, index) == node) {
            for(; ; ) {
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.referencePlain();
                Object current = reference.get();
                for(; ; ) {
                  if(current == null || current == BucketSyncMap.EXPUNGED) return false;
                  if(!Objects.equals(current, oldValue)) return false;

                  previous = reference.compareAndExchange(current, newValue);
                  if(previous != current) {
                    current = previous;
                    Thread.onSpinWait();
                    continue;
                  }

                  break retry;
                }
              }

              if((node = node.nextPlain()) == null) {
                return false;
              }
            }
          }
        }
      }
    }

    return true;
  }

  // Bulk Operations

  @Override
  @SuppressWarnings("unchecked")
  public void forEach(final BiConsumer<? super K, ? super V> action) {
    requireNonNull(action, "action");

    this.promote(true);

    final Node<K, V>[] table = this.immutableTable;
    if(table.length > 0) {
      final Traverser<K, V> traverser = new Traverser<>(table);

      Node<K, V> node;
      while((node = traverser.advanceNode()) != null) {
        final Object current = node.referencePlain().get();
        if(current == null || current == BucketSyncMap.EXPUNGED) continue;

        action.accept(node.key, (V) current);
      }
    }
  }

  @Override
  public void clear() {
    this.promote(true);

    final Node<K, V>[] table = this.immutableTable;
    if(table.length > 0) {
      long count = 0L;

      final Traverser<K, V> traverser = new Traverser<>(table);

      Node<K, V> node;
      while((node = traverser.advanceNode()) != null) {
        final ObjectReference reference = node.referencePlain();
        Object current = reference.get();
        for(; ; ) {
          if(current == null || current == BucketSyncMap.EXPUNGED) break;

          final Object witness = reference.compareAndExchange(current, null);
          if(witness != current) {
            current = witness;
            Thread.onSpinWait();
            continue;
          }

          count--;
          break;
        }
      }

      this.addCount(count);
    }
  }

  // Views

  /**
   * {@inheritDoc}
   *
   * <p>The {@link Iterator} produced by this view is weakly consistent: it
   * iterates over an immutable snapshot captured at iterator creation time.
   * New insertions after calling {@link Set#iterator()} are not reflected in
   * the traversal.</p>
   *
   * <p>{@link Iterator#remove()} attempts to remove the last returned entry
   * from the backing map, and succeeds only if the key still maps to the same
   * value at the time of removal.</p>
   */
  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    final EntrySet entries = this.entrySet;
    if(entries != null) return entries;
    return this.entrySet = new EntrySet();
  }

  /* --- < Private Operations > -------------------------------------------- */

  /**
   * Assists in transferring nodes from the old table to the new table, during
   * a resize.
   *
   * @param node the forwarding node
   * @return the next table
   */
  private Node<K, V>@Nullable [] forward(final ForwardingNode<K, V> node) {
    if(this.amended) {
      final Node<K, V>[] next = node.nextTable;
      if(next == this.transferTable) return next;
    }

    return this.mutableTable;
  }

  /**
   * Updates the size of the map by adding the given value. If the map is
   * increasing in size, try to resize the mutable table.
   *
   * @param value the value to change the size by
   */
  private void addCount(final long value) {
    this.size.add(value);

    if(value <= 0L) return;
    this.resize();
  }

  /**
   * Records a missed attempt to grab a node from the immutable table. If the
   * missed attempts exceeds the amount of elements in the map, the mutable
   * table will then be promoted to the immutable table.
   */
  private void miss() {
    this.misses.increment();

    if(this.misses.sum() < this.size.sum()) return;
    this.promote(false);
  }

  /**
   * Locks for the initialize operation, then creates a mutable table to
   * allow initial writes.
   *
   * @return the mutable table
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private Node<K, V>@Nullable [] initialize() {
    long state;

    Node<K, V>[] source;
    Node<K, V>@org.jetbrains.annotations.Nullable [] destination;

    int operation, version, nextVersion;
    long next = StampLock.with(StampLock.OPERATION_INITIALIZE, StampLock.PHASE_RUNNING, 1, 0);

    for(state = this.stampLock.getVolatile(); ; ) {
      if((destination = this.mutableTable) != null || (source = this.immutableTable).length != 0) break;

      operation = StampLock.operation(state);
      version = StampLock.version(state);

      if(operation == StampLock.OPERATION_NONE) {
        next = StampLock.withVersion(next, (nextVersion = version + 1));

        final long witness = this.stampLock.compareAndExchange(state, next);
        if(witness != state) {
          state = witness;
          Thread.onSpinWait();
          continue;
        }

        if(source != this.immutableTable || this.mutableTable != null) {
          state = StampLock.withReset(nextVersion + 1);
          this.stampLock.setVolatile(state);
          Thread.onSpinWait();
          continue;
        }

        this.mutableTable = destination = new Node[this.capacity];
        this.amended = true;

        state = StampLock.withReset(nextVersion + 1);
        this.stampLock.setVolatile(state);
        break;
      }
    }

    return destination;
  }

  /**
   * Creates a new mutable table with an increased capacity from the previous
   * mutable table and transfers the nodes to it.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private void resize() {
    long state;

    Node<K, V>@org.jetbrains.annotations.Nullable [] source;
    Node<K, V>@org.jetbrains.annotations.Nullable [] destination;
    int length;

    int operation, version, nextVersion, count, phase;
    for(state = this.stampLock.getVolatile(); ; ) {
      if(!this.amended
        || (source = this.mutableTable) == null
        || (length = source.length) <= 0
        || length >= BucketSyncMap.MAXIMUM_CAPACITY
        || this.size.sum() < ((long) length * this.loadFactor)) return;

      operation = StampLock.operation(state);
      version = StampLock.version(state);

      if(operation == StampLock.OPERATION_NONE) {
        final long next = StampLock.with(StampLock.OPERATION_RESIZE, StampLock.PHASE_RUNNING, 1, (nextVersion = version + 1));
        final long witness = this.stampLock.compareAndExchange(state, next);
        if(witness != state) {
          state = witness;
          Thread.onSpinWait();
          continue;
        }

        if(!this.amended || source != this.mutableTable) {
          state = StampLock.withReset(nextVersion + 1);
          this.stampLock.setVolatile(state);
          Thread.onSpinWait();
          continue;
        }

        BucketSyncMap.TRANSFER_INDEX.setRelease(this, length);

        this.capacity = length << 1;
        this.transferTable = destination = new Node[this.capacity];
        break;
      } else if(operation == StampLock.OPERATION_RESIZE
        && StampLock.phase(state) == StampLock.PHASE_RUNNING
        && (count = StampLock.count(state)) < BucketSyncMap.MAXIMUM_TRANSFER_THREADS) {
        nextVersion = version;

        if((destination = this.transferTable) == null) {
          Thread.onSpinWait();
          continue;
        }

        final long next = StampLock.withCount(state, count + 1);
        final long witness = this.stampLock.compareAndExchange(state, next);
        if(witness != state) {
          state = witness;
          Thread.onSpinWait();
          continue;
        }

        break;
      }

      return;
    }

    try {
      if(this.transfer(source, destination, true) >= length) {
        state = this.stampLock.getVolatile();
        for(; ; ) {
          if(StampLock.phase(state) != StampLock.PHASE_RUNNING) return;

          final long next = StampLock.withPhase(state, StampLock.PHASE_ACHIEVED);
          final long witness = this.stampLock.compareAndExchange(state, next);
          if(witness != state) {
            state = witness;
            Thread.onSpinWait();
            continue;
          }

          this.transferTable = null;
          this.mutableTable = destination;
          break;
        }
      }
    } finally {
      state = this.stampLock.getVolatile();
      for(; ; ) {
        long next = state;

        if((count = StampLock.count(state)) == 0L) break;
        count -= 1;
        next = StampLock.withCount(next, count);

        if((phase = StampLock.phase(state)) == StampLock.PHASE_FINALIZED) break;
        final boolean finalize = (phase == StampLock.PHASE_ACHIEVED && count == 0);
        if(finalize) {
          next = StampLock.withPhase(next, StampLock.PHASE_FINALIZED);
        }

        final long witness = this.stampLock.compareAndExchange(state, next);
        if(witness != state) {
          state = witness;
          Thread.onSpinWait();
          continue;
        }

        if(finalize) {
          BucketSyncMap.TRANSFER_INDEX.setOpaque(this, 0);
          BucketSyncMap.TRANSFER_PROGRESS.setOpaque(this, 0);

          state = StampLock.withReset(nextVersion + 1);
          this.stampLock.setVolatile(state);
        }

        break;
      }
    }
  }

  /**
   * Creates a new mutable table from the immutable table, when new nodes are
   * required and transfers the nodes to it.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private void amend() {
    long state;

    Node<K, V>[] source;
    Node<K, V>@org.jetbrains.annotations.Nullable [] destination;
    int length;

    int operation, version, nextVersion, count, phase;
    for(state = this.stampLock.getVolatile(); ; ) {
      if(this.amended || this.mutableTable != null || (length = (source = this.immutableTable).length) == 0) return;

      operation = StampLock.operation(state);
      version = StampLock.version(state);

      if(operation == StampLock.OPERATION_NONE) {
        final long next = StampLock.with(StampLock.OPERATION_AMEND, StampLock.PHASE_RUNNING, 1, (nextVersion = version + 1));
        final long witness = this.stampLock.compareAndExchange(state, next);
        if(witness != state) {
          state = witness;
          Thread.onSpinWait();
          continue;
        }

        if(this.amended || this.mutableTable != null || source != this.immutableTable) {
          state = StampLock.withReset(nextVersion + 1);
          this.stampLock.setVolatile(state);
          Thread.onSpinWait();
          continue;
        }

        BucketSyncMap.TRANSFER_INDEX.setRelease(this, length);

        this.transferTable = destination = new Node[length];
        break;
      } else if(operation == StampLock.OPERATION_AMEND
        && StampLock.phase(state) == StampLock.PHASE_RUNNING
        && (count = StampLock.count(state)) < BucketSyncMap.MAXIMUM_TRANSFER_THREADS) {
        nextVersion = version;

        if((destination = this.transferTable) == null) {
          Thread.onSpinWait();
          continue;
        }

        final long next = StampLock.withCount(state, count + 1);
        final long witness = this.stampLock.compareAndExchange(state, next);
        if(witness != state) {
          state = witness;
          Thread.onSpinWait();
          continue;
        }

        break;
      }

      return;
    }

    try {
      if(this.transfer(source, destination, false) >= length) {
        state = this.stampLock.getVolatile();
        for(; ; ) {
          if(StampLock.phase(state) != StampLock.PHASE_RUNNING) return;

          final long next = StampLock.withPhase(state, StampLock.PHASE_ACHIEVED);
          final long witness = this.stampLock.compareAndExchange(state, next);
          if(witness != state) {
            state = witness;
            Thread.onSpinWait();
            continue;
          }

          this.transferTable = null;
          this.mutableTable = destination;
          this.amended = true;
          break;
        }
      }
    } finally {
      state = this.stampLock.getVolatile();
      for(; ; ) {
        long next = state;

        if((count = StampLock.count(state)) == 0L) break;
        count -= 1;
        next = StampLock.withCount(next, count);

        if((phase = StampLock.phase(state)) == StampLock.PHASE_FINALIZED) break;
        final boolean finalize = (phase == StampLock.PHASE_ACHIEVED && count == 0);
        if(finalize) {
          next = StampLock.withPhase(next, StampLock.PHASE_FINALIZED);
        }

        final long witness = this.stampLock.compareAndExchange(state, next);
        if(witness != state) {
          state = witness;
          Thread.onSpinWait();
          continue;
        }

        if(finalize) {
          BucketSyncMap.TRANSFER_INDEX.setOpaque(this, 0);
          BucketSyncMap.TRANSFER_PROGRESS.setOpaque(this, 0);

          state = StampLock.withReset(nextVersion + 1);
          this.stampLock.setVolatile(state);
        }

        break;
      }
    }
  }

  /**
   * Locks for the promotion operation, then replaces the immutable table with
   * the mutable table and removes the mutable table.
   *
   * @param wait whether to wait until we can promote
   */
  protected void promote(final boolean wait) {
    long state;

    Node<K, V>@org.jetbrains.annotations.Nullable [] source;

    int operation, version, nextVersion;
    long next = StampLock.with(StampLock.OPERATION_PROMOTE, StampLock.PHASE_RUNNING, 1, 0);

    for(state = this.stampLock.getVolatile(); ; ) {
      if(!this.amended || (source = this.mutableTable) == null) break;

      operation = StampLock.operation(state);
      version = StampLock.version(state);

      if(operation == StampLock.OPERATION_NONE) {
        next = StampLock.withVersion(next, (nextVersion = version + 1));

        final long witness = this.stampLock.compareAndExchange(state, next);
        if(witness != state) {
          state = witness;
          Thread.onSpinWait();
          continue;
        }

        if(!this.amended || source != this.mutableTable) {
          state = StampLock.withReset(nextVersion + 1);
          this.stampLock.setVolatile(state);
          Thread.onSpinWait();
          continue;
        }

        this.misses.reset();
        this.amended = false;
        this.mutableTable = null;
        this.immutableTable = source;

        state = StampLock.withReset(nextVersion + 1);
        this.stampLock.setVolatile(state);
        break;
      } else if(wait && operation != StampLock.OPERATION_PROMOTE) {
        Thread.onSpinWait();
      } else {
        break;
      }
    }
  }

  /**
   * Transfers nodes from the {@code source} table to the {@code destination}
   * table and returns the amount of nodes that have been transferred.
   *
   * @param source the source table
   * @param destination the destination table
   * @param resize whether this is a resize operation
   * @return the transfer progress count
   */
  @SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter"})
  private int transfer(final Node<K, V>[] source, final Node<K, V>[] destination, final boolean resize) {
    final int capacity = source.length, nextCapacity = destination.length;

    int stride;
    if((stride = BucketSyncMap.NCPU > 1 ? (capacity >>> 3) / BucketSyncMap.NCPU : capacity) < BucketSyncMap.MINIMUM_TRANSFER_STRIDE) {
      stride = BucketSyncMap.MINIMUM_TRANSFER_STRIDE;
    }

    final ForwardingNode<K, V> forwardingNode = new ForwardingNode<>(destination);
    int progress = 0, delta = 0;
    boolean finished = false;

    outer: for(; ; ) {
      Node<K, V> node;
      int index, bound = 0;

      index = (int) BucketSyncMap.TRANSFER_INDEX.getAcquire(this);
      for(; ; ) {
        final int witness;
        if(index <= 0) {
          index = -1;
          break;
        } else if((witness = (int) BucketSyncMap.TRANSFER_INDEX.compareAndExchangeAcquire(this, index, bound = (index > stride ? index - stride : 0))) == index) {
          delta = index - bound;
          break;
        }

        index = witness;
        Thread.onSpinWait();
      }

      boolean advance = false;
      for(int i = index - 1; ; ) {
        if(i < 0 || i >= capacity || (resize && (i + capacity >= nextCapacity))) {
          if(finished) {
            break outer;
          }

          finished = true;
          break;
        } else if(i < bound) {
          break;
        } else if((node = BucketSyncMap.getNode(source, i)) == null) {
          advance = !resize || BucketSyncMap.replaceNode(source, i, forwardingNode);
        } else if(node.hash == BucketSyncMap.NODE_MOVED) {
          advance = true;
        } else {
          synchronized(node) {
            if(BucketSyncMap.getNodePlain(source, i) == node) {
              Node<K, V> loHead = null, loTail = null;
              Node<K, V> hiHead = null, hiTail = null;
              Node<K, V> next = node;

              retry: while((node = next) != null) {
                next = node.nextPlain();

                final ObjectReference reference = node.referencePlain();
                if(!resize) {
                  for(; ; ) {
                    final Object current = reference.get();
                    if(current == BucketSyncMap.EXPUNGED) continue retry;
                    if(current != null) break;

                    if(!reference.expunge()) {
                      Thread.onSpinWait();
                      continue;
                    }

                    continue retry;
                  }
                }

                final BucketSyncMap.Node<K, V> cloned = new BucketSyncMap.Node<>(node.hash, node.key, reference);
                if((node.hash & capacity) == 0) {
                  if(loTail == null) {
                    loHead = cloned;
                  } else {
                    loTail.next(cloned);
                  }

                  loTail = cloned;
                } else {
                  if(hiTail == null) {
                    hiHead = cloned;
                  } else {
                    hiTail.next(cloned);
                  }

                  hiTail = cloned;
                }
              }

              if(resize) {
                if(loHead != null) BucketSyncMap.setNode(destination, i, loHead);
                if(hiHead != null) BucketSyncMap.setNode(destination, i + capacity, hiHead);
              } else {
                if(loTail != null) {
                  loTail.next(hiHead);

                  if(loHead != null) BucketSyncMap.setNode(destination, i, loHead);
                } else {
                  if(hiHead != null) BucketSyncMap.setNode(destination, i, hiHead);
                }
              }

              if(resize) BucketSyncMap.setNode(source, i, forwardingNode);
              advance = true;
            }
          }
        }

        if(advance) i--;
      }

      if(delta > 0) {
        progress = (int) BucketSyncMap.TRANSFER_PROGRESS.getAcquire(this);
        for(; ; ) {
          if(progress >= capacity) break;

          final int witness = (int) BucketSyncMap.TRANSFER_PROGRESS.compareAndExchangeRelease(this, progress, progress + delta);
          if(witness == progress) {
            progress += delta;
            delta = 0;
            break;
          }

          progress = witness;
          Thread.onSpinWait();
        }
      }
    }

    return progress;
  }

  /* --- < Stamp Lock > ---------------------------------------------------- */

  /**
   * Represents a stamped lock for doing bulk map operations such as promoting,
   * resizing, and amending.
   */
  private static final class StampLock {

    // Constants

    private static final int OPERATION_NONE = 0;

    private static final int OPERATION_INITIALIZE = 1;
    private static final int OPERATION_RESIZE = 2;
    private static final int OPERATION_AMEND = 3;
    private static final int OPERATION_PROMOTE = 4;

    private static final int PHASE_IDLE = 0;
    private static final int PHASE_RUNNING = 1;
    private static final int PHASE_ACHIEVED = 2;
    private static final int PHASE_FINALIZED = 3;

    private static final int OPERATION_SHIFT = 0;
    private static final long OPERATION_MASK = 0b111L;

    private static final int PHASE_SHIFT = 3;
    private static final long PHASE_MASK = 0b111L;

    private static final int COUNT_SHIFT = 6;
    private static final long COUNT_MASK = (1L << 16) - 1;

    private static final int VERSION_SHIFT = 32;
    private static final long VERSION_MASK = (1L << 32) - 1;

    // Reflection

    private static final VarHandle STATE;

    static {
      try {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        STATE = lookup.findVarHandle(StampLock.class, "state", long.class);
      } catch(final Exception exception) {
        throw new ExceptionInInitializerError(exception);
      }
    }

    // Packing

    private static int operation(final long state) {
      return (int) ((state >>> StampLock.OPERATION_SHIFT) & StampLock.OPERATION_MASK);
    }

    private static int phase(final long state) {
      return (int) ((state >>> StampLock.PHASE_SHIFT) & StampLock.PHASE_MASK);
    }

    private static int count(final long state) {
      return (int) ((state >>> StampLock.COUNT_SHIFT) & StampLock.COUNT_MASK);
    }

    private static int version(final long state) {
      return (int) ((state >>> StampLock.VERSION_SHIFT) & StampLock.VERSION_MASK);
    }

    private static long with(final int operation, final int phase, final int count, final int version) {
      return ((long) operation & StampLock.OPERATION_MASK) << StampLock.OPERATION_SHIFT
        | ((long) phase & StampLock.PHASE_MASK) << StampLock.PHASE_SHIFT
        | ((long) count & StampLock.COUNT_MASK) << StampLock.COUNT_SHIFT
        | ((long) version & StampLock.VERSION_MASK) << StampLock.VERSION_SHIFT;
    }

    private static long withReset(final int version) {
      return StampLock.with(StampLock.OPERATION_NONE, StampLock.PHASE_IDLE, 0, version);
    }

    private static long withPhase(final long state, final int phase) {
      return (state & ~(StampLock.PHASE_MASK << StampLock.PHASE_SHIFT)) | (((long) phase & StampLock.PHASE_MASK) << StampLock.PHASE_SHIFT);
    }

    private static long withCount(final long state, final int count) {
      return (state & ~(StampLock.COUNT_MASK << StampLock.COUNT_SHIFT)) | (((long) count & StampLock.COUNT_MASK) << StampLock.COUNT_SHIFT);
    }

    private static long withVersion(final long state, final int version) {
      return (state & ~(StampLock.VERSION_MASK << StampLock.VERSION_SHIFT)) | (((long) version & StampLock.VERSION_MASK) << StampLock.VERSION_SHIFT);
    }

    // Fields

    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
    private long state = 0L;

    // Operations

    private StampLock() {
    }

    private long getVolatile() {
      return (long) StampLock.STATE.getVolatile(this);
    }

    private void setVolatile(final long update) {
      StampLock.STATE.setVolatile(this, update);
    }

    private long compareAndExchange(final long expect, final long update) {
      return (long) StampLock.STATE.compareAndExchangeAcquire(this, expect, update);
    }
  }

  /* --- < Object Reference > ---------------------------------------------- */

  /**
   * Represents a value holder for sharing across nodes in the immutable and
   * mutable tables, providing atomic updates for the underlying value.
   */
  @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
  private static final class ObjectReference {
    private static final VarHandle VALUE;

    static {
      try {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        VALUE = lookup.findVarHandle(ObjectReference.class, "value", Object.class);
      } catch(final Exception exception) {
        throw new ExceptionInInitializerError(exception);
      }
    }

    private @Nullable Object value;

    private ObjectReference(final @Nullable Object value) {
      this.value = value;
    }

    private boolean valueExists() {
      final Object value;
      return (value = ObjectReference.VALUE.getOpaque(this)) != null && value != BucketSyncMap.EXPUNGED;
    }

    @SuppressWarnings("unchecked")
    private <V> @Nullable V value() {
      final Object value;
      return (value = ObjectReference.VALUE.getAcquire(this)) != BucketSyncMap.EXPUNGED ? (V) value : null;
    }

    @SuppressWarnings("unchecked")
    private <V> @Nullable V valueOr(final @Nullable V defaultValue) {
      final Object value;
      return ((value = ObjectReference.VALUE.getAcquire(this)) != null && value != BucketSyncMap.EXPUNGED) ? (V) value : defaultValue;
    }

    private @Nullable Object get() {
      return ObjectReference.VALUE.getAcquire(this);
    }

    private boolean expunge() {
      return ObjectReference.VALUE.compareAndExchangeRelease(this, null, BucketSyncMap.EXPUNGED) == null;
    }

    private @Nullable Object compareAndExchange(final @Nullable Object expect, final @Nullable Object update) {
      return ObjectReference.VALUE.compareAndExchangeRelease(this, expect, update);
    }
  }

  /* --- < Nodes > --------------------------------------------------------- */

  /**
   * Represents a key-value pair in this map with the {@link ObjectReference}
   * holding the value. The key and hash would usually be set, except when
   * the node is special, the hash would be negative and the key {@code null}.
   *
   * @param <K> the key type
   * @param <V> the value type
   */
  protected static class Node<K, V> {
    private static final VarHandle REFERENCE;
    private static final VarHandle NEXT;

    static {
      try {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        REFERENCE = lookup.findVarHandle(Node.class, "reference", ObjectReference.class);
        NEXT = lookup.findVarHandle(Node.class, "next", Node.class);
      } catch(final Exception exception) {
        throw new ExceptionInInitializerError(exception);
      }
    }

    private final int hash;
    private final @UnknownNullability K key;
    private @Nullable ObjectReference reference;
    private @Nullable Node<K, V> next;

    private Node(final int hash, final @UnknownNullability K key, final @Nullable ObjectReference reference) {
      this.hash = hash;
      this.key = key;

      Node.REFERENCE.setOpaque(this, reference);
    }

    private ObjectReference referencePlain() {
      return (ObjectReference) Node.REFERENCE.get(this);
    }

    private ObjectReference reference() {
      return (ObjectReference) Node.REFERENCE.getAcquire(this);
    }

    private void reference(final ObjectReference reference) {
      Node.REFERENCE.setRelease(this, reference);
    }

    @SuppressWarnings("unchecked")
    private @Nullable Node<K, V> nextPlain() {
      return (Node<K, V>) Node.NEXT.get(this);
    }

    @SuppressWarnings("unchecked")
    private @Nullable Node<K, V> next() {
      return (Node<K, V>) Node.NEXT.getAcquire(this);
    }

    private void next(final @Nullable Node<K, V> node) {
      Node.NEXT.setRelease(this, node);
    }

    protected @Nullable Node<K, V> find(final int hash, final Object key) {
      Node<K, V> node = this;
      K nodeKey;

      do {
        if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) return node;
      } while((node = node.next()) != null);

      return null;
    }
  }

  /**
   * Represents a node that has been transferred to another table.
   *
   * @param <K> the key type
   * @param <V> the value type
   */
  protected static final class ForwardingNode<K, V> extends Node<K, V> {
    private transient final Node<K, V>[] nextTable;

    private ForwardingNode(final Node<K, V>[] nextTable) {
      super(BucketSyncMap.NODE_MOVED, null, null);

      this.nextTable = nextTable;
    }

    @Override
    protected @Nullable Node<K, V> find(final int hash, final Object key) {
      Node<K, V>[] table = this.nextTable;
      int length;

      Node<K, V> node;
      int nodeHash;
      K nodeKey;

      while((length = table.length) > 0) {
        if((node = BucketSyncMap.getNode(table, (length - 1) & hash)) == null) {
          return null;
        } else if((nodeHash = node.hash) == BucketSyncMap.NODE_MOVED) {
          table = ((ForwardingNode<K, V>) node).nextTable;
        } else {
          if(nodeHash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
            return node;
          }

          while((node = node.next()) != null) {
            if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
              return node;
            }
          }

          return null;
        }
      }

      return null;
    }
  }

  /* --- < Iteration > ----------------------------------------------------- */

  /**
   * Represents a view of a map entry.
   */
  private final class MapEntry implements Map.Entry<K, V> {
    private final K key;
    private @Nullable V value;

    private MapEntry(final K key, final @Nullable V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public K getKey() {
      return this.key;
    }

    @Override
    public @Nullable V getValue() {
      return this.value;
    }

    @Override
    public @Nullable V setValue(final V value) {
      final V previous = BucketSyncMap.this.put(this.key, value);
      this.value = value;
      return previous;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.key) ^ Objects.hashCode(this.value);
    }

    @Override
    public boolean equals(final @Nullable Object other) {
      if(this == other) return true;
      if(!(other instanceof final Map.Entry<?, ?> that)) return false;
      return Objects.equals(this.getKey(), that.getKey())
        && Objects.equals(this.getValue(), that.getValue());
    }

    @Override
    public String toString() {
      return "BucketSyncMap.Entry{key=" + this.key + ", value=" + this.value + "}";
    }
  }

  /**
   * Represents a view of the map entries.
   */
  private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
    @Override
    public int size() {
      return BucketSyncMap.this.size();
    }

    @Override
    public boolean contains(final @Nullable Object entry) {
      if(!(entry instanceof final Map.Entry<?,?> that)) return false;
      final V value = BucketSyncMap.this.get(that.getKey());
      return value != null && Objects.equals(value, that.getValue());
    }

    @Override
    public boolean remove(final @Nullable Object entry) {
      if(!(entry instanceof final Map.Entry<?,?> that)) return false;
      return BucketSyncMap.this.remove(that.getKey(), that.getValue());
    }

    @Override
    public void clear() {
      BucketSyncMap.this.clear();
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
      BucketSyncMap.this.promote(true);
      return new EntryIterator(BucketSyncMap.this.immutableTable);
    }
  }

  /**
   * Represents an entry {@link Iterator} that traverses the nodes in the
   * given table.
   */
  private final class EntryIterator extends Traverser<K, V> implements Iterator<Map.Entry<K, V>> {
    private Map.@Nullable Entry<K, V> next;
    private Map.@Nullable Entry<K, V> current;

    private EntryIterator(final Node<K, V>[] table) {
      super(table);

      this.advanceEntry();
    }

    @Override
    public boolean hasNext() {
      return this.next != null;
    }

    @Override
    public Map.Entry<K, V> next() {
      final Map.Entry<K, V> current;
      if((current = this.next) == null) throw new NoSuchElementException();
      this.current = current;
      this.advanceEntry();
      return current;
    }

    @Override
    public void remove() {
      final Map.Entry<K, V> current;
      if((current = this.current) == null) throw new IllegalStateException();
      this.current = null;
      BucketSyncMap.this.remove(current.getKey(), current.getValue());
    }

    @SuppressWarnings("unchecked")
    private void advanceEntry() {
      this.next = null;

      Node<K, V> node;
      while((node = this.advanceNode()) != null) {
        final Object current = node.referencePlain().get();
        if(current == null || current == BucketSyncMap.EXPUNGED) continue;

        this.next = new MapEntry(node.key, (V) current);
        break;
      }
    }
  }

  /**
   * Represents a simple node traversal class for a table.
   *
   * @param <K> the key
   * @param <V> the value
   */
  private static class Traverser<K, V> {
    private final Node<K, V>[] table;
    private final int length;
    private @Nullable Node<K, V> next;
    private int index;

    private Traverser(final Node<K, V>[] table) {
      this.table = table;
      this.length = table.length;
    }

    protected @Nullable Node<K, V> advanceNode() {
      Node<K, V> node;
      if((node = this.next) != null) {
        node = node.nextPlain();
      }

      for(; ; ) {
        final int index;
        if(node != null) {
          return this.next = node;
        }

        if(this.length <= (index = this.index) || index < 0) {
          return this.next = null;
        }

        node = BucketSyncMap.getNode(this.table, index);

        this.index++;
      }
    }
  }
}
