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

import java.util.HashMap;
import java.util.Map;

/**
 * Provides {@link ForwardingSyncMap} specific test data for {@link AbstractMapTest}.
 *
 * @since 1.0.0
 */
public class ForwardingSyncMapTest extends AbstractMapTest<String, String> {
  @Override
  protected Map<String, String> createMap() {
    return new ForwardingSyncMap<>(HashMap::new);
  }

  @Override
  protected Map<String, String> populate(final Map<String, String> map, final int elements) {
    for(int i = 0; i < elements; i++) {
      map.put(this.key(i), this.value(i));
    }

    return map;
  }

  @Override
  protected String key(final int index) {
    return String.valueOf(index);
  }

  @Override
  protected String value(final int index) {
    return String.valueOf(index);
  }
}
