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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Threads(16)
public class SyncMapBenchmark {
  private static final int SIZE = 100_000;

  @Param({ "SyncMap", "SynchronizedMap", "ConcurrentHashMap" })
  private String implementation;

  @Param({ "none", "presize", "prepopulate" })
  private String mode;

  private Map<Integer, Integer> map;

  @Setup(Level.Iteration)
  public void setup() {
    final boolean presized = !"none".equalsIgnoreCase(this.mode);
    final boolean prepopulate = "prepopulate".equalsIgnoreCase(this.mode);

    if ("SyncMap".equalsIgnoreCase(this.implementation)) {
      this.map = presized ? new SyncMap<>(SyncMapBenchmark.SIZE) : new SyncMap<>();
    } else if ("SynchronizedMap".equalsIgnoreCase(this.implementation)) {
      this.map = presized
        ? Collections.synchronizedMap(new HashMap<>(SyncMapBenchmark.SIZE))
        : Collections.synchronizedMap(new HashMap<>());
    } else if ("ConcurrentHashMap".equalsIgnoreCase(this.implementation)) {
      this.map = presized ? new ConcurrentHashMap<>(SyncMapBenchmark.SIZE) : new ConcurrentHashMap<>();
    }

    if(prepopulate) {
      for(int i = 0; i < SyncMapBenchmark.SIZE; i++) {
        this.map.put(i, i);
      }
    }

    if(presized && (this.map instanceof final SyncMap<Integer, Integer> sync)) {
      sync.amend();
    }
  }

  @Benchmark
  @Threads(8)
  @OperationsPerInvocation(SyncMapBenchmark.SIZE)
  public void getOnly(final Blackhole blackhole) {
    for(int i = 0; i < SyncMapBenchmark.SIZE; i++) {
      blackhole.consume(this.map.get(i));
    }
  }

  @Benchmark
  @Threads(8)
  @OperationsPerInvocation(SyncMapBenchmark.SIZE)
  public void putOnly(final Blackhole blackhole) {
    for(int i = 0; i < SyncMapBenchmark.SIZE; i++) {
      blackhole.consume(this.map.put(i, i));
    }
  }

  @Benchmark
  @Threads(8)
  @OperationsPerInvocation(SyncMapBenchmark.SIZE)
  public void putAndGet(final Blackhole blackhole) {
    for(int i = 0; i < SyncMapBenchmark.SIZE; i++) {
      blackhole.consume(this.map.put(i, i));
      blackhole.consume(this.map.get(i));
    }
  }

  @Benchmark
  @Threads(8)
  @OperationsPerInvocation(SyncMapBenchmark.SIZE)
  public void randomPutAndGet(final Blackhole blackhole) {
    final Random random = new Random(8);
    for(int i = 0; i < SyncMapBenchmark.SIZE; i++) {
      final int key = random.nextInt(SyncMapBenchmark.SIZE);
      if(random.nextBoolean()) {
        blackhole.consume(this.map.put(key, key));
      } else {
        blackhole.consume(this.map.get(key));
      }
    }
  }
}
