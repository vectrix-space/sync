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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class BucketSyncMapBenchmark {
  @State(Scope.Benchmark)
  public static class Container {
    @Param({ "BucketSyncMap", "ConcurrentHashMap", "SynchronizedMap" })
    private String implementation;

    @Param({ "none", "presize", "prepopulate" })
    private String mode;

    @Param({ "false", "true" })
    private boolean prime;

    @Param({ "100000" })
    private int size;

    private Map<Integer, Integer> map;

    @Setup(Level.Iteration)
    public void setup() {
      final boolean presized = !"none".equalsIgnoreCase(this.mode);
      final boolean prepopulate = "prepopulate".equalsIgnoreCase(this.mode);

      switch(this.implementation) {
        case "BucketSyncMap" -> this.map = presized ? new BucketSyncMap<>(BucketSyncMap.FASTEST_SPREAD, this.size) : new BucketSyncMap<>(BucketSyncMap.FASTEST_SPREAD);
        case "ConcurrentHashMap" -> this.map = presized ? new ConcurrentHashMap<>(this.size) : new ConcurrentHashMap<>();
        case "SynchronizedMap" -> this.map = presized
          ? Collections.synchronizedMap(new HashMap<>(this.size))
          : Collections.synchronizedMap(new HashMap<>());
        default -> throw new IllegalArgumentException("Unable to identify implementation: " + this.implementation);
      }

      if(prepopulate) {
        for(int i = 0; i < this.size; i++) {
          this.map.put(i, i);
        }
      }

      if(this.prime) {
        if(this.map instanceof final BucketSyncMap<Integer, Integer> bucketSyncMap) {
          bucketSyncMap.promote(true);
        } else {
          for(int i = 0; i < this.size; i++) {
            this.map.get(i);
          }
        }
      }
    }
  }

  @State(Scope.Thread)
  public static class Sample {
    @Param({ "50" })
    private int readPercentage;

    private int cursor;
    private int length;
    private boolean[] isRead;

    @Setup(Level.Iteration)
    public void setup(final Container container) {
      this.length = container.size;
      this.isRead = new boolean[this.length];

      final SplittableRandom random = new SplittableRandom(Thread.currentThread().getId());
      for(int i = 0; i < this.length; i++) {
        this.isRead[i] = random.nextInt(100) < this.readPercentage;
      }

      this.cursor = 0;
    }

    private int next() {
      final int i = this.cursor;
      this.cursor = (i + 1) % this.length;
      return i;
    }
  }

  @Benchmark
  @Threads(8)
  public void get_only(final Container container, final Sample sample, final Blackhole blackhole) {
    final int key = sample.next();
    blackhole.consume(container.map.get(key));
  }

  @Benchmark
  @Threads(8)
  public void put_only(final Container container, final Sample sample, final Blackhole blackhole) {
    final int key = sample.next();
    blackhole.consume(container.map.put(key, key));
  }

  @Benchmark
  @Threads(8)
  public void get_put(final Container container, final Sample sample, final Blackhole blackhole) {
    final int key = sample.next();

    if(sample.isRead[key]) {
      blackhole.consume(container.map.get(key));
    } else {
      blackhole.consume(container.map.put(key, key));
    }
  }
}
