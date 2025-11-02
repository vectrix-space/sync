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

import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.ApiStatus;

/**
 * Provides common constants and utilities for hashing operations used across
 * the collections framework.
 *
 * @author Vectrix
 * @since 1.0.0
 */
@ApiStatus.NonExtendable
public interface Hashing {
  /**
   * The bitmask applied to ensure hash values are non-negative and fit within
   * the usable range of 32-bit signed integers.
   */
  /* package */ int HASH_BITS = 0x7FFFFFFF;

  /**
   * A random seed used to introduce variability into hash computations,
   * reducing predictability and the likelihood of collision attacks.
   */
  /* package */ int HASH_SEED = ThreadLocalRandom.current().nextInt();

  /**
   * A hash mixing function optimized for raw speed with minimal transformation.
   * Provides adequate distribution for small or moderate-sized maps but may
   * degrade performance with large datasets or many similar keys.
   */
  MixFunction FASTEST_MIX = x -> (x ^ (x >>> 16)) & Hashing.HASH_BITS;

  /**
   * A hash mixing function that balances performance and moderate hash
   * distribution. Suitable for medium-sized maps, but may still degrade under
   * high key similarity or very large data sets.
   */
  MixFunction FAST_MIX = x -> {
    x ^= (x >>> 16);
    x ^= (x >>> 13);
    return x & Hashing.HASH_BITS;
  };

  /**
   * A hash mixing function that emphasizes stronger distribution while
   * maintaining good performance. Incorporates {@link #HASH_SEED} to randomize
   * results and reduce vulnerability to hash collision attacks.
   */
  MixFunction BALANCED_MIX = x -> {
    x ^= Hashing.HASH_SEED;
    x ^= (x >>> 16);
    x ^= (x >>> 13);
    return x & Hashing.HASH_BITS;
  };

  /**
   * Represents a hash mixing function to distribute key-value pairs into
   * buckets using an {@code int} hash.
   *
   * @author Vectrix
   * @since 1.0.0
   */
  @FunctionalInterface
  interface MixFunction {
    /**
     * Mixes the provided hash for distributing key-value pairs into buckets.
     *
     * @param hash the hash
     * @return the mixed hash
     * @since 1.0.0
     */
    int mix(final int hash);
  }
}
