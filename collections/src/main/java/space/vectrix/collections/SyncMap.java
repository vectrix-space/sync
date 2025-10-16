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
package space.vectrix.collections;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import static java.util.Objects.requireNonNull;

/**
 * Represents a hash table capable of highly concurrent retrievals and updates.
 * It works similarly to {@link ConcurrentHashMap} and
 * shares many of the same concepts. However, the main difference is with how
 * this map carefully manages two separate tables. One table is immutable
 * providing efficient retrieval and updates of existing and frequently
 * accessed key-value pairs that is fully lock-free. Whereas, the mutable table
 * allows new key-value pairs but may involve bucket-level locking.
 *
 * <p>This map is optimized for cases where updates and retrievals for existing
 * entries is a lot faster than recently added entries. However, the
 * performance in other cases are close or equal to that of {@link
 * ConcurrentHashMap} which is faster than a traditional
 * map paired with a read and write lock, or maps with an exclusive lock (such
 * as using {@link Collections#synchronizedMap(Map)}) in similar scenarios.</p>
 *
 * <p>Null values or keys are not accepted.</p>
 *
 * @author Vectrix
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.0.0
 */
@ApiStatus.Experimental
public class SyncMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

  /* ---------------------------- < Constants > ---------------------------- */

  /**
   * Represents the maximum capacity for the underlying tables.
   */
  /* package */ static final int MAXIMUM_CAPACITY = 1 << 30;

  /**
   * Represents the default initial capacity of the tables.
   */
  /* package */ static final int DEFAULT_CAPACITY = 16;

  /**
   * Represents the default load factor for resizing this map.
   */
  /* package */ static final float DEFAULT_LOAD_FACTOR = 0.75F;

  /**
   * Represents the maximum number of threads that can participate in a
   * transfer operation.
   */
  /* package */ static final int MAXIMUM_TRANSFER_THREADS = 16;

  /**
   * Represents the minimum transfer stride for transferring batches of
   * nodes per thread.
   */
  /* package */ static final int MINIMUM_TRANSFER_STRIDE = 16;

  /**
   * Represents the number of usable bits for node hash keys.
   */
  /* package */ static final int HASH_BITS = 0x7FFFFFFF;

  /**
   * Represents the hash for a node that has been transferred.
   */
  /* package */ static final int NODE_MOVED = -1;

  /**
   * Represents a sentinel value for an expunged object reference.
   */
  /* package */ static final Object EXPUNGED = new Object();

  /**
   * Represents the maximum number of processors for transfer size limits.
   */
  /* package */ static final int NCPU = Runtime.getRuntime().availableProcessors();

  /* ------------------------------ < Utilities > ------------------------------ */

  /**
   * Spreads the given hash value to a positive value and forces the top bit to
   * 0.
   *
   * @param value the value to spread
   * @return the spread value
   */
  /* package */ static int spread(final int value) {
    return (value ^ (value >>> 16)) & SyncMap.HASH_BITS;
  }

  /**
   * Returns the optimal table size depending on the given capacity.
   *
   * @param value the current size
   * @return the new size
   */
  /* package */ static int tableSizeFor(final int value) {
    int length = value - 1;
    length |= length >>> 1;
    length |= length >>> 2;
    length |= length >>> 4;
    length |= length >>> 8;
    length |= length >>> 16;
    return length <= 0 ? 1 : (length >= SyncMap.MAXIMUM_CAPACITY ? SyncMap.MAXIMUM_CAPACITY : length + 1);
  }

  /* ---------------------------- < Reflection > ---------------------------- */

  /**
   * Provides atomic operations for {@link Node} tables.
   */
  /* package */ static final VarHandle NODE_ARRAY;

  /**
   * Provides atomic operations for {@link SyncMap#transferIndex}.
   */
  /* package */ static final VarHandle TRANSFER_INDEX;

  /**
   * Provides atomic operations for {@link SyncMap#transferProgress}.
   */
  /* package */ static final VarHandle TRANSFER_PROGRESS;

  static {
    try {
      final MethodHandles.Lookup lookup = MethodHandles.lookup();

      NODE_ARRAY = MethodHandles.arrayElementVarHandle(Node[].class);
      TRANSFER_INDEX = lookup.findVarHandle(SyncMap.class, "transferIndex", int.class);
      TRANSFER_PROGRESS = lookup.findVarHandle(SyncMap.class, "transferProgress", int.class);
    } catch(final Exception exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  /* ------------------------------- < Table > ------------------------------ */

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
  /* package */ static <K, V> @Nullable Node<K, V> getNode(final Node<K, V>@NotNull [] table, final int index) {
    return (Node<K, V>) SyncMap.NODE_ARRAY.getAcquire(table, index);
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
  /* package */ static <K, V> boolean replaceNode(final Node<K, V>@NotNull [] table, final int index, final @Nullable Node<K, V> nextNode) {
    return SyncMap.NODE_ARRAY.compareAndSet(table, index, (Node<K, V>) null, nextNode);
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
  /* package */ static <K, V> void setNode(final Node<K, V> @NotNull [] table, final int index, final @Nullable Node<K, V> node) {
    SyncMap.NODE_ARRAY.setRelease(table, index, node);
  }

  /* ------------------------------ < Fields > ------------------------------ */

  /**
   * Represents the load factor for resizing the map.
   */
  /* package */ final transient float loadFactor;

  /**
   * Represents an immutable hash table that allows fast retrieval and updates
   * without locking.
   */
  /* package */ transient volatile Node<K, V>@NotNull [] immutableTable;

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
   * Represents the transfer index a thread may claim a range of when
   * participating in a transfer operation.
   */
  private transient volatile int transferIndex;

  /**
   * Represents the transfer progress threads will add completed ranges to.
   */
  private transient volatile int transferProgress;

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
  private transient EntrySet entrySet;

  /* ------------------------- < Public Operations > ------------------------- */

  /**
   * Initializes a new {@link SyncMap} with {@link SyncMap#DEFAULT_CAPACITY} and
   * {@link SyncMap#DEFAULT_LOAD_FACTOR}.
   *
   * @since 1.0.0
   */
  public SyncMap() {
    this(SyncMap.DEFAULT_CAPACITY);
  }

  /**
   * Initializes a new {@link SyncMap} with the given initial capacity and
   * {@link SyncMap#DEFAULT_LOAD_FACTOR}.
   *
   * @param initialCapacity the initial capacity
   * @since 1.0.0
   */
  public SyncMap(final int initialCapacity) {
    this(initialCapacity, SyncMap.DEFAULT_LOAD_FACTOR);
  }

  /**
   * Initializes a new {@link SyncMap} with the given initial capacity and load
   * factor.
   *
   * @param initialCapacity the initial capacity
   * @param loadFactor the load factor
   * @since 1.0.0
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public SyncMap(final int initialCapacity, final float loadFactor) {
    final int capacity = initialCapacity >= SyncMap.MAXIMUM_CAPACITY
      ? SyncMap.MAXIMUM_CAPACITY
      : SyncMap.tableSizeFor(initialCapacity);

    this.loadFactor = loadFactor;
    this.immutableTable = (Node<K, V>[]) new Node[capacity];
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
  public boolean containsKey(final @NotNull Object key) {
    requireNonNull(key, "key");

    final int hash = SyncMap.spread(key.hashCode());

    final ObjectReference reference = this.getValue(hash, key);
    if(reference == null) return false;

    final Object value = reference.get();
    return value != null && value != SyncMap.EXPUNGED;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V get(final @NotNull Object key) {
    requireNonNull(key, "key");

    final int hash = SyncMap.spread(key.hashCode());

    final ObjectReference reference = this.getValue(hash, key);
    if(reference == null) return null;

    final Object value = reference.get();
    if(value == SyncMap.EXPUNGED) return null;

    return (V) value;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull V getOrDefault(final @NotNull Object key, final @NotNull V defaultValue) {
    requireNonNull(key, "key");
    requireNonNull(defaultValue, "defaultValue");

    final int hash = SyncMap.spread(key.hashCode());

    final ObjectReference reference = this.getValue(hash, key);
    if(reference == null) return defaultValue;

    final Object value = reference.get();
    if(value == null || value == SyncMap.EXPUNGED) return defaultValue;

    return (V) value;
  }

  /* package */ final @Nullable ObjectReference getValue(final int hash, final @NotNull Object key) {
    ObjectReference reference;
    if((reference = this.getImmutableValue(hash, key)) != null) {
      return reference;
    } else if(this.amended) {
      reference = this.getMutableValue(hash, key);

      this.miss();
      return reference;
    }

    return null;
  }

  /* package */ final @Nullable ObjectReference getImmutableValue(final int hash, final @NotNull Object key) {
    final Node<K, V>[] table = this.immutableTable;

    Node<K, V> node; K nodeKey;
    if((node = SyncMap.getNode(table, (table.length - 1) & hash)) != null) {
      do {
        if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
          return node.reference;
        }
      } while((node = node.next) != null);
    }

    return null;
  }

  /* package */ final @Nullable ObjectReference getMutableValue(final int hash, final @NotNull Object key) {
    final Node<K, V>[] table = this.mutableTable;
    Node<K, V> node;
    final int nodeHash; K nodeKey;

    if(table != null && (node = SyncMap.getNode(table, (table.length - 1) & hash)) != null) {
      if((nodeHash = node.hash) == hash) {
        if((nodeKey = node.key) == key || nodeKey.equals(key)) {
          return node.reference;
        }
      } else if(nodeHash < 0) {
        return (node = node.find(hash, key)) != null ? node.reference : null;
      }

      while((node = node.next) != null) {
        if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
          return node.reference;
        }
      }
    }

    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V computeIfAbsent(final @NotNull K key, final @NotNull Function<? super @NotNull K, ? extends @Nullable V> mappingFunction) {
    requireNonNull(key, "key");
    requireNonNull(mappingFunction, "mappingFunction");

    final int hash = SyncMap.spread(key.hashCode());

    V next;
    retry: for(Node<K, V>[] immutableTable, mutableTable = null; ; ) {
      Node<K, V> node = SyncMap.getNode((immutableTable = this.immutableTable), immutableTable.length - 1 & hash);
      while(node != null) {
        final K nodeKey;
        if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
          node = node.next;
          continue;
        }

        final ObjectReference reference = node.reference;
        Object current = reference.get();
        for(; ; ) {
          if(current != null && current != SyncMap.EXPUNGED) return (V) current;

          next = mappingFunction.apply(key);
          if(next == null) return null;

          final Object previous = reference.compareAndExchange(current, next);
          if(previous != current) {
            current = previous;
            Thread.onSpinWait();
            continue;
          }

          if(previous == SyncMap.EXPUNGED) {
            this.amendNode(hash, key, reference);
          }

          break retry;
        }
      }

      final int length, index;
      if(!this.amended || (mutableTable == null && (mutableTable = this.mutableTable) == null) || (length = mutableTable.length) == 0) {
        this.amend();
      } else if((node = SyncMap.getNode(mutableTable, index = (length - 1) & hash)) == null) {
        next = mappingFunction.apply(key);
        if(next == null) return null;

        if(SyncMap.replaceNode(mutableTable, index, new Node<>(hash, key, new ObjectReference(next)))) {
          break;
        }

        Thread.onSpinWait();
      } else if(node.hash == SyncMap.NODE_MOVED) {
        mutableTable = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(SyncMap.getNode(mutableTable, index) == node) {
            for(; ; ) {
              final K nodeKey;
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.reference;
                Object current = reference.get();
                for(; ; ) {
                  if(current != null && current != SyncMap.EXPUNGED) return (V) current;

                  next = mappingFunction.apply(key);
                  if(next == null) return null;

                  final Object previous = reference.compareAndExchange(current, next);
                  if (previous != current) {
                    current = previous;
                    Thread.onSpinWait();
                    continue;
                  }

                  break retry;
                }
              }

              final Node<K, V> previousNode = node;
              if((node = node.next) == null) {
                next = mappingFunction.apply(key);
                if(next == null) return null;

                previousNode.next = new Node<>(hash, key, new ObjectReference(next));
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

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V computeIfPresent(final @NotNull K key, final @NotNull BiFunction<? super @NotNull K, ? super @NotNull V, ? extends @Nullable V> remappingFunction) {
    requireNonNull(key, "key");
    requireNonNull(remappingFunction, "remappingFunction");

    final int hash = SyncMap.spread(key.hashCode());

    V next; long count = 0L;
    retry: for(Node<K, V>[] table; ; ) {
      Node<K, V> node = SyncMap.getNode((table = this.immutableTable), table.length - 1 & hash);
      while(node != null) {
        final K nodeKey;
        if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
          node = node.next;
          continue;
        }

        final ObjectReference reference = node.reference;
        Object current = reference.get();
        for(; ; ) {
          if(current == null || current == SyncMap.EXPUNGED) return null;
          next = remappingFunction.apply(key, (V) current);

          final Object previous = reference.compareAndExchange(current, next);
          if(previous != current) {
            current = previous;
            Thread.onSpinWait();
            continue;
          }

          if(next == null) {
            count = -1L;
          }

          break retry;
        }
      }

      if(!this.amended || (table = this.mutableTable) == null) return null;

      final int index;
      if((node = SyncMap.getNode(table, index = (table.length - 1) & hash)) != null) {
        synchronized(node) {
          if(SyncMap.getNode(table, index) == node) {
            for(Node<K, V> previousNode = null; ; ) {
              final K nodeKey;
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.reference;
                Object current = reference.get();
                for(; ; ) {
                  if(current == null || current == SyncMap.EXPUNGED) return null;
                  next = remappingFunction.apply(key, (V) current);

                  final Object previous = reference.compareAndExchange(current, next);
                  if(previous != current) {
                    current = previous;
                    Thread.onSpinWait();
                    continue;
                  }

                  if(next == null) {
                    if(previousNode != null) {
                      previousNode.next = node.next;
                    } else {
                      SyncMap.setNode(table, index, node.next);
                    }

                    count = -1L;
                  }

                  break retry;
                }
              }

              previousNode = node;

              if((node = node.next) == null) {
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

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V compute(final @NotNull K key, final @NotNull BiFunction<? super @NotNull K, ? super @Nullable V, ? extends @Nullable V> remappingFunction) {
    requireNonNull(key, "key");
    requireNonNull(remappingFunction, "remappingFunction");

    final int hash = SyncMap.spread(key.hashCode());

    V next; long count = 0L;
    retry: for(Node<K, V>[] immutableTable, mutableTable = null; ; ) {
      Node<K, V> node = SyncMap.getNode((immutableTable = this.immutableTable), immutableTable.length - 1 & hash);
      while(node != null) {
        final K nodeKey;
        if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
          node = node.next;
          continue;
        }

        final ObjectReference reference = node.reference;
        Object current = reference.get();
        for(; ; ) {
          next = remappingFunction.apply(key, current == SyncMap.EXPUNGED ? null : (V) current);
          if(next == null && (current == null || current == SyncMap.EXPUNGED)) return null;

          final Object previous = reference.compareAndExchange(current, next);
          if(previous != current) {
            current = previous;
            Thread.onSpinWait();
            continue;
          }

          if(next != null) {
            if(previous == null) {
              count = 1L;
            } else if(previous == SyncMap.EXPUNGED) {
              this.amendNode(hash, key, reference);

              count = 1L;
            }
          } else {
            count = -1L;
          }

          break retry;
        }
      }

      final int length, index;
      if(!this.amended || (mutableTable == null && (mutableTable = this.mutableTable) == null) || (length = mutableTable.length) == 0) {
        this.amend();
      } else if((node = SyncMap.getNode(mutableTable, index = (length - 1) & hash)) == null) {
        next = remappingFunction.apply(key, null);
        if(next == null) return null;

        if(SyncMap.replaceNode(mutableTable, index, new Node<>(hash, key, new ObjectReference(next)))) {
          count = 1L;
          break;
        }

        Thread.onSpinWait();
      } else if(node.hash == SyncMap.NODE_MOVED) {
        mutableTable = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(SyncMap.getNode(mutableTable, index) == node) {
            for(Node<K, V> previousNode = null; ; ) {
              final K nodeKey;
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.reference;
                Object current = reference.get();
                for(; ; ) {
                  next = remappingFunction.apply(key, current == SyncMap.EXPUNGED ? null : (V) current);
                  if(next == null && (current == null || current == SyncMap.EXPUNGED)) return null;

                  final Object previous = reference.compareAndExchange(current, next);
                  if(previous != current) {
                    current = previous;
                    Thread.onSpinWait();
                    continue;
                  }

                  if(next != null && (previous == null || previous == SyncMap.EXPUNGED)) {
                    count = 1L;
                  } else if(next == null && (previous != null && previous != SyncMap.EXPUNGED)) {
                    if(previousNode != null) {
                      previousNode.next = node.next;
                    } else {
                      SyncMap.setNode(mutableTable, index, node.next);
                    }

                    count = -1L;
                  }

                  break retry;
                }
              }

              previousNode = node;

              if((node = node.next) == null) {
                next = remappingFunction.apply(key, null);
                if(next == null) return null;

                previousNode.next = new Node<>(hash, key, new ObjectReference(next));

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
  public @Nullable V putIfAbsent(final @NotNull K key, final @NotNull V value) {
    requireNonNull(key, "key");
    requireNonNull(value, "value");

    final int hash = SyncMap.spread(key.hashCode());

    retry: for(Node<K, V>[] immutableTable, mutableTable = null; ; ) {
      Node<K, V> node = SyncMap.getNode((immutableTable = this.immutableTable), immutableTable.length - 1 & hash);
      while(node != null) {
        final K nodeKey;
        if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
          node = node.next;
          continue;
        }

        final ObjectReference reference = node.reference;
        Object current = reference.get();
        for(; ; ) {
          if(current != null && current != SyncMap.EXPUNGED) return (V) current;

          final Object previous = reference.compareAndExchange(current, value);
          if(previous != current) {
            current = previous;
            Thread.onSpinWait();
            continue;
          }

          if(previous == SyncMap.EXPUNGED) {
            this.amendNode(hash, key, reference);
          }

          break retry;
        }
      }

      final int length, index;
      if(!this.amended || (mutableTable == null && (mutableTable = this.mutableTable) == null) || (length = mutableTable.length) == 0) {
        this.amend();
      } else if((node = SyncMap.getNode(mutableTable, index = (length - 1) & hash)) == null) {
        if(SyncMap.replaceNode(mutableTable, index, new Node<>(hash, key, new ObjectReference(value)))) {
          break;
        }

        Thread.onSpinWait();
      } else if(node.hash == SyncMap.NODE_MOVED) {
        mutableTable = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(SyncMap.getNode(mutableTable, index) == node) {
            for(; ; ) {
              final K nodeKey;
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.reference;
                Object current = reference.get();
                for(; ; ) {
                  if(current != null && current != SyncMap.EXPUNGED) return (V) current;

                  final Object previous = reference.compareAndExchange(current, value);
                  if (previous != current) {
                    current = previous;
                    Thread.onSpinWait();
                    continue;
                  }

                  break retry;
                }
              }

              final Node<K, V> previousNode = node;
              if((node = node.next) == null) {
                previousNode.next = new Node<>(hash, key, new ObjectReference(value));
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
  public @Nullable V put(final @NotNull K key, final @NotNull V value) {
    requireNonNull(key, "key");
    requireNonNull(value, "value");

    final int hash = SyncMap.spread(key.hashCode());

    retry: for(Node<K, V>[] immutableTable, mutableTable = null; ; ) {
      Node<K, V> node = SyncMap.getNode((immutableTable = this.immutableTable), immutableTable.length - 1 & hash);
      while(node != null) {
        final K nodeKey;
        if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
          node = node.next;
          continue;
        }

        final ObjectReference reference = node.reference;
        Object current = reference.get();
        for(; ; ) {
          final Object previous = reference.compareAndExchange(current, value);
          if(previous != current) {
            current = previous;
            Thread.onSpinWait();
            continue;
          }

          if(previous == SyncMap.EXPUNGED) {
            this.amendNode(hash, key, reference);
          } else if(previous != null) {
            return (V) previous;
          }

          break retry;
        }
      }

      final int length, index;
      if(!this.amended || (mutableTable == null && (mutableTable = this.mutableTable) == null) || (length = mutableTable.length) == 0) {
        this.amend();
      } else if((node = SyncMap.getNode(mutableTable, index = (length - 1) & hash)) == null) {
        if(SyncMap.replaceNode(mutableTable, index, new Node<>(hash, key, new ObjectReference(value)))) {
          break;
        }

        Thread.onSpinWait();
      } else if(node.hash == SyncMap.NODE_MOVED) {
        mutableTable = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(SyncMap.getNode(mutableTable, index) == node) {
            for(; ; ) {
              final K nodeKey;
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.reference;
                Object current = reference.get();
                for(; ; ) {
                  final Object previous = reference.compareAndExchange(current, value);
                  if(previous != current) {
                    current = previous;
                    Thread.onSpinWait();
                    continue;
                  }

                  if(previous != null && previous != SyncMap.EXPUNGED) {
                    return (V) previous;
                  }

                  break retry;
                }
              }

              final Node<K, V> previousNode = node;
              if((node = node.next) == null) {
                previousNode.next = new Node<>(hash, key, new ObjectReference(value));
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

  /* package */ void amendNode(final int hash, final @NotNull K key, final @NotNull ObjectReference reference) {
    Node<K, V>[] table = this.mutableTable;

    for(Node<K, V> node; ; ) {
      final int length, index;
      if(!this.amended || (table == null && (table = this.mutableTable) == null) || (length = table.length) == 0) {
        return;
      } else if((node = SyncMap.getNode(table, index = (length - 1) & hash)) == null) {
        if(SyncMap.replaceNode(table, index, new Node<>(hash, key, reference))) {
          return;
        }

        Thread.onSpinWait();
      } else if(node.hash == SyncMap.NODE_MOVED) {
        table = this.forward((ForwardingNode<K, V>) node);
      } else {
        synchronized(node) {
          if(SyncMap.getNode(table, index) == node) {
            for(; ; ) {
              final K nodeKey;
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                node.reference = reference;
                return;
              }

              final Node<K, V> previousNode = node;
              if((node = node.next) == null) {
                previousNode.next = new Node<>(hash, key, reference);
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
  public @Nullable V remove(final @NotNull Object key) {
    requireNonNull(key, "key");

    final int hash = SyncMap.spread(key.hashCode());

    Object previous;
    retry: for(Node<K, V>[] table; ; ) {
      Node<K, V> node = SyncMap.getNode((table = this.immutableTable), table.length - 1 & hash);
      while(node != null) {
        final K nodeKey;
        if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
          node = node.next;
          continue;
        }

        final ObjectReference reference = node.reference;
        Object current = reference.get();
        for(; ; ) {
          if(current == null || current == SyncMap.EXPUNGED) return null;

          previous = reference.compareAndExchange(current, null);
          if(previous != current) {
            current = previous;
            Thread.onSpinWait();
            continue;
          }

          break retry;
        }
      }

      if(!this.amended || (table = this.mutableTable) == null) return null;

      final int index;
      if((node = SyncMap.getNode(table, index = (table.length - 1) & hash)) != null) {
        synchronized(node) {
          if(SyncMap.getNode(table, index) == node) {
            for(Node<K, V> previousNode = null; ; ) {
              final K nodeKey;
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.reference;
                Object current = reference.get();
                for(; ; ) {
                  if(current == null || current == SyncMap.EXPUNGED) return null;

                  previous = reference.compareAndExchange(current, null);
                  if(previous != current) {
                    current = previous;
                    Thread.onSpinWait();
                    continue;
                  }

                  if(previousNode != null) {
                    previousNode.next = node.next;
                  } else {
                    SyncMap.setNode(table, index, node.next);
                  }

                  break retry;
                }
              }

              previousNode = node;

              if((node = node.next) == null) {
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
  public boolean remove(final @NotNull Object key, final @NotNull Object value) {
    requireNonNull(key, "key");
    requireNonNull(value, "value");

    final int hash = SyncMap.spread(key.hashCode());

    retry: for(Node<K, V>[] table; ; ) {
      Node<K, V> node = SyncMap.getNode((table = this.immutableTable), table.length - 1 & hash);
      while(node != null) {
        final K nodeKey;
        if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
          node = node.next;
          continue;
        }

        final ObjectReference reference = node.reference;
        Object current = reference.get();
        for(; ; ) {
          if(current == null || current == SyncMap.EXPUNGED) return false;
          if(!Objects.equals(value, current)) return false;

          final Object previous = reference.compareAndExchange(current, null);
          if(previous != current) {
            current = previous;
            Thread.onSpinWait();
            continue;
          }

          break retry;
        }
      }

      if(!this.amended || (table = this.mutableTable) == null) return false;

      final int index;
      if((node = SyncMap.getNode(table, index = (table.length - 1) & hash)) != null) {
        synchronized(node) {
          if(SyncMap.getNode(table, index) == node) {
            for(Node<K, V> previousNode = null; ; ) {
              final K nodeKey;
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.reference;
                Object current = reference.get();
                for(; ; ) {
                  if(current == null || current == SyncMap.EXPUNGED) return false;
                  if(!Objects.equals(value, current)) return false;

                  final Object previous = reference.compareAndExchange(current, null);
                  if(previous != current) {
                    current = previous;
                    Thread.onSpinWait();
                    continue;
                  }

                  if(previousNode != null) {
                    previousNode.next = node.next;
                  } else {
                    SyncMap.setNode(table, index, node.next);
                  }

                  break retry;
                }
              }

              previousNode = node;

              if((node = node.next) == null) {
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
  public @Nullable V replace(final @NotNull K key, final @NotNull V value) {
    requireNonNull(key, "key");
    requireNonNull(value, "value");

    final int hash = SyncMap.spread(key.hashCode());

    Object previous;
    retry: for(Node<K, V>[] table; ; ) {
      Node<K, V> node = SyncMap.getNode((table = this.immutableTable), table.length - 1 & hash);
      while(node != null) {
        final K nodeKey;
        if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
          node = node.next;
          continue;
        }

        final ObjectReference reference = node.reference;
        Object current = reference.get();
        for(; ; ) {
          if(current == null || current == SyncMap.EXPUNGED) return null;

          previous = reference.compareAndExchange(current, value);
          if(previous != current) {
            current = previous;
            Thread.onSpinWait();
            continue;
          }

          break retry;
        }
      }

      if(!this.amended || (table = this.mutableTable) == null) return null;

      final int index;
      if((node = SyncMap.getNode(table, index = (table.length - 1) & hash)) != null) {
        synchronized(node) {
          if(SyncMap.getNode(table, index) == node) {
            for(; ; ) {
              final K nodeKey;
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.reference;
                Object current = reference.get();
                for(; ; ) {
                  if(current == null || current == SyncMap.EXPUNGED) return null;

                  previous = reference.compareAndExchange(current, value);
                  if(previous != current) {
                    current = previous;
                    Thread.onSpinWait();
                    continue;
                  }

                  break retry;
                }
              }

              if((node = node.next) == null) {
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
  public boolean replace(final @NotNull K key, final @NotNull V oldValue, final @NotNull V newValue) {
    requireNonNull(key, "key");
    requireNonNull(oldValue, "oldValue");
    requireNonNull(newValue, "newValue");

    final int hash = SyncMap.spread(key.hashCode());

    Object previous;
    retry: for(Node<K, V>[] table; ; ) {
      Node<K, V> node = SyncMap.getNode((table = this.immutableTable), table.length - 1 & hash);
      while(node != null) {
        final K nodeKey;
        if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
          node = node.next;
          continue;
        }

        final ObjectReference reference = node.reference;
        Object current = reference.get();
        for(; ; ) {
          if(current == null || current == SyncMap.EXPUNGED) return false;
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

      if(!this.amended || (table = this.mutableTable) == null) return false;

      final int index;
      if((node = SyncMap.getNode(table, index = (table.length - 1) & hash)) != null) {
        synchronized(node) {
          if(SyncMap.getNode(table, index) == node) {
            for(; ; ) {
              final K nodeKey;
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                final ObjectReference reference = node.reference;
                Object current = reference.get();
                for(; ; ) {
                  if(current == null || current == SyncMap.EXPUNGED) return false;
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

              if((node = node.next) == null) {
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
  public void forEach(final @NotNull BiConsumer<? super @NotNull K, ? super @NotNull V> action) {
    requireNonNull(action, "action");

    this.promote();

    final Traverser<K, V> traverser = new Traverser<>(this.immutableTable);

    Node<K, V> node;
    while((node = traverser.advanceNode()) != null) {
      final Object current = node.reference.get();
      if(current == null || current == SyncMap.EXPUNGED) continue;

      action.accept(node.key, (V) current);
    }
  }

  @Override
  public void clear() {
    this.promote();

    final Traverser<K, V> traverser = new Traverser<>(this.immutableTable);

    Node<K, V> node; long count = 0L;
    while((node = traverser.advanceNode()) != null) {
      final ObjectReference reference = node.reference;
      Object current = reference.get();
      for(; ; ) {
        if(current == null || current == SyncMap.EXPUNGED) continue;

        final Object previous = reference.compareAndExchange(current, null);
        if(previous != current) {
          current = previous;
          Thread.onSpinWait();
          continue;
        }

        count--;
        break;
      }
    }

    this.addCount(count);
  }

  // Views

  /**
   * {@inheritDoc}
   *
   * <p>Creating an {@link Iterator} from this view, takes an immutable
   * snapshot of the entries, meaning, modifications after calling
   * {@link Set#iterator()} will not be visible. Calling
   * {@link Iterator#remove()} will work provided the key-value pair is
   * identical to the pair currently in the map.</p>
   */
  @Override
  public @NotNull Set<Entry<K, V>> entrySet() {
    if(this.entrySet != null) return this.entrySet;
    return this.entrySet = new EntrySet();
  }

  /* ------------------------ < Private Operations > ------------------------ */

  /**
   * Records a missed attempt to grab a node from the immutable table. If the
   * missed attempts exceeds the amount of elements in the map, the mutable
   * table will then be promoted to the immutable table.
   */
  /* package */ void miss() {
    this.misses.increment();

    if(this.misses.sum() < this.size.sum()) return;
    this.promote();
  }

  /**
   * Locks for the promotion operation, then replaces the immutable table with
   * the mutable table and removes the mutable table.
   */
  /* package */ void promote() {
    Node<K, V>[] source;

    long stamp = this.stampLock.getAcquire();
    while(stamp == StampLock.DEFAULT_STAMP && this.amended && (source = this.mutableTable) != null) {
      final long next = StampLock.pack(StampLock.MODE_PROMOTE);
      final long witness = this.stampLock.compareAndExchange(stamp, next);
      if(witness != StampLock.DEFAULT_STAMP) {
        stamp = witness;
        Thread.onSpinWait();
        continue;
      }

      if(!this.amended || source != this.mutableTable) {
        this.stampLock.reset();
        continue;
      }

      this.misses.reset();
      this.amended = false;
      this.mutableTable = null;
      this.immutableTable = source;

      this.stampLock.reset();
      break;
    }
  }

  /**
   * Updates the size of the map by adding the given value. If the map is
   * increasing in size, try to resize the mutable table.
   *
   * @param value the value to change the size by
   */
  /* package */ void addCount(final long value) {
    this.size.add(value);

    if(value <= 0L) return;
    this.resize();
  }

  /**
   * Assists in transferring nodes from the old table to the new table, during
   * a resize.
   *
   * @param node the forwarding node
   * @return the next table
   * @since 1.0.0
   */
  /* package */ Node<K, V>@Nullable [] forward(final @NotNull ForwardingNode<K, V> node) {
    if(this.amended) {
      final Node<K, V>[] next = node.nextTable;
      if(next == this.transferTable) return next;
    }

    return this.mutableTable;
  }

  /**
   * Creates a new mutable table with an increased capacity from the previous
   * mutable table and transfers the nodes to it.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  /* package */ void resize() {
    Node<K, V>[] source, destination; int length;

    long stamp = this.stampLock.getAcquire();
    for(; ; ) {
      if(!this.amended
        || (source = this.mutableTable) == null
        || (length = source.length) <= 0
        || (this.size.sum() * this.loadFactor) < length) return;

      if(stamp == StampLock.DEFAULT_STAMP) {
        final long next = StampLock.pack(StampLock.MODE_RESIZE);
        final long witness = this.stampLock.compareAndExchange(stamp, next);
        if(witness != StampLock.DEFAULT_STAMP) {
          stamp = witness;
          Thread.onSpinWait();
          continue;
        }

        if(!this.amended || source != this.mutableTable) {
          this.stampLock.reset();
          continue;
        }

        this.transferIndex = length;
        this.transferTable = destination = new Node[length << 1];
        break;
      } else if(StampLock.modeOf(stamp) == StampLock.MODE_RESIZE && StampLock.stageOf(stamp) == StampLock.STAGE_RUNNING) {
        final long count = StampLock.countOf(stamp);
        if(count >= SyncMap.MAXIMUM_TRANSFER_THREADS) {
          return;
        }

        if((destination = this.transferTable) == null) {
          Thread.onSpinWait();
          continue;
        }

        final long next = stamp + StampLock.ONE_COUNT;
        final long witness = this.stampLock.compareAndExchange(stamp, next);
        if(witness != stamp) {
          stamp = witness;
          Thread.onSpinWait();
          continue;
        }

        break;
      }

      return;
    }

    stamp = this.stampLock.getAcquire();

    final boolean achieved = StampLock.stageOf(stamp) == StampLock.STAGE_RUNNING && this.transfer(source, destination, true) >= length;
    if(achieved) {
      stamp = this.stampLock.getAcquire();
      for(; ; ) {
        if(StampLock.stageOf(stamp) != StampLock.STAGE_RUNNING) break;

        final long next = StampLock.withStage(stamp, StampLock.STAGE_ACHIEVED);
        final long witness = this.stampLock.compareAndExchange(stamp, next);
        if(witness != stamp) {
          stamp = witness;
          Thread.onSpinWait();
          continue;
        }

        break;
      }
    }

    stamp = this.stampLock.getAcquire();
    for(; ; ) {
      long next = stamp;

      long count = StampLock.countOf(stamp);
      if(count == 0L) return;

      count -= 1L;
      next -= StampLock.ONE_COUNT;

      final int stage = StampLock.stageOf(stamp);
      if(stage == StampLock.STAGE_FINALIZING) return;

      final boolean finalize = (stage == StampLock.STAGE_ACHIEVED && count == 0);
      if(finalize) {
        next = StampLock.withStage(next, StampLock.STAGE_FINALIZING);
      }

      final long witness = this.stampLock.compareAndExchange(stamp, next);
      if(witness != stamp) {
        stamp = witness;
        Thread.onSpinWait();
        continue;
      }

      if(finalize) {
        this.transferIndex = 0;
        this.transferProgress = 0;

        this.transferTable = null;
        this.mutableTable = destination;

        this.stampLock.reset();
      }

      break;
    }
  }

  /**
   * Creates a new mutable table from the immutable table, when new nodes are
   * required and transfers the nodes to it.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  /* package */ void amend() {
    Node<K, V>[] source, destination; int length;

    long stamp = this.stampLock.getAcquire();
    for(; ; ) {
      if(this.amended || this.mutableTable != null) return;

      source = this.immutableTable;
      length = source.length;

      if(stamp == StampLock.DEFAULT_STAMP) {
        final long next = StampLock.pack(StampLock.MODE_AMEND);
        final long witness = this.stampLock.compareAndExchange(stamp, next);
        if(witness != StampLock.DEFAULT_STAMP) {
          stamp = witness;
          Thread.onSpinWait();
          continue;
        }

        if(this.amended || source != this.immutableTable || this.mutableTable != null) {
          this.stampLock.reset();
          continue;
        }

        this.transferIndex = length;
        this.transferTable = destination = new Node[length];
        break;
      } else if(StampLock.modeOf(stamp) == StampLock.MODE_AMEND && StampLock.stageOf(stamp) == StampLock.STAGE_RUNNING) {
        final long count = StampLock.countOf(stamp);
        if(count >= SyncMap.MAXIMUM_TRANSFER_THREADS) {
          return;
        }

        if((destination = this.transferTable) == null) {
          Thread.onSpinWait();
          continue;
        }

        final long next = stamp + StampLock.ONE_COUNT;
        final long witness = this.stampLock.compareAndExchange(stamp, next);
        if(witness != stamp) {
          stamp = witness;
          Thread.onSpinWait();
          continue;
        }

        break;
      }

      return;
    }

    stamp = this.stampLock.getAcquire();

    final boolean achieved = StampLock.stageOf(stamp) == StampLock.STAGE_RUNNING && this.transfer(source, destination, false) >= length;
    if(achieved) {
      stamp = this.stampLock.getAcquire();
      for(; ; ) {
        if(StampLock.stageOf(stamp) != StampLock.STAGE_RUNNING) break;

        final long next = StampLock.withStage(stamp, StampLock.STAGE_ACHIEVED);
        final long witness = this.stampLock.compareAndExchange(stamp, next);
        if(witness != stamp) {
          stamp = witness;
          Thread.onSpinWait();
          continue;
        }

        break;
      }
    }

    stamp = this.stampLock.getAcquire();
    for(; ; ) {
      long next = stamp;

      long count = StampLock.countOf(stamp);
      if(count == 0L) return;

      count -= 1L;
      next -= StampLock.ONE_COUNT;

      final int stage = StampLock.stageOf(stamp);
      if(stage == StampLock.STAGE_FINALIZING) return;

      final boolean finalize = (stage == StampLock.STAGE_ACHIEVED && count == 0);
      if(finalize) {
        next = StampLock.withStage(next, StampLock.STAGE_FINALIZING);
      }

      final long witness = this.stampLock.compareAndExchange(stamp, next);
      if(witness != stamp) {
        stamp = witness;
        Thread.onSpinWait();
        continue;
      }

      if(finalize) {
        this.transferIndex = 0;
        this.transferProgress = 0;

        this.transferTable = null;
        this.mutableTable = destination;
        this.amended = true;

        this.stampLock.reset();
      }

      break;
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
  /* package */ int transfer(final Node<K, V>@NotNull [] source, final Node<K, V>@NotNull [] destination, final boolean resize) {
    final int capacity = source.length, nextCapacity = destination.length;

    int stride;
    if((stride = SyncMap.NCPU > 1 ? (capacity >>> 3) / SyncMap.NCPU : capacity) < SyncMap.MINIMUM_TRANSFER_STRIDE) {
      stride = SyncMap.MINIMUM_TRANSFER_STRIDE;
    }

    final ForwardingNode<K, V> forwardingNode = new ForwardingNode<>(destination);
    int progress = 0, delta = 0;
    boolean finished = false;

    outer: for(; ; ) {
      Node<K, V> node;
      int index, bound = 0;

      for(; ; ) {
        if((index = (int) SyncMap.TRANSFER_INDEX.getAcquire(this)) <= 0 || finished) {
          index = -1;
          break;
        } else if(SyncMap.TRANSFER_INDEX.compareAndSet(this, index, bound = (index > stride ? index - stride : 0))) {
          delta = index - bound;
          break;
        }

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
        } else if((node = SyncMap.getNode(source, i)) == null) {
          advance = !resize || SyncMap.replaceNode(source, i, forwardingNode);
        } else if(node.hash == SyncMap.NODE_MOVED) {
          advance = true;
        } else {
          synchronized(node) {
            if(SyncMap.getNode(source, i) == node) {
              Node<K, V> loHead = null, loTail = null;
              Node<K, V> hiHead = null, hiTail = null;
              Node<K, V> next = node;

              retry: while((node = next) != null) {
                next = node.next;

                final ObjectReference reference = node.reference;
                if(!resize) {
                  for(; ; ) {
                    final Object current = reference.get();
                    if(current == SyncMap.EXPUNGED) continue retry;
                    if(current != null) break;

                    if(!reference.expunge()) {
                      Thread.onSpinWait();
                      continue;
                    }

                    continue retry;
                  }
                }

                final space.vectrix.collections.SyncMap.Node<K, V> cloned = new space.vectrix.collections.SyncMap.Node<>(node.hash, node.key, node.reference);
                if((node.hash & capacity) == 0) {
                  if(loTail == null) {
                    loHead = cloned;
                  } else {
                    loTail.next = cloned;
                  }

                  loTail = cloned;
                } else {
                  if(hiTail == null) {
                    hiHead = cloned;
                  } else {
                    hiTail.next = cloned;
                  }

                  hiTail = cloned;
                }
              }

              if(resize) {
                if(loHead != null) SyncMap.setNode(destination, i, loHead);
                if(hiHead != null) SyncMap.setNode(destination, i + capacity, hiHead);
              } else {
                if(loTail != null) {
                  loTail.next = hiHead;

                  if(loHead != null) SyncMap.setNode(destination, i, loHead);
                } else {
                  if(hiHead != null) SyncMap.setNode(destination, i, hiHead);
                }
              }

              if(resize) SyncMap.setNode(source, i, forwardingNode);
              advance = true;
            }
          }
        }

        if(advance) i--;
      }

      if(delta > 0) {
        progress = (int) SyncMap.TRANSFER_PROGRESS.getAcquire(this);
        for(; ; ) {
          if(progress >= capacity) break;

          final int current = (int) SyncMap.TRANSFER_PROGRESS.compareAndExchangeRelease(this, progress, progress + delta);
          if(current == progress) {
            progress += delta;
            delta = 0;
            break;
          }

          progress = current;
          Thread.onSpinWait();
        }
      }
    }

    return progress;
  }

  /* --------------------------- < Stamp Lock > --------------------------- */

  /**
   * Represents a stamped lock for doing bulk map operations such as promoting,
   * resizing, and amending.
   */
  /* package */ static final class StampLock {
    /* package */ static final long DEFAULT_STAMP = 0L;

    /*
     * Stamp mode for bulk table updates.
     */
    /* package */ static final int MODE_PROMOTE = 1;
    /* package */ static final int MODE_RESIZE = 2;
    /* package */ static final int MODE_AMEND = 3;

    /*
     * Stamp stage for bulk table updates.
     */
    /* package */ static final int STAGE_RUNNING = 1;
    /* package */ static final int STAGE_ACHIEVED = 2;
    /* package */ static final int STAGE_FINALIZING = 3;

    /*
     * Shift for the stamp.
     */
    /* package */ static final int SHIFT_MODE = 0;
    /* package */ static final int SHIFT_STAGE = 3;
    /* package */ static final int SHIFT_COUNT = 6;

    /*
     * Mask for the stamp.
     */
    /* package */ static final long MASK_MODE = 0b111;
    /* package */ static final long MASK_STAGE = 0b111;

    /* package */ static final long ONE_COUNT = 1L << StampLock.SHIFT_COUNT;

    /**
     * Returns a packed {@code long} stamp with the given mode, running stage,
     * and thread count of {@code 1}.
     *
     * @param mode the mode
     * @return the packed stamp
     */
    /* package */ static long pack(final int mode) {
      return ((long) mode & 7L) << StampLock.SHIFT_MODE
        | ((long) StampLock.STAGE_RUNNING & 7L) << StampLock.SHIFT_STAGE
        | 1L << StampLock.SHIFT_COUNT;
    }

    /**
     * Returns the mode from the packed stamp.
     *
     * @param stamp the packed stamp
     * @return the mode
     */
    /* package */ static int modeOf(final long stamp) {
      return (int) ((stamp >>> StampLock.SHIFT_MODE) & StampLock.MASK_MODE);
    }

    /**
     * Returns the stage from the packed stamp.
     *
     * @param stamp the packed stamp
     * @return the stage
     */
    /* package */ static int stageOf(final long stamp) {
      return (int) ((stamp >>> StampLock.SHIFT_STAGE) & StampLock.MASK_STAGE);
    }

    /**
     * Returns the count from the packet stamp.
     *
     * @param stamp the packed stamp
     * @return the count
     */
    /* package */ static long countOf(final long stamp) {
      return stamp >>> StampLock.SHIFT_COUNT;
    }

    /**
     * Returns the packed stamp with the given stage applied.
     *
     * @param stamp the packed stamp
     * @param newStage the new stage
     * @return the packed stamp, with the new stage
     */
    /* package */ static long withStage(final long stamp, final int newStage) {
      final long stageMask = StampLock.MASK_STAGE << StampLock.SHIFT_STAGE;
      return (stamp & ~stageMask) | (((long) newStage & 7L) << StampLock.SHIFT_STAGE);
    }

    private static final VarHandle STAMP;

    static {
      try {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        STAMP = lookup.findVarHandle(StampLock.class, "stamp", long.class);
      } catch(final Exception exception) {
        throw new ExceptionInInitializerError(exception);
      }
    }

    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
    private long stamp;

    /* package */ StampLock() {
      this.stamp = StampLock.DEFAULT_STAMP;
    }

    /* package */ long getAcquire() {
      return (long) StampLock.STAMP.getAcquire(this);
    }

    /* package */ long compareAndExchange(final long expect, final long update) {
      return (long) StampLock.STAMP.compareAndExchangeRelease(this, expect, update);
    }

    /* package */ void reset() {
      StampLock.STAMP.setRelease(this, StampLock.DEFAULT_STAMP);
    }
  }

  /* ------------------------ < Object Reference > ------------------------ */

  /**
   * Represents a value holder for sharing across nodes in the immutable and
   * mutable tables, providing atomic updates for the underlying value.
   */
  @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
  /* package */ static final class ObjectReference {
    private static final VarHandle VALUE;

    static {
      try {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        VALUE = lookup.findVarHandle(ObjectReference.class, "value", Object.class);
      } catch(final Exception exception) {
        throw new ExceptionInInitializerError(exception);
      }
    }

    private Object value;

    /* package */ ObjectReference(final @Nullable Object value) {
      this.value = value;
    }

    /* package */ @Nullable Object get() {
      return ObjectReference.VALUE.getAcquire(this);
    }

    /* package */ boolean expunge() {
      return ObjectReference.VALUE.compareAndSet(this, null, SyncMap.EXPUNGED);
    }

    /* package */ @Nullable Object compareAndExchange(final @Nullable Object expect, final @Nullable Object update) {
      return ObjectReference.VALUE.compareAndExchangeRelease(this, expect, update);
    }
  }

  /* ------------------------------ < Nodes > ------------------------------ */

  /**
   * Represents a key-value pair in this map with the {@link ObjectReference}
   * holding the value. The key and hash would usually be set, except when
   * the node is special, the hash would be negative and the key {@code null}.
   *
   * @param <K> the key type
   * @param <V> the value type
   */
  /* package */ static class Node<K, V> {
    /* package */ final int hash;
    /* package */ final K key;
    /* package */ volatile ObjectReference reference;
    /* package */ volatile Node<K, V> next;

    /* package */ Node(final int hash, final @UnknownNullability K key, final @Nullable ObjectReference reference) {
      this.hash = hash;
      this.key = key;
      this.reference = reference;
    }

    /* package */ @Nullable Node<K, V> find(final int hash, final @NotNull Object key) {
      Node<K, V> node = this; K nodeKey;
      do {
        if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) return node;
      } while((node = node.next) != null);

      return null;
    }
  }

  /**
   * Represents a node that has been transferred to another table.
   *
   * @param <K> the key type
   * @param <V> the value type
   */
  /* package */ static final class ForwardingNode<K, V> extends Node<K, V> {
    /* package */ transient final Node<K, V>[] nextTable;

    /* package */ ForwardingNode(final Node<K, V>@NotNull [] nextTable) {
      super(SyncMap.NODE_MOVED, null, null);

      this.nextTable = nextTable;
    }

    @Override
    /* package */ @Nullable Node<K, V> find(final int hash, final @NotNull Object key) {
      Node<K, V> node; int length, nodeHash; K nodeKey;

      for(Node<K, V>[] table = this.nextTable; table != null && (length = table.length) > 0; ) {
        if((node = SyncMap.getNode(table, (length - 1) & hash)) == null) {
          return null;
        } else if((nodeHash = node.hash) == SyncMap.NODE_MOVED) {
          table = ((ForwardingNode<K, V>) node).nextTable;
        } else {
          if(nodeHash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
            return node;
          }

          while((node = node.next) != null) {
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

  /* ---------------------------- < Iteration > ---------------------------- */

  /**
   * Represents a view of a map entry.
   */
  /* package */ final class MapEntry implements Entry<K, V> {
    private final K key;
    private V value;

    /* package */ MapEntry(final @NotNull K key, final @Nullable V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public K getKey() {
      return this.key;
    }

    @Override
    public V getValue() {
      return this.value;
    }

    @Override
    public V setValue(final @NotNull V value) {
      final V previous = SyncMap.this.put(this.key, value);
      this.value = value;
      return previous;
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.key, this.value);
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
      return "SyncMap.Entry{key=" + this.key + ", value=" + this.value + "}";
    }
  }

  /**
   * Represents a view of the map entries.
   */
  /* package */ final class EntrySet extends AbstractSet<Entry<K, V>> {
    @Override
    public int size() {
      return SyncMap.this.size();
    }

    @Override
    public boolean contains(final @Nullable Object entry) {
      if(!(entry instanceof final Map.Entry<?,?> that)) return false;
      final V value = SyncMap.this.get(that.getKey());
      return value != null && Objects.equals(value, that.getValue());
    }

    @Override
    public boolean remove(final @Nullable Object entry) {
      if(!(entry instanceof final Map.Entry<?,?> that)) return false;
      return SyncMap.this.remove(that.getKey(), that.getValue());
    }

    @Override
    public void clear() {
      SyncMap.this.clear();
    }

    @Override
    public @NotNull Iterator<Entry<K, V>> iterator() {
      SyncMap.this.promote();
      return new EntryIterator(SyncMap.this.immutableTable);
    }
  }

  /**
   * Represents an entry {@link Iterator} that traverses the nodes in the
   * given table.
   */
  /* package */ final class EntryIterator extends Traverser<K, V> implements Iterator<Entry<K, V>> {
    private Entry<K, V> next;
    private Entry<K, V> current;

    /* package */ EntryIterator(final Node<K, V>@NotNull [] table) {
      super(table);

      this.advanceEntry();
    }

    @Override
    public boolean hasNext() {
      return this.next != null;
    }

    @Override
    public @NotNull Entry<K, V> next() {
      final Entry<K, V> current;
      if((current = this.next) == null) throw new NoSuchElementException();
      this.current = current;
      this.advanceEntry();
      return current;
    }

    @Override
    public void remove() {
      final Entry<K, V> current;
      if((current = this.current) == null) throw new IllegalStateException();
      this.current = null;
      SyncMap.this.remove(current.getKey(), current.getValue());
    }

    @SuppressWarnings("unchecked")
    private void advanceEntry() {
      this.next = null;

      Node<K, V> node;
      while((node = this.advanceNode()) != null) {
        final Object current = node.reference.get();
        if(current == null || current == SyncMap.EXPUNGED) continue;

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
  /* package */ static class Traverser<K, V> {
    private final Node<K, V>[] table;
    private final int length;
    private Node<K, V> next;
    private int index;

    /* package */ Traverser(final Node<K, V>@NotNull [] table) {
      this.table = table;
      this.length = table.length;
    }

    /* package */ final @Nullable Node<K, V> advanceNode() {
      Node<K, V> node;
      if((node = this.next) != null) {
        node = node.next;
      }

      for(; ; ) {
        final int index;
        if(node != null) {
          return this.next = node;
        }

        if(this.length <= (index = this.index) || index < 0) {
          return this.next = null;
        }

        node = SyncMap.getNode(this.table, index);

        this.index++;
      }
    }
  }
}
