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

  /*
   * Lock states for bulk table updates.
   */
  /* package */ static final int TRANSFER_PROMOTE = -1;
  /* package */ static final int TRANSFER_RESIZE = 1;
  /* package */ static final int TRANSFER_AMEND = 2;

  /*
   * Operation states for bulk table updates.
   */
  /* package */ static final int STATE_IDLE = 0;
  /* package */ static final int STATE_ACHIEVED = 1;
  /* package */ static final int STATE_COMPLETED = 2;
  /* package */ static final int STATE_FINALIZED = 3;

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
   * Provides atomic operations for {@link ValueReference#value}.
   */
  /* package */ static final VarHandle NODE_VALUE;

  /**
   * Provides atomic operations for {@link SyncMap#token}.
   */
  /* package */ static final VarHandle OPERATION_TOKEN;

  /**
   * Provides atomic operations for {@link OperationToken#threads}.
   */
  /* package */ static final VarHandle OPERATION_THREADS;

  /**
   * Provides atomic operations for {@link OperationToken#state}.
   */
  /* package */ static final VarHandle OPERATION_STATE;

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
      NODE_VALUE = lookup.findVarHandle(ValueReference.class, "value", Object.class);
      OPERATION_TOKEN = lookup.findVarHandle(SyncMap.class, "token", OperationToken.class);
      OPERATION_THREADS = lookup.findVarHandle(OperationToken.class, "threads", int.class);
      OPERATION_STATE = lookup.findVarHandle(OperationToken.class, "state", int.class);
      TRANSFER_INDEX = lookup.findVarHandle(SyncMap.class, "transferIndex", int.class);
      TRANSFER_PROGRESS = lookup.findVarHandle(SyncMap.class, "transferProgress", int.class);
    } catch(final Exception exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  /* ------------------------------- < Table > ------------------------------- */

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
  /* package */ static <K, V> @Nullable Node<K, V> getNode(final Node<K, V> @NotNull [] table, final int index) {
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
  /* package */ static <K, V> boolean replaceNode(final Node<K, V> @NotNull [] table, final int index, final @Nullable Node<K, V> nextNode) {
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
   * Represents the {@link OperationToken} for the current operation if started,
   * otherwise {@code null}.
   */
  @SuppressWarnings({"FieldMayBeFinal", "unused"})
  private transient volatile @Nullable OperationToken token;

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

    final ValueReference<V> reference;
    return (reference = this.getValue(hash, key)) != null && Atomics.IS_PRESENT.test(SyncMap.NODE_VALUE.getAcquire(reference));
  }

  @Override
  public V get(final @NotNull Object key) {
    requireNonNull(key, "key");

    final int hash = SyncMap.spread(key.hashCode());

    final ValueReference<V> reference;
    return (reference = this.getValue(hash, key)) != null ? Sentinel.unbox(SyncMap.NODE_VALUE.getAcquire(reference)) : null;
  }

  @Override
  public V getOrDefault(final @NotNull Object key, final @NotNull V defaultValue) {
    requireNonNull(key, "key");
    requireNonNull(defaultValue, "defaultValue");

    final int hash = SyncMap.spread(key.hashCode());

    final ValueReference<V> reference;
    return (reference = this.getValue(hash, key)) == null
      ? defaultValue
      : Sentinel.unboxOr(SyncMap.NODE_VALUE.getAcquire(reference), defaultValue);
  }

  @SuppressWarnings("unchecked")
  /* package */ final @Nullable ValueReference<V> getValue(final int hash, final @NotNull Object key) {
    ValueReference<V> reference;
    if((reference = this.getImmutableValue(hash, (K) key)) != null) {
      return reference;
    } else if(this.amended) {
      reference = this.getMutableValue(hash, (K) key);

      // Record a miss even if the node does not exist, but only if the table
      // is amended.
      this.miss();
      return reference;
    }

    return null;
  }

  /* package */ final @Nullable ValueReference<V> getImmutableValue(final int hash, final @NotNull K key) {
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

  /* package */ final @Nullable ValueReference<V> getMutableValue(final int hash, final @NotNull K key) {
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

    Node<K, V>[] table = this.immutableTable; Node<K, V> node;
    Object value = Sentinel.EMPTY; Object result;
    int length = table.length, index; long count = 0L;

    final Atomics.ValueEntry entry = new Atomics.ValueEntry();
    if((node = SyncMap.getNode(table, (length - 1) & hash)) != null
        && (node = node.find(hash, key)) != null
        && Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_NULL, (value = mappingFunction.apply(key)))) {
      // If the new value got committed (if it was null), and the new value is
      // not null, then increment the count.
      if(value != null) count = 1L;
      result = value;
    } else if(!Atomics.IS_PRESENT.test(result = entry.previous)) {
      // Only proceed here if the previous value was null, expunged or
      // uncommitted.
      retry: for(; ; ) {
        table = this.immutableTable;
        length = table.length;

        if((node = SyncMap.getNode(table, (length - 1) & hash)) != null) {
          do {
            if(node.hash == hash && (node.key == key || node.key.equals(key))) {
              if(Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_EXPUNGED, value != Sentinel.EMPTY ? value : (value = mappingFunction.apply(key)))) {
                // If the next value is not null, attempt to unexpunge the
                // value and set the new value. Then add the node back to
                // the mutable table and increment the count.
                if(value != null) {
                  this.amendNode(hash, key, node.reference);

                  count = 1L;
                }

                result = value;
                break retry;
              } else if(Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_NULL, value != Sentinel.EMPTY ? value : (value = mappingFunction.apply(key)))) {
                // If the value was null and the next value is not null,
                // increment the count.
                if(value != null) count = 1L;

                result = value;
                break retry;
              } else if(Atomics.IS_PRESENT.test(entry.next)) {
                // If the value was present and not expunged, return.
                result = entry.previous;
                break retry;
              }

              continue retry;
            }
          } while((node = node.next) != null);
        }

        if((table = this.mutableTable) == null || (length = table.length) == 0) {
          this.amend();
        } else {
          if((node = SyncMap.getNode(table, index = (length - 1) & hash)) == null) {
            // Compute and check if the value is not null, if not then break.
            if((value != Sentinel.EMPTY ? value : (value = mappingFunction.apply(key))) == null) {
              result = null;
              break;
            }

            // Try insert a new node if it hasn't previously existed, then
            // increment the count.
            if(SyncMap.replaceNode(table, index, new Node<>(hash, key, new ValueReference<>((V) value)))) {
              count = 1L;
              result = value;
              break;
            }
          } else if(node.hash == SyncMap.NODE_MOVED) {
            // If the node has moved during a transfer, join the effort in
            // completing the transfer.
            this.forward((ForwardingNode<K, V>) node);
          } else {
            synchronized(node) {
              if(SyncMap.getNode(table, index) == node) {
                for(; ; ) {
                  if(node.hash == hash && (node.key == key || node.key.equals(key))) {
                    if(Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_NULL_OR_EXPUNGED, value != Sentinel.EMPTY ? value : mappingFunction.apply(key))) {
                      // If the value was null or expunged and the next value
                      // is not null, increment the count.
                      if(value != null) count = 1L;
                      result = value;
                      break retry;
                    }

                    result = entry.previous;
                    break retry;
                  }

                  final Node<K, V> previousNode = node;
                  if((node = node.next) == null) {
                    // Compute and check if the value is not null, if not then
                    // break.
                    if((value != Sentinel.EMPTY ? value : (value = mappingFunction.apply(key))) == null) {
                      result = null;
                      break retry;
                    }

                    // If the node does not exist in this bucket, create a new
                    // node and increment the count.
                    previousNode.next = new Node<>(hash, key, new ValueReference<>((V) value));
                    count = 1L;
                    result = value;
                    break retry;
                  }
                }
              }
            }
          }
        }

        Thread.onSpinWait();
      }
    }

    if(count > 0L) this.addCount(count);
    return Sentinel.unbox(result);
  }

  @Override
  public @Nullable V computeIfPresent(final @NotNull K key, final @NotNull BiFunction<? super @NotNull K, ? super @NotNull V, ? extends @Nullable V> remappingFunction) {
    requireNonNull(key, "key");
    requireNonNull(remappingFunction, "remappingFunction");

    final int hash = SyncMap.spread(key.hashCode());

    Node<K, V>[] table = this.immutableTable; Node<K, V> node;
    int length = table.length; long count = 0L;

    final Atomics.ValueEntry entry = new Atomics.ValueEntry();
    if((node = SyncMap.getNode(table, (length - 1) & hash)) != null && (node = node.find(hash, key)) != null) {
      // Try update the node with the new value, only if it is present.
      if(Atomics.computeAndReplace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_PRESENT, that -> remappingFunction.apply(key, Sentinel.unbox(that))) && entry.next == null) {
        // If the new value got committed (if it was present and not expunged),
        // and the new value is null, then decrement the count.
        count = -1L;
      }
    } else if(this.amended && (table = this.mutableTable) != null) {
      length = table.length;

      // Only proceed here if the node did not exist in the immutable table.
      final int index;
      if((node = SyncMap.getNode(table, index = (length - 1) & hash)) != null) {
        synchronized(node) {
          if(SyncMap.getNode(table, index) == node) {
            for(Node<K, V> previousNode = null; ; ) {
              if(node.hash == hash && (node.key == key || node.key.equals(key))) {
                if(Atomics.computeAndReplace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_PRESENT, that -> remappingFunction.apply(key, Sentinel.unbox(that))) && entry.next == null) {
                  // If the value was present or expunged and the next value
                  // is null, remove the node and decrement the count.
                  if(previousNode != null) {
                    previousNode.next = node.next;
                  } else {
                    SyncMap.setNode(table, index, node.next);
                  }

                  count = -1L;
                }

                break;
              }

              previousNode = node;

              if((node = node.next) == null) {
                break;
              }
            }
          }
        }
      }
    }

    if(count != 0L) this.addCount(count);
    return Sentinel.unbox(entry.next);
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V compute(final @NotNull K key, final @NotNull BiFunction<? super @NotNull K, ? super @NotNull V, ? extends @Nullable V> remappingFunction) {
    requireNonNull(key, "key");
    requireNonNull(remappingFunction, "remappingFunction");

    final int hash = SyncMap.spread(key.hashCode());

    Node<K, V>[] table = this.immutableTable; Node<K, V> node;
    Object value = Sentinel.EMPTY;
    int length = table.length, index; long count = 0L;

    final Atomics.ValueEntry entry = new Atomics.ValueEntry();
    if((node = SyncMap.getNode(table, (length - 1) & hash)) != null
        && (node = node.find(hash, key)) != null
        && Atomics.computeAndReplace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_EMPTY_OR_PRESENT, that -> remappingFunction.apply(key, Sentinel.unbox(that)))
    ) {
      // If the value was null and the new value is not null, then increment
      // the count. If the value was not null and the new value is null, then
      // decrement the count.
      if(entry.previous == null && entry.next != null) {
        count = 1L;
      } else if(entry.previous != null && entry.next == null) {
        count = -1L;
      }
    } else {
      // Only proceed here if the previous value was expunged or uncommitted.
      retry: for(; ; ) {
        table = this.immutableTable;
        length = table.length;

        if((node = SyncMap.getNode(table, (length - 1) & hash)) != null) {
          do {
            if(node.hash == hash && (node.key == key || node.key.equals(key))) {
              if(Atomics.computeAndReplace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_EXPUNGED, that -> remappingFunction.apply(key, Sentinel.unbox(that)))) {
                // Attempt to unexpunge the value and set the new value. If the
                // next value was not null, increment the count. If the next
                // value was null, decrement the count.
                if(entry.next != null) {
                  this.amendNode(hash, key, node.reference);

                  count = 1L;
                }

                break retry;
              } else if(Atomics.computeAndReplace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_EMPTY_OR_PRESENT, that -> remappingFunction.apply(key, Sentinel.unbox(that)))) {
                // If the value was not expunged then set the new value. If the
                // next value was not null, increment the count. If the next
                // value was null, decrement the count.
                if(entry.previous == null && entry.next != null) {
                  count = 1L;
                } else if(entry.previous != null && entry.next == null) {
                  count = -1L;
                }

                break retry;
              }

              continue retry;
            }
          } while((node = node.next) != null);
        }

        if((table = this.mutableTable) == null || (length = table.length) == 0) {
          this.amend();
        } else {
          if((node = SyncMap.getNode(table, index = (length - 1) & hash)) == null) {
            // Compute and check if the value is not null, if not then break.
            if((value != Sentinel.EMPTY ? value : (value = remappingFunction.apply(key, null))) == null) {
              entry.next = null;
              break;
            }

            // Try insert a new node if it hasn't previously existed, then
            // increment the count.
            if(SyncMap.replaceNode(table, index, new Node<>(hash, key, new ValueReference<>((V) value)))) {
              count = 1L;
              entry.next = value;
              break;
            }
          } else if(node.hash == SyncMap.NODE_MOVED) {
            // If the node has moved during a transfer, join the effort in
            // completion the transfer.
            this.forward((ForwardingNode<K, V>) node);
          } else {
            synchronized(node) {
              if(SyncMap.getNode(table, index) == node) {
                for(Node<K, V> previousNode = null; ; ) {
                  if(node.hash == hash && (node.key == key || node.key.equals(key))) {
                    Atomics.computeAndReplace(entry, SyncMap.NODE_VALUE, node.reference, that -> remappingFunction.apply(key, Sentinel.unbox(that)));

                    // If the value was null or expunged and the next value
                    // and the next value is not null, increment the count.
                    // If value was not null and the next value was null,
                    // decrement the count.
                    if(!Atomics.IS_PRESENT.test(entry.previous) && entry.next != null) {
                      count = 1L;
                    } else if(Atomics.IS_PRESENT.test(entry.previous) && entry.next == null) {
                      if(previousNode != null) {
                        previousNode.next = node.next;
                      } else {
                        SyncMap.setNode(table, index, node.next);
                      }

                      count = -1L;
                    }

                    break retry;
                  }

                  previousNode = node;
                  if((node = node.next) == null) {
                    if((value != Sentinel.EMPTY ? value : (value = remappingFunction.apply(key, null))) == null) {
                      // Compute and check if the value is not null, if not
                      // then break.
                      entry.next = null;
                      break retry;
                    }

                    // If the node does not exist in this bucket, create a new
                    // node and increment the count.
                    previousNode.next = new Node<>(hash, key, new ValueReference<>((V) value));

                    count = 1L;
                    entry.next = value;
                    break retry;
                  }
                }
              }
            }
          }
        }

        Thread.onSpinWait();
      }
    }

    if(count != 0L) this.addCount(count);
    return Sentinel.unbox(entry.next);
  }

  @Override
  public @Nullable V putIfAbsent(final @NotNull K key, final @NotNull V value) {
    requireNonNull(key, "key");
    requireNonNull(value, "value");

    final int hash = SyncMap.spread(key.hashCode());

    Node<K, V>[] table = this.immutableTable; Node<K, V> node;
    int length = table.length, index; long count = 0L;

    final Atomics.ValueEntry entry = new Atomics.ValueEntry();
    if((node = SyncMap.getNode(table, (length - 1) & hash)) != null
        && (node = node.find(hash, key)) != null
        && Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_NULL, value)) {
      // If the new value got committed (if it was null), then increment
      // the count.
      count = 1L;
    } else if(!Atomics.IS_PRESENT.test(entry.previous)) {
      retry: for(; ; ) {
        table = this.immutableTable;
        length = table.length;

        if((node = SyncMap.getNode(table, (length - 1) & hash)) != null) {
          do {
            if(node.hash == hash && (node.key == key || node.key.equals(key))) {
              if(Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_EXPUNGED, value)) {
                // Attempt to unexpunge the value and set the new value. If the
                // new value was committed, then add the node back to the
                // mutable table and increment the count.
                this.amendNode(hash, key, node.reference);

                count = 1L;
                break retry;
              } else if(Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_NULL, value)) {
                // If the value was null, increment the count.
                count = 1L;
                break retry;
              } else if(Atomics.IS_PRESENT.test(entry.previous)) {
                // If the value was present and not expunged, return.
                break retry;
              }

              continue retry;
            }
          } while((node = node.next) != null);
        }

        if((table = this.mutableTable) == null || (length = table.length) == 0) {
          this.amend();
        } else {
          if((node = SyncMap.getNode(table, index = (length - 1) & hash)) == null) {
            // Try insert a new node if it hasn't previously existed, then
            // increment the count.
            if(SyncMap.replaceNode(table, index, new Node<>(hash, key, new ValueReference<>(value)))) {
              count = 1L;
              break;
            }
          } else if(node.hash == SyncMap.NODE_MOVED) {
            // If the node has moved during a transfer, join the effort in
            // completing the transfer.
            this.forward((ForwardingNode<K, V>) node);
          } else {
            synchronized(node) {
              if(SyncMap.getNode(table, index) == node) {
                for(; ; ) {
                  if(node.hash == hash && (node.key == key || node.key.equals(key))) {
                    // If the new value got committed (if it was null or
                    // expunged), then increment the count.
                    if(Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_NULL_OR_EXPUNGED, value)) {
                      count = 1L;
                    }

                    break retry;
                  }

                  final Node<K, V> previousNode = node;
                  if((node = node.next) == null) {
                    // If the node does not exist in this bucket, create a new
                    // node and increment the count.
                    previousNode.next = new Node<>(hash, key, new ValueReference<>(value));

                    count = 1L;
                    break retry;
                  }
                }
              }
            }
          }
        }

        Thread.onSpinWait();
      }
    }

    if(count > 0L) this.addCount(count);
    return Sentinel.unbox(entry.previous);
  }

  @Override
  public @Nullable V put(final @NotNull K key, final @NotNull V value) {
    requireNonNull(key, "key");
    requireNonNull(value, "value");

    final int hash = SyncMap.spread(key.hashCode());

    Node<K, V>[] immutableTable = this.immutableTable;
    int length = immutableTable.length;
    Node<K, V> node = SyncMap.getNode(immutableTable, (length - 1) & hash);
    K nodeKey;

    // Fast-path: Update an existing value reference if possible, lock-free.
    while(node != null) {
      if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
        node = node.next;
        continue;
      }

      final Atomics.ValueEntry entry = new Atomics.ValueEntry();
      if(!Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_EMPTY_OR_PRESENT, value)) break;

      // If the new value got committed (if it was not expunged), then see if
      // the previous value was null to increase the count.
      if(entry.previous == null) this.addCount(1L);
      return Sentinel.unbox(entry.previous);
    }

    // Slow-path: Add a new value or update a value reference that has not been
    //            promoted yet, with locking.
    for(Node<K, V>[] mutableTable = this.mutableTable; ; ) {
      immutableTable = this.immutableTable;
      length = immutableTable.length;
      node = SyncMap.getNode(immutableTable, (length - 1) & hash);

      // Re-check the immutable table, even if it is unlikely it would have
      // updated during this action.
      if(node != null) {
        final Object current = this.immutableAmend(node, hash, key, value);
        if(current != Sentinel.EMPTY) return Sentinel.unbox(current);
      }

      // Now check the mutable table.
      final int index;
      if(!this.amended || mutableTable == null || (length = mutableTable.length) == 0) {
        this.amend();
      } else if((node = SyncMap.getNode(mutableTable, index = (length - 1) & hash)) == null) {
        // Try insert a new node if it hasn't previously existed, then
        // increment the count.
        if(SyncMap.replaceNode(mutableTable, index, new Node<>(hash, key, new ValueReference<>(value)))) {
          this.addCount(1L);
          return null;
        }
      } else if(node.hash == SyncMap.NODE_MOVED) {
        // If the node has moved during a resize transfer, follow
        // to the next table if possible.
        mutableTable = this.forward((ForwardingNode<K, V>) node);
        continue;
      } else {
        synchronized(node) {
          if(SyncMap.getNode(mutableTable, index) == node) {
            for(; ; ) {
              if(node.hash == hash && ((nodeKey = node.key) == key || nodeKey.equals(key))) {
                // If the node matches the hash and key, updates the value
                // and increment the count if the value was null or
                // expunged.
                final Atomics.ValueEntry entry = new Atomics.ValueEntry();
                Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, value);

                if(Atomics.IS_NULL_OR_EXPUNGED.test(entry.previous)) this.addCount(1L);
                return Sentinel.unbox(entry.previous);
              }

              final Node<K, V> previousNode = node;
              if((node = node.next) == null) {
                // If the node does not exist in this bucket, create a new
                // node and increment the count.
                previousNode.next = new Node<>(hash, key, new ValueReference<>(value));

                this.addCount(1L);
                return null;
              }
            }
          }
        }
      }

      mutableTable = this.mutableTable;
    }
  }

  private @Nullable Object immutableAmend(Node<K, V> node, final int hash, final K key, final V value) {
    K nodeKey;
    while(node != null) {
      if(node.hash != hash || ((nodeKey = node.key) != key && !nodeKey.equals(key))) {
        node = node.next;
        continue;
      }

      final ValueReference<V> reference = node.reference;
      final Object current = SyncMap.NODE_VALUE.getAcquire(reference);

      if(!SyncMap.NODE_VALUE.compareAndSet(reference, current, value)) continue;

      if(Atomics.IS_EXPUNGED.test(current)) {
        // Attempt to unexpunge the value and set the new value. If the
        // new value was committed, then add the node back to the
        // mutable table and increment the count.
        this.amendNode(hash, key, reference);

        this.addCount(1L);
        return null;
      } else {
        if(current == null) this.addCount(1L);
        return current;
      }
    }

    return Sentinel.EMPTY;
  }

  /* package */ void amendNode(final int hash, final @NotNull K key, final @NotNull ValueReference<V> reference) {
    Node<K, V>[] table; Node<K, V> node;
    int index, length;

    for(table = this.mutableTable; ; ) {
      if(!this.amended || table == null || (length = table.length) == 0) {
        // If the mutable table has been removed, we can't proceed.
        return;
      } else {
        if((node = SyncMap.getNode(table, index = (length - 1) & hash)) == null) {
          if(SyncMap.replaceNode(table, index, new Node<>(hash, key, reference))) {
            return;
          }
        } else if(node.hash == SyncMap.NODE_MOVED) {
          // If the node has moved during a resize transfer, follow
          // to the next table if possible.
          table = this.forward((ForwardingNode<K, V>) node);
        } else {
          synchronized(node) {
            if(SyncMap.getNode(table, index) == node) {
              for(; ; ) {
                if(node.hash == hash && (node.key == key || node.key.equals(key))) {
                  node.reference = reference;
                  return;
                }

                final Node<K, V> previous = node;
                if((node = node.next) == null) {
                  previous.next = new Node<>(hash, key, reference);
                  return;
                }
              }
            }
          }
        }
      }
    }
  }

  @Override
  public @Nullable V remove(final @NotNull Object key) {
    requireNonNull(key, "key");

    final int hash = SyncMap.spread(key.hashCode());

    Node<K, V>[] table = this.immutableTable; Node<K, V> node;
    int length = table.length; long count = 0L;

    final Atomics.ValueEntry entry = new Atomics.ValueEntry();
    if((node = SyncMap.getNode(table, (length - 1) & hash)) != null && (node = node.find(hash, key)) != null) {
      // Try update the node with the new value, only if it is present.
      if(Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_PRESENT, null)) {
        // If the new value got committed (if it was present and not expunged),
        // and the new value is null, then decrement the count.
        count = -1L;
      }
    } else if(this.amended && (table = this.mutableTable) != null) {
      length = table.length;

      // Only proceed here if the node did not exist in the immutable table.
      final int index;
      if((node = SyncMap.getNode(table, index = (length - 1) & hash)) != null) {
        synchronized(node) {
          if(SyncMap.getNode(table, index) == node) {
            for(Node<K, V> previousNode = null; ; ) {
              if(node.hash == hash && (node.key == key || node.key.equals(key))) {
                if(Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_PRESENT, null)) {
                  // If the value was present or expunged and the next value
                  // is null, remove the node and decrement the count.
                  if(previousNode != null) {
                    previousNode.next = node.next;
                  } else {
                    SyncMap.setNode(table, index, node.next);
                  }

                  count = -1L;
                  break;
                }

                break;
              }

              previousNode = node;

              if((node = node.next) == null) {
                break;
              }
            }
          }
        }
      }
    }

    if(count != 0L) this.addCount(count);
    return Sentinel.unbox(entry.previous);
  }

  @Override
  public boolean remove(final @NotNull Object key, final @NotNull Object value) {
    requireNonNull(key, "key");
    requireNonNull(value, "value");

    final int hash = SyncMap.spread(key.hashCode());

    Node<K, V>[] table = this.immutableTable; Node<K, V> node;
    int length = table.length; long count = 0L;

    if((node = SyncMap.getNode(table, (length - 1) & hash)) != null && (node = node.find(hash, key)) != null) {
      // Try update the node with the new value, only if it is present.
      if(Atomics.replace(SyncMap.NODE_VALUE, node.reference, value, null)) {
        // If the new value got committed (if it was present and not expunged),
        // and the new value is null, then decrement the count.
        count = -1L;
      }
    } else if(this.amended && (table = this.mutableTable) != null) {
      length = table.length;

      // Only proceed here if the node did not exist in the immutable table.
      final int index;
      if((node = SyncMap.getNode(table, index = (length - 1) & hash)) != null) {
        synchronized(node) {
          if(SyncMap.getNode(table, index) == node) {
            for(Node<K, V> previousNode = null; ; ) {
              if(node.hash == hash && (node.key == key || node.key.equals(key))) {
                if(Atomics.replace(SyncMap.NODE_VALUE, node.reference, value, null)) {
                  // If the value was present or expunged and the next value
                  // is null, remove the node and decrement the count.
                  if(previousNode != null) {
                    previousNode.next = node.next;
                  } else {
                    SyncMap.setNode(table, index, node.next);
                  }

                  count = -1L;
                }

                break;
              }

              previousNode = node;

              if((node = node.next) == null) {
                break;
              }
            }
          }
        }
      }
    }

    if(count != 0L) this.addCount(count);
    return count < 0L;
  }

  @Override
  public @Nullable V replace(final @NotNull K key, final @NotNull V value) {
    requireNonNull(key, "key");
    requireNonNull(value, "value");

    final int hash = SyncMap.spread(key.hashCode());

    Node<K, V>[] table = this.immutableTable; Node<K, V> node;
    int length = table.length;

    final Atomics.ValueEntry entry = new Atomics.ValueEntry();
    if((node = SyncMap.getNode(table, (length - 1) & hash)) != null && (node = node.find(hash, key)) != null) {
      // Try update the node with the new value, only if it is present.
      Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_PRESENT, value);
    } else if(this.amended && (table = this.mutableTable) != null) {
      length = table.length;

      // Only proceed here if the node did not exist in the immutable table.
      final int index;
      if((node = SyncMap.getNode(table, index = (length - 1) & hash)) != null) {
        synchronized(node) {
          if(SyncMap.getNode(table, index) == node) {
            for(; ; ) {
              if(node.hash == hash && (node.key == key || node.key.equals(key))) {
                Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, Atomics.IS_PRESENT, value);
                break;
              }

              if((node = node.next) == null) {
                break;
              }
            }
          }
        }
      }
    }

    return Sentinel.unbox(entry.previous);
  }

  @Override
  public boolean replace(final @NotNull K key, final @NotNull V oldValue, final @NotNull V newValue) {
    requireNonNull(key, "key");
    requireNonNull(oldValue, "oldValue");
    requireNonNull(newValue, "newValue");

    final int hash = SyncMap.spread(key.hashCode());

    Node<K, V>[] table = this.immutableTable; Node<K, V> node;
    int length = table.length;

    if((node = SyncMap.getNode(table, (length - 1) & hash)) != null && (node = node.find(hash, key)) != null) {
      // Try update the node with the new value, only if it is present.
      return Atomics.replace(SyncMap.NODE_VALUE, node.reference, oldValue, newValue);
    } else if(this.amended && (table = this.mutableTable) != null) {
      length = table.length;

      // Only proceed here if the node did not exist in the immutable table.
      final int index;
      if((node = SyncMap.getNode(table, index = (length - 1) & hash)) != null) {
        synchronized(node) {
          if(SyncMap.getNode(table, index) == node) {
            do {
              if(node.hash == hash && (node.key == key || node.key.equals(key))) {
                return Atomics.replace(SyncMap.NODE_VALUE, node.reference, oldValue, newValue);
              }
            } while((node = node.next) != null);
          }
        }
      }
    }

    return false;
  }

  // Bulk Operations

  @Override
  public void forEach(final @NotNull BiConsumer<? super @NotNull K, ? super @NotNull V> action) {
    requireNonNull(action, "action");

    this.promote();

    final Traverser<K, V> traverser = new Traverser<>(this.immutableTable);

    Node<K, V> node; V value;
    while((node = traverser.advanceNode()) != null) {
      if((value = Sentinel.unbox(SyncMap.NODE_VALUE.getAcquire(node.reference))) != null) {
        action.accept(node.key, value);
      }
    }
  }

  @Override
  public void clear() {
    this.promote();

    final Traverser<K, V> traverser = new Traverser<>(this.immutableTable);

    Node<K, V> node; long count = 0L;

    final Atomics.ValueEntry entry = new Atomics.ValueEntry();
    while((node = traverser.advanceNode()) != null) {
      Atomics.replace(entry, SyncMap.NODE_VALUE, node.reference, null);
      if(Atomics.IS_PRESENT.test(entry.previous)) count--;
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
    Node<K, V>[] source; OperationToken token;

    while(this.amended
      && (source = this.mutableTable) != null
      && source.length > 0) {
      // Attempt to acquire the promotion lock, or if the promotion lock has
      // already been retrieved, break.
      if((token = this.createToken(SyncMap.TRANSFER_PROMOTE)) != null) {
        if(!this.amended || source != this.mutableTable) {
          this.resetToken(token);

          Thread.onSpinWait();
          continue;
        }

        this.misses.reset();
        this.amended = false;
        this.mutableTable = null;
        this.immutableTable = source;

        this.resetToken(token);
        break;
      } else if(this.getToken(SyncMap.TRANSFER_PROMOTE) != null) {
        break;
      }
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
    Node<K, V>[] source, destination; OperationToken token;
    int length;

    while(this.amended
      && (source = this.mutableTable) != null
      && (length = source.length) > 0
      && (this.size.sum() * this.loadFactor) >= length) {
      // Attempt to acquire the resize lock, or if resize lock has already
      // been retrieved try join the operation.
      if((token = this.createToken(SyncMap.TRANSFER_RESIZE)) != null) {
        // Re-check the tables exist, before we continue. If they have been
        // removed, reset and restart the operation.
        if(!this.amended || source != this.mutableTable) {
          this.resetToken(token);
          continue;
        }

        this.transferIndex = length;
        this.transferTable = destination = new Node[length << 1];
      } else if((token = this.getToken(SyncMap.TRANSFER_RESIZE)) == null) {
        // If the current operation we're looking to participate with is not
        // running, break.
        break;
      } else if((destination = this.transferTable) == null) {
        // Ensure the destination table has been created, otherwise spin.
        Thread.onSpinWait();
        continue;
      }

      try {
        final boolean finalize;
        if(this.joinToken(token)) {
          try {
            // Check if the state has changed once we have joined the operation.
            // If it has, spin this back to the beginning of the loop.
            if(this.amended
              && source == this.mutableTable
              && destination == this.transferTable
              && this.transfer(source, destination, true) >= length) {
              this.achieveToken(token);
            }
          } finally {
            // Complete this thread work for the operation and store whether
            // the task was completed.
            finalize = this.completeToken(token);
          }
        } else {
          // If we were not able to join this operation, stop trying.
          break;
        }

        // Finish the operation now that all threads have completed their work.
        if(finalize) {
          this.transferIndex = 0;
          this.transferProgress = 0;

          this.transferTable = null;
          this.mutableTable = destination;

          this.finalizeToken(token);
        }

        break;
      } finally {
        if(this.shouldResetToken(token)) {
          this.resetToken(token);
        }
      }
    }
  }

  /**
   * Creates a new mutable table from the immutable table, when new nodes are
   * required and transfers the nodes to it.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  /* package */ void amend() {
    Node<K, V>[] destination; OperationToken token;

    while(!this.amended && this.mutableTable == null) {
      final Node<K, V>[] source = this.immutableTable;
      final int length = source.length;

      // Attempt to acquire the amend lock, or if amend lock has already
      // been retrieved try join the operation.
      if((token = this.createToken(SyncMap.TRANSFER_AMEND)) != null) {
        // Re-check the tables exist, before we continue. If they have been
        // removed, reset and restart the operation.
        if(this.amended || source != this.immutableTable || this.mutableTable != null) {
          this.resetToken(token);
          continue;
        }

        this.transferIndex = length;
        this.transferTable = destination = new Node[length];
      } else if((token = this.getToken(SyncMap.TRANSFER_AMEND)) == null) {
        // If the current operation we're looking to participate with is not
        // running, break.
        break;
      } else if((destination = this.transferTable) == null) {
        // Ensure the destination table has been created, otherwise spin.
        Thread.onSpinWait();
        continue;
      }

      try {
        final boolean finalize;
        if(this.joinToken(token)) {
          try {
            // Check if the state has changed once we have joined the operation.
            // If it has, spin this back to the beginning of the loop.
            if(!this.amended
              && source == this.immutableTable
              && destination == this.transferTable
              && this.transfer(source, destination, false) >= length) {
              this.achieveToken(token);
            }
          } finally {
            // Complete this thread work for the operation and store whether
            // the task was completed.
            finalize = this.completeToken(token);
          }
        } else {
          // If we were not able to join this operation, stop trying.
          break;
        }

        // Finish the operation now that all threads have completed their work.
        if(finalize) {
          this.transferIndex = 0;
          this.transferProgress = 0;

          this.transferTable = null;
          this.mutableTable = destination;
          this.amended = true;

          this.finalizeToken(token);
        }

        break;
      } finally {
        if(this.shouldResetToken(token)) {
          this.resetToken(token);
        }
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

      // Claim the range of indexes we are going to transfer.
      for(; ; ) {
        if((index = (int) SyncMap.TRANSFER_INDEX.get(this)) <= 0 || finished) {
          index = -1;
          break;
        } else if(SyncMap.TRANSFER_INDEX.compareAndSet(this, index, bound = (index > stride ? index - stride : 0))) {
          delta = index - bound;
          break;
        }

        Thread.onSpinWait();
      }

      // Start iterating the nodes in the range that was claimed and move them
      // to the destination table.
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

              while((node = next) != null) {
                next = node.next;

                if(!resize && (SyncMap.NODE_VALUE.getAcquire(node.reference) == Sentinel.EXPUNGED || Atomics.replace(SyncMap.NODE_VALUE, node.reference, null, Sentinel.EXPUNGED))) {
                  continue;
                }

                final Node<K, V> cloned = new Node<>(node.hash, node.key, node.reference);
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

      // Now that the range has been transferred, increase the progress.
      if(delta > 0) {
        for(; ; ) {
          if((progress = (int) SyncMap.TRANSFER_PROGRESS.getAcquire(this)) >= capacity) break;
          if(SyncMap.TRANSFER_PROGRESS.compareAndSet(this, progress, progress + delta)) {
            progress += delta;
            delta = 0;
            break;
          }

          Thread.onSpinWait();
        }
      }
    }

    return progress;
  }

  /* ------------------------- < Value Reference > ------------------------- */

  /**
   * Represents a holder for a value that can be shared across nodes, usually
   * when the node has been cloned.
   *
   * @param <T> the value type
   */
  @SuppressWarnings("FieldMayBeFinal")
  /* package */ static final class ValueReference<T> {
    private transient volatile Object value;

    /* package */ ValueReference(final @Nullable T value) {
      this.value = value;
    }
  }

  /* ------------------------------ < Nodes > ------------------------------ */

  /**
   * Represents a key-value pair in this map with the {@link ValueReference}
   * holding the value. The key and hash would usually be set, except when
   * the node is special, the hash would be negative and the key {@code null}.
   *
   * @param <K> the key type
   * @param <V> the value type
   */
  /* package */ static class Node<K, V> {
    /* package */ final int hash;
    /* package */ final K key;
    /* package */ volatile ValueReference<V> reference;
    /* package */ volatile Node<K, V> next;

    /* package */ Node(final int hash, final K key, final ValueReference<V> reference) {
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

  /* ------------------------------ < Token > ------------------------------ */

  /**
   * Returns a new {@link OperationToken} if the operation was successfully
   * started, otherwise returns {@code null}.
   *
   * @param operation the operation
   * @return a new token, otherwise null
   */
  private @Nullable OperationToken createToken(final int operation) {
    OperationToken token;
    for(; ; ) {
      if(SyncMap.OPERATION_TOKEN.getAcquire(this) != null) return null;
      if(SyncMap.OPERATION_TOKEN.compareAndSet(this, null, token = new OperationToken(operation))) {
        return token;
      }

      Thread.onSpinWait();
    }
  }

  /**
   * Returns the current {@link OperationToken} if it matches the given
   * {@code operation} code, otherwise returns {@code null}.
   *
   * @param operation the operation
   * @return the current token, if valid, otherwise null
   */
  private @Nullable OperationToken getToken(final int operation) {
    final OperationToken token;
    if((token = ((OperationToken) SyncMap.OPERATION_TOKEN.getAcquire(this))) != null
      && token.operation == operation) {
      return token;
    }

    return null;
  }

  /**
   * Returns {@code true} if the current {@link Thread} can successfully
   * participate in this operation, otherwise returns {@code false}.
   *
   * @param token the operation token
   * @return true if joined, otherwise false
   */
  private boolean joinToken(final @NotNull OperationToken token) {
    for(; ; ) {
      final int threads = token.threads;
      if(this.invalidToken(token) || token.state != SyncMap.STATE_IDLE || threads >= SyncMap.MAXIMUM_TRANSFER_THREADS) return false;
      if(SyncMap.OPERATION_THREADS.compareAndSet(token, threads, threads + 1)) {
        return true;
      }

      Thread.onSpinWait();
    }
  }

  /**
   * Marks the {@link OperationToken} with the {@link SyncMap#STATE_ACHIEVED}.
   *
   * @param token the token
   */
  private void achieveToken(final @NotNull OperationToken token) {
    for(; ; ) {
      if(this.invalidToken(token) || token.state != SyncMap.STATE_IDLE) return;
      if(SyncMap.OPERATION_STATE.compareAndSet(token, SyncMap.STATE_IDLE, STATE_ACHIEVED)) {
        return;
      }

      Thread.onSpinWait();
    }
  }

  /**
   * Returns {@code true} if the current {@link Thread} is able to complete the
   * operation, otherwise returns {@code false}.
   *
   * @param token the token
   * @return true if completed, otherwise false
   */
  private boolean completeToken(final @NotNull OperationToken token) {
    int threads;
    for(; ; ) {
      threads = token.threads;
      if(SyncMap.OPERATION_THREADS.compareAndSet(token, threads, threads - 1)) {
        threads--;
        break;
      }

      Thread.onSpinWait();
    }

    for(; ; ) {
      final int state = token.state;
      if(threads > 0 || state != SyncMap.STATE_ACHIEVED) return false;
      if(SyncMap.OPERATION_STATE.compareAndSet(token, SyncMap.STATE_ACHIEVED, SyncMap.STATE_COMPLETED)) {
        return true;
      }

      Thread.onSpinWait();
    }
  }

  /**
   * Marks the {@link OperationToken} with the {@link SyncMap#STATE_FINALIZED}.
   *
   * @param token the token
   */
  private void finalizeToken(final @NotNull OperationToken token) {
    for(; ; ) {
      final int state = token.state;
      if(this.invalidToken(token) || state != SyncMap.STATE_COMPLETED) return;
      if(SyncMap.OPERATION_STATE.compareAndSet(token, SyncMap.STATE_COMPLETED, SyncMap.STATE_FINALIZED)) {
        break;
      }

      Thread.onSpinWait();
    }
  }

  /**
   * Returns {@code true} if the token should be reset, otherwise {@code false}.
   *
   * @param token the token
   * @return true if it should be reset, otherwise false
   */
  private boolean shouldResetToken(final @NotNull OperationToken token) {
    return !this.invalidToken(token)
      && token.threads <= 0
      && (token.state == SyncMap.STATE_FINALIZED || token.state == SyncMap.STATE_IDLE);
  }

  /**
   * Resets the current operation, if the given token is valid.
   *
   * @param token the token
   */
  private void resetToken(final @NotNull OperationToken token) {
    SyncMap.OPERATION_TOKEN.compareAndSet(this, token, null);
  }

  /**
   * Returns {@code true} if the given token is invalid, otherwise returns
   * {@code false}.
   *
   * @param token the token
   * @return true if the token is invalid, otherwise false
   */
  private boolean invalidToken(final @NotNull OperationToken token) {
    return ((OperationToken) SyncMap.OPERATION_TOKEN.getAcquire(this)) != token;
  }

  /**
   * Represents a unique token for an operation that has been started.
   */
  @SuppressWarnings("unused")
  /* package */ static final class OperationToken {
    /* package */ final int operation;

    private volatile int threads;
    private volatile int state;

    /* package */ OperationToken(final int operation) {
      this.operation = operation;
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

    private void advanceEntry() {
      this.next = null;

      Node<K, V> node;
      V value;
      while((node = this.advanceNode()) != null) {
        if((value = Sentinel.unbox(SyncMap.NODE_VALUE.getAcquire(node.reference))) != null) {
          this.next = new MapEntry(node.key, value);
          break;
        }
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
