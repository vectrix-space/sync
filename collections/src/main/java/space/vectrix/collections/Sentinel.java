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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Provides common sentinels for the collections.
 *
 * @since 1.0.0
 */
@ApiStatus.Experimental
/* package */ enum Sentinel {
  /**
   * Represents a sentinel for an empty or unset value.
   */
  EMPTY,

  /**
   * Represents a sentinel for an expunged value.
   */
  EXPUNGED;

  /**
   * Unboxes the given object from a sentinel or returns the original value.
   *
   * @param value the value
   * @param <V> the expected value type
   * @return the original value or null
   */
  @SuppressWarnings("unchecked")
  /* package */ static <V> @Nullable V unbox(final @Nullable Object value) {
    if(value == null || value instanceof Sentinel) return null;
    return (V) value;
  }
}
