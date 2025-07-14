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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides methods to deal with atomic updates to fields.
 *
 * @since 1.0.0
 */
@SuppressWarnings({"PointlessBitwiseExpression", "SameParameterValue"})
@ApiStatus.Experimental
/* package */ final class Atomics {
  /**
   * Represents a flag that selects a {@code null} value.
   */
  /* package */ static final int EMPTY_FLAG = 1 << 0;

  /**
   * Represents a flag that selects a present value, that is not a sentinel.
   */
  /* package */ static final int PRESENT_FLAG = 1 << 1;

  /**
   * Represents a flag that selects an expunged value.
   */
  /* package */ static final int EXPUNGED_FLAG = 1 << 2;

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
   * Atomically replaces the existing value with the new value, returning the
   * previous value.
   *
   * @param entry the value entry
   * @param handle the variable reference
   * @param holder the variable holder
   * @param value the new value
   * @param <T> the value type
   * @return the previous value
   */
  /* package */ static <T> @NotNull ValueEntry replace(final @NotNull ValueEntry entry,
                                                       final @NotNull VarHandle handle,
                                                       final @NotNull Object holder,
                                                       final @Nullable T value) {
    Object current = Atomics.get(handle, holder), next;
    for(; ; ) {
      if((next = handle.compareAndExchange(holder, current, value)) == current) return entry.set(current, value);
      current = next;
      Thread.onSpinWait();
    }
  }

  /**
   * Atomically replaces the existing value with the new value from the mapper,
   * returning the previous and next value.
   *
   * @param entry the value entry
   * @param handle the variable reference
   * @param holder the variable holder
   * @param mapper the value mapper
   * @param <T> the value type
   * @return the previous and next value
   */
  /* package */ static <T> @NotNull ValueEntry computeAndReplace(final @NotNull ValueEntry entry,
                                                                 final @NotNull VarHandle handle,
                                                                 final @NotNull Object holder,
                                                                 final @NotNull Function<@Nullable Object, @Nullable T> mapper) {
    Object current = Atomics.get(handle, holder), next; T value;
    for(; ; ) {
      if((next = handle.compareAndExchange(holder, current, (value = mapper.apply(current)))) == current) return entry.set(current, value);
      current = next;
      Thread.onSpinWait();
    }
  }

  /**
   * Atomically replaces the existing value with the new value, if the given
   * flag allows the current value, returning the previous value, and next
   * value or {@link Sentinel#EMPTY}.
   *
   * @param entry the value entry
   * @param handle the variable reference
   * @param holder the variable holder
   * @param flag the flag to match the current value against
   * @param value the new value
   * @param <T> the value type
   * @return the previous value and next value
   */
  /* package */ static <T> @NotNull ValueEntry replace(final @NotNull ValueEntry entry,
                                                       final @NotNull VarHandle handle,
                                                       final @NotNull Object holder,
                                                       final int flag,
                                                       final @Nullable T value) {
    Object current = Atomics.get(handle, holder), next;
    for(; ; ) {
      if(Atomics.checkFlags(current, flag)) return entry.set(current, Sentinel.EMPTY);
      if((next = handle.compareAndExchange(holder, current, value)) == current) return entry.set(current, value);
      current = next;
      Thread.onSpinWait();
    }
  }

  /**
   * Atomically replaces the existing value with the new value from the mapper,
   * returning the previous, and next value or {@link Sentinel#EMPTY}.
   *
   * @param entry the value entry
   * @param handle the variable reference
   * @param holder the variable holder
   * @param flag the flag to match the current value against
   * @param mapper the value mapper
   * @param <T> the value type
   * @return the previous and next value
   */
  /* package */ static <T> @NotNull ValueEntry computeAndReplace(final @NotNull ValueEntry entry,
                                                                 final @NotNull VarHandle handle,
                                                                 final @NotNull Object holder,
                                                                 final int flag,
                                                                 final @NotNull Function<@Nullable Object, @Nullable T> mapper) {
    Object current = Atomics.get(handle, holder), next; T value;
    for(; ; ) {
      if(Atomics.checkFlags(current, flag)) return entry.set(current, Sentinel.EMPTY);
      if((next = handle.compareAndExchange(holder, current, (value = mapper.apply(current)))) == current) return entry.set(current, value);
      current = next;
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

  private static boolean checkFlags(final @Nullable Object previous, final int compare) {
    final int mask = previous == null ? Atomics.EMPTY_FLAG : (previous == Sentinel.EXPUNGED ? Atomics.EXPUNGED_FLAG : Atomics.PRESENT_FLAG);
    return (compare & mask) == 0;
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

    /* package */ ValueEntry set(final @Nullable Object previous, final @Nullable Object next) {
      this.previous = previous;
      this.next = next;
      return this;
    }

    /* package */ boolean commited() {
      return this.next != Sentinel.EMPTY;
    }
  }
}
