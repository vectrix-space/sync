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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Provides common tests for {@link Map} implementations.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @since 1.0.0
 */
public abstract class AbstractMapTest<K, V> {
  /**
   * Creates a new {@link Map} to use in a test.
   *
   * @return the map
   * @since 1.0.0
   */
  protected abstract @NotNull Map<K, V> createMap();

  /**
   * Populates the given {@link Map} with the given amount {@code int}
   * elements.
   *
   * @param map the map
   * @param elements the element count
   * @return the map
   * @since 1.0.0
   */
  protected abstract @NotNull Map<K, V> populate(final @NotNull Map<K, V> map, final int elements);

  /**
   * Returns the {@link K} key at the specified {@code index}.
   *
   * @param index the index
   * @return the key
   * @since 1.0.0
   */
  protected abstract @NotNull K key(final int index);

  /**
   * Returns the {@link V} value at the specified {@code index}.
   *
   * @param index the index
   * @return the value
   * @since 1.0.0
   */
  protected abstract @NotNull V value(final int index);

  // size

  @Test
  public void test_size_empty() {
    final Map<K, V> map = this.createMap();
    assertEquals(0, map.size(), "Map should be of size 0.");
  }

  @Test
  public void test_size_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertEquals(5, map.size(), "Map should be of size 5.");
  }

  // isEmpty

  @Test
  public void test_isEmpty_true() {
    final Map<K, V> map = this.createMap();
    assertTrue(map.isEmpty(), "Map should be empty.");
  }

  @Test
  public void test_isEmpty_false() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertFalse(map.isEmpty(), "Map should not be empty.");
  }

  // containsKey

  @Test
  public void test_containsKey_empty() {
    final Map<K, V> map = this.createMap();
    assertFalse(map.containsKey(this.key(5)), "Map should return false for contains key.");
  }

  @Test
  public void test_containsKey_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertTrue(map.containsKey(this.key(3)), "Map should return true for contains key.");
    assertFalse(map.containsKey(this.key(5)), "Map should return false for contains key.");
  }

  // get

  @Test
  public void test_get_empty() {
    final Map<K, V> map = this.createMap();
    assertNull(map.get(this.key(3)), "Map should return null.");
  }

  @Test
  public void test_get_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertEquals(this.value(3), map.get(this.key(3)), "Map should return the value 3 for key 3.");
    assertEquals(this.value(4), map.get(this.key(4)), "Map should return the value 4 for key 4.");
    assertNull(map.get(this.key(10)), "Map should return the value null for key 10.");
  }

  @Test
  public void test_get_nullKey() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.get(null), "Map should throw exception when given a null key.");
  }

  // getOrDefault

  @Test
  public void test_getOrDefault_empty() {
    final Map<K, V> map = this.createMap();
    assertEquals(this.value(5), map.getOrDefault(this.key(3), this.value(5)), "Map should return the value 5 for key 3.");
  }

  @Test
  public void test_getOrDefault_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertEquals(this.value(3), map.getOrDefault(this.key(3), this.value(5)), "Map should return the value 3 for key 3.");
    assertEquals(this.value(4), map.getOrDefault(this.key(4), this.value(5)), "Map should return the value 3 for key 3.");
  }

  @Test
  public void test_getOrDefault_nullKey() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.getOrDefault(this.key(3), null), "Map should throw exception when given a null default value.");
    assertThrows(NullPointerException.class, () -> map.getOrDefault(null, this.value(3)), "Map should throw exception when given a null key.");
  }

  // computeIfAbsent

  @Test
  public void test_computeIfAbsent_empty() {
    final Map<K, V> map = this.createMap();
    assertEquals(this.value(3), map.computeIfAbsent(this.key(3), key -> this.value(3)), "Map should return the value 3 for key 3.");
    assertEquals(this.value(3), map.get(this.key(3)), "Map should return the value 3 for key 3.");
    assertEquals(1, map.size(), "Map should be of size 1.");
  }

  @Test
  public void test_computeIfAbsent_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertEquals(this.value(3), map.computeIfAbsent(this.key(3), key -> this.value(5)), "Map should return the value 3 for key 3.");
    assertEquals(this.value(3), map.get(this.key(3)), "Map should return the value 3 for key 3.");
    assertEquals(5, map.size(), "Map should be of size 5.");
  }

  @Test
  public void test_computeIfAbsent_nullKey() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.computeIfAbsent(null, key -> this.value(0)), "Map should throw exception when given a null key.");
  }

  @Test
  public void test_computeIfAbsent_fullNullValue() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertEquals(this.value(3), map.computeIfAbsent(this.key(3), key -> null), "Map should return the value 3 for key 3.");
    assertEquals(this.value(3), map.get(this.key(3)), "Map should return the value 3 for key 3.");
    assertEquals(5, map.size(), "Map should be of size 5.");
  }

  @Test
  public void test_computeIfAbsent_emptyNullValue() {
    final Map<K, V> map = this.createMap();
    assertNull(map.computeIfAbsent(this.key(3), key -> null), "Map should return null for key 3.");
    assertNull(map.get(this.key(3)), "Map should return null for key 3.");
    assertEquals(0, map.size(), "Map should be of size 0.");
  }

  // computeIfPresent

  @Test
  public void test_computeIfPresent_empty() {
    final Map<K, V> map = this.createMap();
    assertNull(map.computeIfPresent(this.key(3), (key, previousValue) -> this.value(3)), "Map should return null for key 3.");
    assertNull(map.get(this.key(3)), "Map should return null for key 3.");
    assertEquals(0, map.size(), "Map should be of size 0.");
  }

  @Test
  public void test_computeIfPresent_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertEquals(this.value(5), map.computeIfPresent(this.key(3), (key, previousValue) -> this.value(5)), "Map should return the value 5 for key 3.");
    assertEquals(this.value(5), map.get(this.key(3)), "Map should return the value 5 for key 3.");
    assertEquals(5, map.size(), "Map should be of size 5.");
  }

  @Test
  public void test_computeIfPresent_nullKey() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.computeIfPresent(null, (key, previousValue) -> this.value(0)), "Map should throw exception when given a null key.");
  }

  @Test
  public void test_computeIfPresent_fullNullValue() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertNull(map.computeIfPresent(this.key(3), (key, previousValue) -> null), "Map should return null for key 3.");
    assertNull(map.get(this.key(3)), "Map should return null for key 3.");
    assertEquals(4, map.size(), "Map should be of size 4.");
  }

  @Test
  public void test_computeIfPresent_emptyNullValue() {
    final Map<K, V> map = this.createMap();
    assertNull(map.computeIfPresent(this.key(3), (key, previousValue) -> null), "Map should return null for key 3.");
    assertNull(map.get(this.key(3)), "Map should return null for key 3.");
    assertEquals(0, map.size(), "Map should be of size 0.");
  }

  // compute

  @Test
  public void test_compute_empty() {
    final Map<K, V> map = this.createMap();
    assertEquals(this.value(3), map.compute(this.key(3), (key, previousValue) -> this.value(3)), "Map should return the value 3 for key 3.");
    assertEquals(this.value(3), map.get(this.key(3)), "Map should return the value 3 for key 3.");
    assertEquals(1, map.size(), "Map should be of size 1.");
  }

  @Test
  public void test_compute_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertEquals(this.value(5), map.compute(this.key(3), (key, previousValue) -> this.value(5)), "Map should return the value 5 for key 3.");
    assertEquals(this.value(5), map.get(this.key(3)), "Map should return the value 5 for key 3.");
    assertEquals(5, map.size(), "Map should be of size 5.");
  }

  @Test
  public void test_compute_nullKey() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.compute(null, (key, previousValue) -> this.value(0)), "Map should throw exception when given a null key.");
  }

  @Test
  public void test_compute_fullNullValue() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertNull(map.compute(this.key(3), (key, previousValue) -> null), "Map should return null for key 3.");
    assertNull(map.get(this.key(3)), "Map should return null for key 3.");
    assertEquals(4, map.size(), "Map should be of size 4.");
  }

  @Test
  public void test_compute_emptyNullValue() {
    final Map<K, V> map = this.createMap();
    assertNull(map.compute(this.key(3), (key, previousValue) -> null), "Map should return null for key 3.");
    assertNull(map.get(this.key(3)), "Map should return null for key 3.");
    assertEquals(0, map.size(), "Map should be of size 0.");
  }

  // putIfAbsent

  @Test
  public void test_putIfAbsent_empty() {
    final Map<K, V> map = this.createMap();
    assertNull(map.putIfAbsent(this.key(3), this.value(3)), "Map should return null for storing key 3 and value 3.");
    assertEquals(this.value(3), map.get(this.key(3)), "Map should return value 3 for key 3.");
    assertEquals(1, map.size(), "Map should be of size 1.");
  }

  @Test
  public void test_putIfAbsent_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertEquals(this.value(3), map.putIfAbsent(this.key(3), this.value(5)), "Map should return value 3 for key 3.");
    assertEquals(this.value(3), map.get(this.key(3)), "Map should return value 3 for key 3.");
    assertEquals(5, map.size(), "Map should be of size 5.");
  }

  @Test
  public void test_putIfAbsent_nullKeyValue() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.putIfAbsent(null, this.value(3)), "Map should throw exception when given a null key.");
    assertThrows(NullPointerException.class, () -> map.putIfAbsent(this.key(3), null), "Map should throw exception when given a null value.");
  }

  // put

  @Test
  public void test_put_empty() {
    final Map<K, V> map = this.createMap();
    assertNull(map.put(this.key(3), this.value(3)), "Map should return null for storing key 3 and value 3.");
    assertEquals(this.value(3), map.get(this.key(3)), "Map should return value 3 for key 3.");
    assertEquals(1, map.size(), "Map should be of size 1.");
  }

  @Test
  public void test_put_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertEquals(this.value(3), map.put(this.key(3), this.value(5)), "Map should return value 3 for key 3.");
    assertEquals(this.value(5), map.get(this.key(3)), "Map should return value 5 for key 3.");
    assertEquals(5, map.size(), "Map should be of size 5.");
  }

  @Test
  public void test_put_nullKeyValue() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.put(null, this.value(3)), "Map should throw exception when given a null key.");
    assertThrows(NullPointerException.class, () -> map.put(this.key(3), null), "Map should throw exception when given a null value.");
  }

  // removeKey

  @Test
  public void test_removeKey_empty() {
    final Map<K, V> map = this.createMap();
    assertNull(map.remove(this.key(3)), "Map should return null for deleting key 3.");
    assertNull(map.get(this.key(3)), "Map should return null for key 3.");
    assertEquals(0, map.size(), "Map should be of size 0.");
  }

  @Test
  public void test_removeKey_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertEquals(this.value(3), map.remove(this.key(3)), "Map should return value 3 for key 3.");
    assertNull(map.get(this.key(3)), "Map should return null for key 3.");
    assertEquals(4, map.size(), "Map should be of size 4.");
  }

  @Test
  public void test_removeKey_nullKey() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.remove(null), "Map should throw exception when given a null key.");
  }

  // removeKeyValue

  @Test
  public void test_removeKeyValue_empty() {
    final Map<K, V> map = this.createMap();
    assertFalse(map.remove(this.key(3), this.value(3)), "Map should return false for deleting key 3 and value 3.");
    assertNull(map.get(this.key(3)), "Map should return null for key 3.");
    assertEquals(0, map.size(), "Map should be of size 0.");
  }

  @Test
  public void test_removeKeyValue_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertFalse(map.remove(this.key(3), this.value(5)), "Map should return false for key 3 and value 5.");
    assertEquals(this.value(3), map.get(this.key(3)), "Map should return value 3 for key 3.");
    assertEquals(5, map.size(), "Map should be of size 5.");

    assertTrue(map.remove(this.key(3), this.value(3)), "Map should return true for key 3 and value 5.");
    assertNull(map.get(this.key(3)), "Map should return null for key 3.");
    assertEquals(4, map.size(), "Map should be of size 4.");
  }

  @Test
  public void test_removeKeyValue_nullKeyValue() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.remove(null, this.value(3)), "Map should throw exception when given a null key.");
    assertThrows(NullPointerException.class, () -> map.remove(this.key(3), null), "Map should throw exception when given a null value.");
  }

  // replaceKey

  @Test
  public void test_replaceKey_empty() {
    final Map<K, V> map = this.createMap();
    assertNull(map.replace(this.key(3), this.value(3)), "Map should return null for key 3 and value 3.");
    assertNull(map.get(this.key(3)), "Map should return null for key 3.");
    assertEquals(0, map.size(), "Map should be of size 0.");
  }

  @Test
  public void test_replaceKey_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertEquals(this.value(3), map.replace(this.key(3), this.value(5)), "Map should return value 3 for key 3 and value 5.");
    assertEquals(this.value(5), map.get(this.key(3)), "Map should return value 5 for key 3.");
    assertEquals(5, map.size(), "Map should be of size 5.");
  }

  @Test
  public void test_replaceKey_nullKeyValue() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.remove(null, this.value(3)), "Map should throw exception when given a null key.");
    assertThrows(NullPointerException.class, () -> map.remove(this.key(3), null), "Map should throw exception when given a null value.");
  }

  // replaceKeyValue

  @Test
  public void test_replaceKeyValue_empty() {
    final Map<K, V> map = this.createMap();
    assertFalse(map.replace(this.key(3), this.value(3), this.value(5)), "Map should return false for deleting key 3, value 3 and value 5.");
    assertNull(map.get(this.key(3)), "Map should return null for key 3.");
    assertEquals(0, map.size(), "Map should be of size 0.");
  }

  @Test
  public void test_replaceKeyValue_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    assertFalse(map.replace(this.key(3), this.value(5), this.value(8)), "Map should return false for key 3, value 5 and value 8.");
    assertEquals(this.value(3), map.get(this.key(3)), "Map should return value 3 for key 3.");
    assertEquals(5, map.size(), "Map should be of size 5.");

    assertTrue(map.replace(this.key(3), this.value(3), this.value(5)), "Map should return true for key 3, value 3 and value 5.");
    assertEquals(this.value(5), map.get(this.key(3)), "Map should return value 5 for key 3.");
    assertEquals(5, map.size(), "Map should be of size 5.");
  }

  @Test
  public void test_replaceKeyValue_nullKeyValue() {
    final Map<K, V> map = this.createMap();
    assertThrows(NullPointerException.class, () -> map.replace(null, this.value(3), this.value(5)), "Map should throw exception when given a null key.");
    assertThrows(NullPointerException.class, () -> map.replace(this.key(3), null, null), "Map should throw exception when given a null value.");
  }

  // forEach

  @Test
  public void test_forEach_empty() {
    final Map<K, V> map = this.createMap();
    final AtomicInteger count = new AtomicInteger();

    map.forEach((key, value) -> count.incrementAndGet());
    assertEquals(0, count.get(), "Map should not invoke consumer for empty map.");
  }

  @Test
  public void test_forEach_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    final boolean[] visited = new boolean[5];

    map.forEach((key, value) -> {
      for(int i = 0; i < 5; i++) {
        if(this.key(i).equals(key) && this.value(i).equals(value)) {
          visited[i] = true;
          break;
        }
      }
    });

    for(int i = 0; i < 5; i++) {
      assertTrue(visited[i], "Map should have visited key " + i);
    }
  }

  // clear

  @Test
  public void test_clear_empty() {
    final Map<K, V> map = this.createMap();
    map.clear();
    assertEquals(0, map.size(), "Map should be empty after clear.");
  }

  @Test
  public void test_clear_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    map.clear();
    assertEquals(0, map.size(), "Map should be empty after clear.");

    for(int i = 0; i < 5; i++) {
      assertNull(map.get(this.key(i)), "Map should return null for key " + i + " after clear.");
    }
  }

  // entrySet

  @Test
  public void test_entrySet_empty() {
    final Map<K, V> map = this.createMap();
    final Set<Map.Entry<K, V>> entrySet = map.entrySet();

    assertFalse(entrySet.iterator().hasNext(), "Map entrySet iterator should have no elements.");
  }

  @Test
  public void test_entrySet_full() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    final Set<Map.Entry<K, V>> entrySet = map.entrySet();

    assertEquals(5, entrySet.size(), "Map entrySet should be of size 5.");

    final boolean[] found = new boolean[5];
    for(final Map.Entry<K, V> entry : entrySet) {
      for(int i = 0; i < 5; i++) {
        if(this.key(i).equals(entry.getKey()) && this.value(i).equals(entry.getValue())) {
          found[i] = true;
          break;
        }
      }
    }

    for(int i = 0; i < 5; i++) {
      assertTrue(found[i], "Map entrySet should contain key-value pair " + i + ".");
    }
  }

  @Test
  public void test_entrySet_backingMapUpdate() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    final Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();

    map.remove(this.key(1));

    final AtomicInteger count = new AtomicInteger();
    while(iterator.hasNext()) {
      iterator.next();
      count.incrementAndGet();
    }

    assertEquals(4, count.get(), "Map entrySet iterator should reflect snapshot at creation time.");
  }

  @Test
  public void test_entrySet_snapshotAndRemove() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    final Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();

    while(iterator.hasNext()) {
      final Map.Entry<K, V> entry = iterator.next();
      if(this.key(3).equals(entry.getKey())) {
        iterator.remove();
        break;
      }
    }

    assertEquals(4, map.size(), "Map should be of size 4 after removing key 3.");
    assertNull(map.get(this.key(3)), "Map should not contain key 3 after remove.");
  }

  @Test
  public void test_entrySet_removeFromSnapshot() {
    final Map<K, V> map = this.populate(this.createMap(), 5);
    final Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();

    while(iterator.hasNext()) {
      final Map.Entry<K, V> entry = iterator.next();
      if(this.key(3).equals(entry.getKey())) {
        map.put(this.key(3), this.value(7));
        iterator.remove();
        break;
      }
    }

    assertEquals(this.value(7), map.get(this.key(3)), "Map should return value 7 for key 3.");
  }
}
