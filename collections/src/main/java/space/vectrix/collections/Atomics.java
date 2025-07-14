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

import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides methods to deal with atomic updates to fields.
 *
 * @since 1.0.0
 */
@SuppressWarnings("SameParameterValue")
@ApiStatus.Experimental
/* package */ final class Atomics {
  /**
   * Represents a predicate to check whether a value is empty.
   */
  /* package */ static Predicate<@Nullable Object> IS_EMPTY = Objects::isNull;

  /**
   * Represents a predicate to check whether a value is expunged.
   */
  /* package */ static Predicate<@Nullable Object> IS_EXPUNGED = that -> that == Sentinel.EXPUNGED;

  /**
   * Represents a predicate to check whether a value is present.
   */
  /* package */ static Predicate<@Nullable Object> IS_PRESENT = that -> that != null && !(that instanceof Sentinel);

  /**
   * Represents a predicate to check whether a value is empty or expunged.
   */
  /* package */ static Predicate<@Nullable Object> IS_EMPTY_OR_EXPUNGED = Atomics.IS_EMPTY.or(Atomics.IS_EXPUNGED);

  /**
   * Represents a predicate to check whether a value is empty or present.
   */
  /* package */ static Predicate<@Nullable Object> IS_EMPTY_OR_PRESENT = Atomics.IS_EMPTY.or(Atomics.IS_PRESENT);

  /**
   * Returns the current value avoiding load and store reordering.
   *
   * @param handle the variable reference
   * @param holder the variable holder
   * @return the current value
   */
  /* package */ static @Nullable Object get(final @NotNull VarHandle handle,
                                            final @NotNull Object holder) {
    return handle.getAcquire(holder);
  }

  /**
   * Atomically replaces the existing value with the new value, setting the
   * previous value.
   *
   * @param entry the value entry
   * @param handle the variable reference
   * @param holder the variable holder
   * @param value the new value
   * @param <T> the value type
   */
  /* package */ static <T> void replace(final @NotNull ValueEntry entry,
                                        final @NotNull VarHandle handle,
                                        final @NotNull Object holder,
                                        final @Nullable T value) {
    Object current;
    for(; ; ) {
      current = handle.getAcquire(holder);
      if(handle.compareAndSet(holder, current, value)) {
        entry.set(current, value);
        return;
      }

      Thread.onSpinWait();
    }
  }

  /**
   * Atomically replaces the existing value with the new value from the mapper,
   * setting the previous and next value.
   *
   * @param entry the value entry
   * @param handle the variable reference
   * @param holder the variable holder
   * @param mapper the value mapper
   * @param <T> the value type
   */
  /* package */ static <T> void computeAndReplace(final @NotNull ValueEntry entry,
                                                  final @NotNull VarHandle handle,
                                                  final @NotNull Object holder,
                                                  final @NotNull Function<@Nullable Object, @Nullable T> mapper) {
    Object current; T value;
    for(; ; ) {
      current = handle.getAcquire(holder);
      if(handle.compareAndSet(holder, current, (value = mapper.apply(current)))) {
        entry.set(current, value);
        return;
      }

      Thread.onSpinWait();
    }
  }

  /**
   * Atomically replaces the existing value with the new value, if the given
   * comparison {@link Predicate} allows the current value, setting the
   * previous value, and next value or {@link Sentinel#EMPTY}. Returns
   * {@code true} if updated, otherwise {@code false}.
   *
   * @param entry the value entry
   * @param handle the variable reference
   * @param holder the variable holder
   * @param compare the predicate to match the current value against
   * @param value the new value
   * @param <T> the value type
   * @return whether the value was updated
   */
  /* package */ static <T> boolean replace(final @NotNull ValueEntry entry,
                                           final @NotNull VarHandle handle,
                                           final @NotNull Object holder,
                                           final @NotNull Predicate<@Nullable Object> compare,
                                           final @Nullable T value) {
    Object current;
    for(; ; ) {
      current = handle.getAcquire(holder);
      if(!compare.test(current)) {
        entry.set(current, Sentinel.EMPTY);
        return false;
      } else if(handle.compareAndSet(holder, current, value)) {
        entry.set(current, value);
        return true;
      }

      Thread.onSpinWait();
    }
  }

  /**
   * Atomically replaces the existing value with the new value from the mapper,
   * if the given comparison {@link Predicate} allows the current value,
   * setting the previous, and next value or {@link Sentinel#EMPTY}. Returns
   * {@code true} if updated, otherwise {@code false}.
   *
   * @param entry the value entry
   * @param handle the variable reference
   * @param holder the variable holder
   * @param compare the predicate to match the current value against
   * @param mapper the value mapper
   * @param <T> the value type
   * @return whether the value was updated
   */
  /* package */ static <T> boolean computeAndReplace(final @NotNull ValueEntry entry,
                                                     final @NotNull VarHandle handle,
                                                     final @NotNull Object holder,
                                                     final @NotNull Predicate<@Nullable Object> compare,
                                                     final @NotNull Function<@Nullable Object, @Nullable T> mapper) {
    Object current; T value;
    for(; ; ) {
      current = handle.getAcquire(holder);
      if(!compare.test(current)) {
        entry.set(current, Sentinel.EMPTY);
        return false;
      } else if(handle.compareAndSet(holder, current, (value = mapper.apply(current)))) {
        entry.set(current, value);
        return true;
      }

      Thread.onSpinWait();
    }
  }

  /**
   * Atomically replaces the existing value with the new value, if the given
   * comparison value is the same as the current value, returning {@code true}
   * if successful, otherwise {@code false}.
   *
   * @param handle the variable reference
   * @param holder the variable holder
   * @param compare the comparison object
   * @param value the new value
   * @param <T> the value type
   * @return true if successful, otherwise false
   */
  /* package */ static <T> boolean replace(final @NotNull VarHandle handle,
                                           final @NotNull Object holder,
                                           final @Nullable Object compare,
                                           final @Nullable T value) {
    Object current;
    for(; ; ) {
      if(!Objects.equals(current = Atomics.get(handle, holder), compare)) return false;
      if(handle.compareAndSet(holder, current, value)) return true;
      Thread.onSpinWait();
    }
  }

  private Atomics() {
  }

  /**
   * Represents a snapshot of the previous and next value from a computed
   * atomic update.
   */
  /* package */ static final class ValueEntry {
    /* package */ @Nullable Object previous = Sentinel.EMPTY;
    /* package */ @Nullable Object next = Sentinel.EMPTY;

    /* package */ ValueEntry() {
    }

    /* package */ void set(final @Nullable Object previous, final @Nullable Object next) {
      this.previous = previous;
      this.next = next;
    }
  }
}
