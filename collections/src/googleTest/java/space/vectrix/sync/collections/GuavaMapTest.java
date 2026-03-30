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

import com.google.common.collect.testing.ConcurrentMapTestSuiteBuilder;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Supplier;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class GuavaMapTest extends TestCase {
  public static Test suite() {
    final TestSuite suite = new TestSuite();
    suite.addTest(concurrentMapTest(
      "BucketSyncMap",
      generator(BucketSyncMap::new)
    ));

    return suite;
  }

  private static Test concurrentMapTest(final String name,
                                        final TestMapGenerator<?, ?> generator,
                                        final Method... suppressed) {
    return ConcurrentMapTestSuiteBuilder
      .using(generator)
      .named(name)
      .withFeatures(
        CollectionSize.ANY,
        MapFeature.GENERAL_PURPOSE,
        CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
        CollectionFeature.NON_STANDARD_TOSTRING
      )
      .suppressing(suppressed)
      .createTestSuite();
  }

  private static TestStringMapGenerator generator(final Supplier<Map<String, String>> supplier) {
    return new TestStringMapGenerator() {
      @Override
      protected Map<String, String> create(final Map.Entry<String, String>[] entries) {
        final Map<String, String> map = supplier.get();
        for(final Map.Entry<String, String> entry : entries) {
          map.put(entry.getKey(), entry.getValue());
        }

        return map;
      }
    };
  }
}
