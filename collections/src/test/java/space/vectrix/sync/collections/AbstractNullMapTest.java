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

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class AbstractNullMapTest<K, V> extends AbstractMapTest<K, V> {
  // get

  @Test
  public void test_get_nullKey() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.get(null), "Map should throw exception when given a null key.");
  }

  // getOrDefault

  @Test
  public void test_getOrDefault_nullKey() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.getOrDefault(this.key(3), null), "Map should throw exception when given a null default value.");
    assertThrows(NullPointerException.class, () -> map.getOrDefault(null, this.value(3)), "Map should throw exception when given a null key.");
  }

  // computeIfAbsent

  @Test
  public void test_computeIfAbsent_nullKey() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.computeIfAbsent(null, key -> this.value(0)), "Map should throw exception when given a null key.");
  }

  // computeIfPresent

  @Test
  public void test_computeIfPresent_nullKey() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.computeIfPresent(null, (key, previousValue) -> this.value(0)), "Map should throw exception when given a null key.");
  }

  // compute

  @Test
  public void test_compute_nullKey() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.compute(null, (key, previousValue) -> this.value(0)), "Map should throw exception when given a null key.");
  }

  // putIfAbsent

  @Test
  public void test_putIfAbsent_nullKeyValue() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.putIfAbsent(null, this.value(3)), "Map should throw exception when given a null key.");
    assertThrows(NullPointerException.class, () -> map.putIfAbsent(this.key(3), null), "Map should throw exception when given a null value.");
  }

  // put

  @Test
  public void test_put_nullKeyValue() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.put(null, this.value(3)), "Map should throw exception when given a null key.");
    assertThrows(NullPointerException.class, () -> map.put(this.key(3), null), "Map should throw exception when given a null value.");
  }

  // removeKey

  @Test
  public void test_removeKey_nullKey() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.remove(null), "Map should throw exception when given a null key.");
  }

  // removeKeyValue

  @Test
  public void test_removeKeyValue_nullKeyValue() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.remove(null, this.value(3)), "Map should throw exception when given a null key.");
    assertThrows(NullPointerException.class, () -> map.remove(this.key(3), null), "Map should throw exception when given a null value.");
  }

  // replaceKey

  @Test
  public void test_replaceKey_nullKeyValue() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.remove(null, this.value(3)), "Map should throw exception when given a null key.");
    assertThrows(NullPointerException.class, () -> map.remove(this.key(3), null), "Map should throw exception when given a null value.");
  }

  // replaceKeyValue

  @Test
  public void test_replaceKeyValue_nullKeyValue() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.replace(null, this.value(3), this.value(5)), "Map should throw exception when given a null key.");
    assertThrows(NullPointerException.class, () -> map.replace(this.key(3), null, null), "Map should throw exception when given a null value.");
  }
}
