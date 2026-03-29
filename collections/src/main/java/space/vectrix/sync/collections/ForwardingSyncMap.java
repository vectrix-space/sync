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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import org.jetbrains.annotations.UnknownNullability;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Represents a forwarding hash table capable of highly concurrent retrievals
 * and updates, internally backed by a non-thread-safe map. The main difference
 * is with how this map carefully manages two separate maps. One map is
 * immutable providing efficient retrieval and updates of existing and
 * frequently accessed key-value pairs that is fully lock-free. Whereas the
 * mutable map allows new key-value pairs but involves a map-level lock.
 *
 * <p>This map is optimized for cases where updates and retrievals for existing
 * entries is a lot faster than recently added entries. However, the
 * performance in other cases are close to that of the backing map which is
 * faster than a traditional map paired with a read and write lock, or maps
 * with an exclusive lock (such as using
 * {@link Collections#synchronizedMap(Map)}) in similar scenarios.</p>
 *
 * @author vectrix
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.1.0
 */
public class ForwardingSyncMap<K extends @Nullable Object, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Hash {

  /* --- < Constants > ----------------------------------------------------- */

  /**
   * Represents a sentinel value for an expunged object reference.
   */
  private static final Object EXPUNGED = new Object();

  /* --- < Fields > -------------------------------------------------------- */

  /**
   * Represents the map creation function for the backing map.
   */
  private final transient IntFunction<Map<K, ObjectReference>> function;

  /**
   * Represents the implicit lock for the mutable backing map.
   */
  private final transient Object lock = new Object();

  /**
   * Represents an immutable hash map that allows fast retrieval and updates
   * without locking.
   */
  private transient volatile Map<K, ObjectReference> immutable;

  /**
   * Represents a mutable hash map that allows updates of new values with
   * locking.
   */
  private transient @UnknownNullability Map<K, ObjectReference> mutable;

  /**
   * Represents whether the mutable map has been amended by the immutable map.
   */
  private transient volatile boolean amended;

  /**
   * Represents the amount of times the immutable map has been missed for
   * the mutable map.
   */
  private transient int misses;

  /**
   * Represents a view of the entries in this map.
   */
  private transient @Nullable EntrySet entrySet;

  /* --- < Public Operations > --------------------------------------------- */

  /**
   * Initializes a new {@link ForwardingSyncMap} with the given map creation
   * function and {@link ForwardingSyncMap#DEFAULT_CAPACITY}.
   *
   * @param function the map creation function
   * @since 1.1.0
   */
  public ForwardingSyncMap(final IntFunction<Map<K, ObjectReference>> function) {
    this(function, ForwardingSyncMap.DEFAULT_CAPACITY);
  }

  /**
   * Initializes a new {@link ForwardingSyncMap} with the given map creation
   * function and given initial capacity.
   *
   * @param function the map creation function
   * @param initialCapacity the initial capacity
   * @since 1.1.0
   */
  public ForwardingSyncMap(final IntFunction<Map<K, ObjectReference>> function, final int initialCapacity) {
    if(initialCapacity < 0) throw new IllegalArgumentException("Initial capacity must be non-negative");
    this.function = function;
    this.immutable = this.function.apply(initialCapacity);
  }

  @Override
  public int size() {
    this.promote();

    int size = 0;
    for(final ObjectReference value : this.immutable.values()) {
      if(value.valueExists()) size++;
    }

    return size;
  }

  @Override
  public boolean isEmpty() {
    this.promote();

    for(final ObjectReference value : this.immutable.values()) {
      if(value.valueExists()) return false;
    }

    return true;
  }

  @Override
  public boolean containsKey(final @Nullable Object key) {
    final ObjectReference reference = this.getEntry(key);
    return reference != null && reference.valueExists();
  }

  @Override
  public @Nullable V get(final @Nullable Object key) {
    final ObjectReference reference = this.getEntry(key);
    return reference != null ? reference.value() : null;
  }

  @Override
  public V getOrDefault(final @Nullable Object key, final V defaultValue) {
    final ObjectReference reference = this.getEntry(key);
    return reference != null ? reference.valueOr(defaultValue) : defaultValue;
  }

  @SuppressWarnings("SuspiciousMethodCalls")
  private @Nullable ObjectReference getEntry(final @Nullable Object key) {
    ObjectReference reference = this.immutable.get(key);
    if(reference == null && this.amended) {
      synchronized(this.lock) {
        if((reference = this.immutable.get(key)) == null && this.amended && this.mutable != null) {
          reference = this.mutable.get(key);
          // The slow path should be avoided, even if the value does not match
          // or is present. So we mark a miss to eventually promote and take a
          // faster path.
          this.missLocked();
        }
      }
    }

    return reference;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V computeIfAbsent(final @Nullable K key, final Function<? super K, ? extends @Nullable V> mappingFunction) {
    requireNonNull(mappingFunction, "mappingFunction");

    ObjectReference reference;
    Object previous;
    V next;

    if((reference = this.immutable.get(key)) != null) {
      previous = reference.get();
      while(previous != ForwardingSyncMap.EXPUNGED) {
        if(previous != null) return (V) previous;

        next = mappingFunction.apply(key);
        if(next == null) return null;

        final Object witness = reference.compareAndExchange(null, next);
        if(witness != null) {
          previous = witness;
          Thread.onSpinWait();
          continue;
        }

        return next;
      }
    }

    synchronized(this.lock) {
      if((reference = this.immutable.get(key)) != null) {
        previous = reference.get();
        for(; ; ) {
          if(previous != null && previous != ForwardingSyncMap.EXPUNGED) return (V) previous;

          next = mappingFunction.apply(key);
          if(next == null) return null;

          final Object witness = reference.compareAndExchange(previous, next);
          if(witness != previous) {
            previous = witness;
            Thread.onSpinWait();
            continue;
          }

          if(witness == ForwardingSyncMap.EXPUNGED) {
            this.mutable.put(key, reference);
          }

          break;
        }
      } else if(this.mutable != null && (reference = this.mutable.get(key)) != null) {
        previous = reference.get();
        for(; ; ) {
          if(previous != null && previous != ForwardingSyncMap.EXPUNGED) return (V) previous;

          next = mappingFunction.apply(key);
          if(next == null) return null;

          final Object witness = reference.compareAndExchange(previous, next);
          if(witness != previous) {
            previous = witness;
            Thread.onSpinWait();
            continue;
          }

          break;
        }
      } else {
        if(!this.amended) {
          this.amendLocked();
          this.amended = true;
        }

        next = mappingFunction.apply(key);
        if(next != null) this.mutable.put(key, new ObjectReference(next));
      }
    }

    return next;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V computeIfPresent(final @Nullable K key, final BiFunction<? super K, ? super @Nullable V, ? extends @Nullable V> remappingFunction) {
    requireNonNull(remappingFunction, "remappingFunction");

    ObjectReference reference;
    Object previous;

    V next = null;

    if((reference = this.immutable.get(key)) != null) {
      previous = reference.get();
      while(previous != ForwardingSyncMap.EXPUNGED) {
        if(previous == null) return null;

        next = remappingFunction.apply(key, (V) previous);

        final Object witness = reference.compareAndExchange(previous, next);
        if(witness != previous) {
          previous = witness;
          Thread.onSpinWait();
          continue;
        }

        return next;
      }
    }

    synchronized(this.lock) {
      if((reference = this.immutable.get(key)) != null) {
        previous = reference.get();
        for(; ; ) {
          if(previous == null || previous == ForwardingSyncMap.EXPUNGED) return null;

          next = remappingFunction.apply(key, (V) previous);

          final Object witness = reference.compareAndExchange(previous, next);
          if(witness != previous) {
            previous = witness;
            Thread.onSpinWait();
            continue;
          }

          break;
        }
      } else if(this.mutable != null && (reference = this.mutable.get(key)) != null) {
        previous = reference.get();
        for(; ; ) {
          if(previous == null || previous == ForwardingSyncMap.EXPUNGED) return null;

          next = remappingFunction.apply(key, (V) previous);

          final Object witness = reference.compareAndExchange(previous, next);
          if(witness != previous) {
            previous = witness;
            Thread.onSpinWait();
            continue;
          }

          if(next == null) {
            this.mutable.remove(key);
            return null;
          }

          break;
        }
      }
    }

    return next;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V compute(final @Nullable K key, final BiFunction<? super K, ? super @Nullable V, ? extends @Nullable V> remappingFunction) {
    requireNonNull(remappingFunction, "remappingFunction");

    ObjectReference reference;
    Object previous;
    V next;

    if((reference = this.immutable.get(key)) != null) {
      previous = reference.get();
      while(previous != ForwardingSyncMap.EXPUNGED) {
        next = remappingFunction.apply(key, (V) previous);

        final Object witness = reference.compareAndExchange(previous, next);
        if(witness != previous) {
          previous = witness;
          Thread.onSpinWait();
          continue;
        }

        return next;
      }
    }

    synchronized(this.lock) {
      if((reference = this.immutable.get(key)) != null) {
        previous = reference.get();
        for(; ; ) {
          next = remappingFunction.apply(key, previous == ForwardingSyncMap.EXPUNGED ? null : (V) previous);

          final Object witness = reference.compareAndExchange(previous, next);
          if(witness != previous) {
            previous = witness;
            Thread.onSpinWait();
            continue;
          }

          if(witness == ForwardingSyncMap.EXPUNGED) {
            this.mutable.put(key, reference);
          }

          break;
        }
      } else if(this.mutable != null && (reference = this.mutable.get(key)) != null) {
        previous = reference.get();
        for(; ; ) {
          next = remappingFunction.apply(key, previous == ForwardingSyncMap.EXPUNGED ? null : (V) previous);

          final Object witness = reference.compareAndExchange(previous, next);
          if(witness != previous) {
            previous = witness;
            Thread.onSpinWait();
            continue;
          }

          if(next == null) {
            this.mutable.remove(key);
            return null;
          }

          break;
        }
      } else {
        if(!this.amended) {
          this.amendLocked();
          this.amended = true;
        }

        next = remappingFunction.apply(key, null);
        if(next != null) this.mutable.put(key, new ObjectReference(next));
      }
    }

    return next;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V putIfAbsent(final @Nullable K key, final V value) {
    requireNonNull(value, "value");

    ObjectReference reference;
    Object previous;

    if((reference = this.immutable.get(key)) != null) {
      previous = reference.get();
      while(previous != ForwardingSyncMap.EXPUNGED) {
        if(previous != null) return (V) previous;

        final Object witness = reference.compareAndExchange(null, value);
        if(witness != null) {
          previous = witness;
          Thread.onSpinWait();
          continue;
        }

        return null;
      }
    }

    synchronized(this.lock) {
      if((reference = this.immutable.get(key)) != null) {
        previous = reference.get();
        for(; ; ) {
          if(previous != null && previous != ForwardingSyncMap.EXPUNGED) return (V) previous;

          final Object witness = reference.compareAndExchange(previous, value);
          if(witness != previous) {
            previous = witness;
            Thread.onSpinWait();
            continue;
          }

          if(witness == ForwardingSyncMap.EXPUNGED) {
            this.mutable.put(key, reference);
            return null;
          }

          break;
        }
      } else if(this.mutable != null && (reference = this.mutable.get(key)) != null) {
        previous = reference.get();
        for(; ; ) {
          if(previous != null && previous != ForwardingSyncMap.EXPUNGED) return (V) previous;

          final Object witness = reference.compareAndExchange(previous, value);
          if(witness != previous) {
            previous = witness;
            Thread.onSpinWait();
            continue;
          }

          if(witness == ForwardingSyncMap.EXPUNGED) {
            return null;
          }

          break;
        }
      } else {
        if(!this.amended) {
          this.amendLocked();
          this.amended = true;
        }

        assert this.mutable != null;
        this.mutable.put(key, new ObjectReference(value));
        return null;
      }
    }

    return (V) previous;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V put(final @Nullable K key, final V value) {
    requireNonNull(value, "value");

    ObjectReference reference;
    Object previous;

    if((reference = this.immutable.get(key)) != null) {
      previous = reference.get();
      while(previous != ForwardingSyncMap.EXPUNGED) {
        final Object witness = reference.compareAndExchange(previous, value);
        if(witness != previous) {
          previous = witness;
          Thread.onSpinWait();
          continue;
        }

        return (V) previous;
      }
    }

    synchronized(this.lock) {
      if((reference = this.immutable.get(key)) != null) {
        previous = reference.get();
        for(; ; ) {
          final Object witness = reference.compareAndExchange(previous, value);
          if(witness != previous) {
            previous = witness;
            Thread.onSpinWait();
            continue;
          }

          if(witness == ForwardingSyncMap.EXPUNGED) {
            this.mutable.put(key, reference);
            return null;
          }

          break;
        }
      } else if(this.mutable != null && (reference = this.mutable.get(key)) != null) {
        previous = reference.get();
        for(; ; ) {
          final Object witness = reference.compareAndExchange(previous, value);
          if(witness != previous) {
            previous = witness;
            Thread.onSpinWait();
            continue;
          }

          if(witness == ForwardingSyncMap.EXPUNGED) {
            return null;
          }

          break;
        }
      } else {
        if(!this.amended) {
          this.amendLocked();
          this.amended = true;
        }

        assert this.mutable != null;
        this.mutable.put(key, new ObjectReference(value));
        return null;
      }
    }

    return (V) previous;
  }

  @Override
  @SuppressWarnings({"unchecked", "SuspiciousMethodCalls"})
  public @Nullable V remove(final @Nullable Object key) {
    ObjectReference reference;
    Object previous;

    if((reference = this.immutable.get(key)) != null) {
      previous = reference.get();
      while(previous != null && previous != ForwardingSyncMap.EXPUNGED) {
        final Object witness = reference.compareAndExchange(previous, null);
        if(witness != previous) {
          previous = witness;
          Thread.onSpinWait();
          continue;
        }

        return (V) previous;
      }
    }

    synchronized(this.lock) {
      if(this.immutable.get(key) == null && this.amended && (reference = this.mutable.remove(key)) != null) {
        return reference.value();
      }
    }

    return null;
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public boolean remove(final @Nullable Object key, final Object value) {
    ObjectReference reference;
    Object previous;

    if((reference = this.immutable.get(key)) != null) {
      previous = reference.get();
      while(previous != null && previous != ForwardingSyncMap.EXPUNGED) {
        if(!Objects.equals(previous, value)) return false;

        final Object witness = reference.compareAndExchange(previous, null);
        if(witness != previous) {
          previous = witness;
          Thread.onSpinWait();
          continue;
        }

        return true;
      }
    }

    synchronized(this.lock) {
      if(this.immutable.get(key) == null && this.amended && (reference = this.mutable.get(key)) != null) {
        boolean removed = false;

        previous = reference.get();
        while(previous != null && previous != ForwardingSyncMap.EXPUNGED) {
          if(!Objects.equals(previous, value)) return false;

          final Object witness = reference.compareAndExchange(previous, null);
          if(witness != previous) {
            previous = witness;
            Thread.onSpinWait();
            continue;
          }

          removed = true;
          break;
        }

        this.mutable.remove(key);
        return removed;
      }
    }

    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable V replace(final @Nullable K key, final V value) {
    requireNonNull(value, "value");

    final ObjectReference reference = this.getEntry(key);
    if(reference != null) {
      Object previous = reference.get();
      for(; ; ) {
        if(previous == null || previous == ForwardingSyncMap.EXPUNGED) return null;

        final Object witness = reference.compareAndExchange(previous, value);
        if(witness != previous) {
          previous = witness;
          Thread.onSpinWait();
          continue;
        }

        return (V) previous;
      }
    }

    return null;
  }

  @Override
  public boolean replace(final @Nullable K key, final V oldValue, final V newValue) {
    requireNonNull(oldValue, "oldValue");
    requireNonNull(newValue, "newValue");

    final ObjectReference reference = this.getEntry(key);
    if(reference != null) {
      Object previous = reference.get();
      for(; ; ) {
        if(previous == null || previous == ForwardingSyncMap.EXPUNGED) return false;
        if(!Objects.equals(previous, oldValue)) return false;

        final Object witness = reference.compareAndExchange(previous, newValue);
        if(witness != previous) {
          previous = witness;
          Thread.onSpinWait();
          continue;
        }

        return true;
      }
    }

    return false;
  }

  // Bulk Operations

  @Override
  public void forEach(final BiConsumer<? super K, ? super V> action) {
    requireNonNull(action, "action");
    this.promote();

    V value;
    for(final Map.Entry<K, ObjectReference> that : this.immutable.entrySet()) {
      if((value = that.getValue().value()) != null) {
        action.accept(that.getKey(), value);
      }
    }
  }

  @Override
  public void clear() {
    synchronized(this.lock) {
      this.immutable = this.function.apply(this.immutable.size());
      this.mutable = null;
      this.amended = false;
      this.misses = 0;
    }
  }

  // Views

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    final EntrySet entrySet;
    if((entrySet = this.entrySet) != null) return entrySet;
    return this.entrySet = new EntrySet();
  }

  /* --- < Private Operations > -------------------------------------------- */

  /**
   * Promotes the mutable map to the immutable map if the mutable map has been
   * amended after first locking.
   */
  private void promote() {
    if(this.amended) {
      synchronized(this.lock) {
        if(this.amended) {
          this.promoteLocked();
        }
      }
    }
  }

  /**
   * Records a missed attempt to grab a value from the immutable map. If the
   * missed attempts exceeds the amount of elements in the map, the mutable
   * map will then be promoted to the immutable map.
   */
  private void missLocked() {
    this.misses++;

    if(this.misses < this.mutable.size()) return;
    this.promoteLocked();
  }

  /**
   * Promotes the mutable map to the immutable map.
   */
  private void promoteLocked() {
    this.immutable = this.mutable;
    this.mutable = null;
    this.amended = false;
    this.misses = 0;
  }

  /**
   * Creates a new mutable map from the immutable map and transfers the entries
   * to it.
   */
  private void amendLocked() {
    if(this.mutable != null) return;

    final Map<K, ObjectReference> next = this.function.apply(this.immutable.size());
    for(final Map.Entry<K, ObjectReference> entry : this.immutable.entrySet()) {
      if(!entry.getValue().expunge()) {
        next.put(entry.getKey(), entry.getValue());
      }
    }

    this.mutable = next;
  }

  /* --- < Object Reference > ---------------------------------------------- */

  /**
   * Represents a value holder for sharing across nodes in the immutable and
   * mutable maps, providing atomic updates for the underlying value.
   */
  @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
  public static final class ObjectReference {
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
      return (value = ObjectReference.VALUE.getOpaque(this)) != null && value != ForwardingSyncMap.EXPUNGED;
    }

    @SuppressWarnings("unchecked")
    private <V> @Nullable V value() {
      final Object value;
      return (value = ObjectReference.VALUE.getAcquire(this)) != ForwardingSyncMap.EXPUNGED ? (V) value : null;
    }

    @SuppressWarnings("unchecked")
    private <V> V valueOr(final V defaultValue) {
      final Object value;
      return ((value = ObjectReference.VALUE.getAcquire(this)) != null && value != ForwardingSyncMap.EXPUNGED) ? (V) value : defaultValue;
    }

    private @Nullable Object get() {
      return ObjectReference.VALUE.getAcquire(this);
    }

    private boolean expunge() {
      return ObjectReference.VALUE.compareAndExchangeRelease(this, null, ForwardingSyncMap.EXPUNGED) == null;
    }

    private @Nullable Object compareAndExchange(final @Nullable Object expect, final @Nullable Object update) {
      return ObjectReference.VALUE.compareAndExchangeRelease(this, expect, update);
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
      final V previous = ForwardingSyncMap.this.put(this.key, value);
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
      return "ForwardingSyncMap.Entry{key=" + this.key + ", value=" + this.value + "}";
    }
  }

  /**
   * Represents a view of the map entries.
   */
  private final class EntrySet extends AbstractSet<Entry<K, V>> {
    @Override
    public int size() {
      return ForwardingSyncMap.this.size();
    }

    @Override
    public boolean contains(final @Nullable Object entry) {
      if(!(entry instanceof final Map.Entry<?,?> that)) return false;
      final V value = ForwardingSyncMap.this.get(that.getKey());
      return value != null && Objects.equals(value, that.getValue());
    }

    @Override
    public boolean remove(final @Nullable Object entry) {
      if(!(entry instanceof final Map.Entry<?,?> that)) return false;
      return ForwardingSyncMap.this.remove(that.getKey(), that.getValue());
    }

    @Override
    public void clear() {
      ForwardingSyncMap.this.clear();
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
      ForwardingSyncMap.this.promote();
      return new EntryIterator(ForwardingSyncMap.this.immutable.entrySet().iterator());
    }
  }

  /**
   * Represents an entry {@link Iterator} that traverses the entries in the
   * given map.
   */
  private final class EntryIterator implements Iterator<Map.Entry<K, V>> {
    private final Iterator<Map.Entry<K, ObjectReference>> backingIterator;
    private Map.@Nullable Entry<K, V> next;
    private Map.@Nullable Entry<K, V> current;

    /* package */ EntryIterator(final Iterator<Map.Entry<K, ObjectReference>> backingIterator) {
      this.backingIterator = backingIterator;
      this.advance();
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
      this.advance();
      return current;
    }

    @Override
    public void remove() {
      final Map.Entry<K, V> current;
      if((current = this.current) == null) throw new IllegalStateException();
      this.current = null;
      ForwardingSyncMap.this.remove(current.getKey(), current.getValue());
    }

    private void advance() {
      this.next = null;
      while(this.backingIterator.hasNext()) {
        final Map.Entry<K, ObjectReference> entry;
        final V value;

        if((value = (entry = this.backingIterator.next()).getValue().value()) != null) {
          this.next = new MapEntry(entry.getKey(), value);
          return;
        }
      }
    }
  }
}
