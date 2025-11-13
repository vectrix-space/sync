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
package space.vectrix.sync.collections.fastutil;

/**
 * Provides common constants for hashing operations used across
 * the collections.
 *
 * @author vectrix
 * @since 1.0.0
 */
public interface Hash {
  /**
   * Represents the default initial capacity of the tables.
   *
   * @since 1.0.0
   */
  int DEFAULT_CAPACITY = 16;

  /**
   * Represents the default load factor for resizing this map.
   *
   * @since 1.0.0
   */
  float DEFAULT_LOAD_FACTOR = 0.75F;

  /**
   * Represents a hash spread function to distribute key-value pairs into
   * buckets using a hash.
   *
   * @author vectrix
   * @since 1.0.0
   */
  @FunctionalInterface
  @SuppressWarnings("UseHashCodeMethodInspection")
  interface SpreadFunction {
    /**
     * Spreads the provided hash for distributing key-value pairs into buckets,
     * using the given {@code int} hash.
     *
     * @param hash the hash
     * @return the spread hash
     * @since 1.0.0
     */
    int spread(final int hash);

    /**
     * Spreads the provided hash for distributing key-value pairs into buckets,
     * using the given {@code short} hash.
     *
     * @param hash the hash
     * @return the spread hash
     * @since 1.0.0
     */
    default int spread(final short hash) {
      return this.spread((int) hash);
    }

    /**
     * Spreads the provided hash for distributing key-value pairs into buckets,
     * using the given {@code long} hash.
     *
     * @param hash the hash
     * @return the spread hash
     * @since 1.0.0
     */
    default int spread(final long hash) {
      return this.spread((int) (hash ^ (hash >>> 32)));
    }

    /**
     * Spreads the provided hash for distributing key-value pairs into buckets,
     * using the given {@code float} hash.
     *
     * @param hash the hash
     * @return the spread hash
     * @since 1.0.0
     */
    default int spread(final float hash) {
      return this.spread(Float.floatToRawIntBits(hash));
    }

    /**
     * Spreads the provided hash for distributing key-value pairs into buckets,
     * using the given {@code double} hash.
     *
     * @param hash the hash
     * @return the spread hash
     * @since 1.0.0
     */
    default int spread(final double hash) {
      final long value = Double.doubleToRawLongBits(hash);
      return this.spread((int) (value ^ (value >>> 32)));
    }
  }
}
