# Benchmarks

## Int2ObjectBucketSyncMap

The following benchmarks are a comparison of `Int2ObjectBucketSyncMap` vs `Int2ObjectMaps#synchronize()`.

### Summary

The `Int2ObjectBucketSyncMap` has similar performance characteristics of the `Int2ObjectMaps#synchronize()`,
with an advantage in write speed.

The following results were recorded on an _M4 Macbook Pro_, using 8 threads and
100,000 entries for each benchmark:

- `Int2ObjectBucketSyncMap` **get()** speed is...
  - +65x to +50x difference in comparison to `Int2ObjectMaps#synchronize()` for an empty map.
  - +70x to +60x difference in comparison to `Int2ObjectMaps#synchronize()` for a presized empty map.
  - +115x to +110x difference in comparison to `Int2ObjectMaps#synchronize()` for a full map.

- `Int2ObjectBucketSyncMap` **put()** speed is...
  - +15x difference in comparison to `Int2ObjectMaps#synchronize()` for an empty map.
  - +15x difference in comparison to `Int2ObjectMaps#synchronize()` for a presized empty map.
  - +25x to +15x difference in comparison to `Int2ObjectMaps#synchronize()` for a full map.

- `Int2ObjectBucketSyncMap` **put()** then **get()** speed is...
  - +35x difference in comparison to `Int2ObjectMaps#synchronize()` for an empty map.
  - +35x difference in comparison to `Int2ObjectMaps#synchronize()` for a presized empty map.
  - +35x to +30x difference in comparison to `Int2ObjectMaps#synchronize()` for a full map.

### Results

```txt
Benchmark                                  (implementation)       (mode)  (prime)  (readPercentage)  (size)   Mode  Cnt  Score    Error   Units
Int2ObjectBucketSyncMapBenchmark.get_only     BucketSyncMap         none    false                50  100000  thrpt    5  1.681 ±  0.001  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only     BucketSyncMap         none     true                50  100000  thrpt    5  1.680 ±  0.001  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only     BucketSyncMap      presize    false                50  100000  thrpt    5  1.681 ±  0.002  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only     BucketSyncMap      presize     true                50  100000  thrpt    5  1.680 ±  0.001  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only     BucketSyncMap  prepopulate    false                50  100000  thrpt    5  2.472 ±  0.009  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only     BucketSyncMap  prepopulate     true                50  100000  thrpt    5  2.471 ±  0.008  ops/ns

Int2ObjectBucketSyncMapBenchmark.get_only   SynchronizedMap         none    false                50  100000  thrpt    5  0.026 ±  0.006  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only   SynchronizedMap         none     true                50  100000  thrpt    5  0.033 ±  0.059  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only   SynchronizedMap      presize    false                50  100000  thrpt    5  0.025 ±  0.002  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only   SynchronizedMap      presize     true                50  100000  thrpt    5  0.027 ±  0.006  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only   SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.022 ±  0.002  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only   SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.023 ±  0.010  ops/ns

Int2ObjectBucketSyncMapBenchmark.get_put      BucketSyncMap         none    false                50  100000  thrpt    5  0.626 ±  0.079  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put      BucketSyncMap         none     true                50  100000  thrpt    5  0.630 ±  0.060  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put      BucketSyncMap      presize    false                50  100000  thrpt    5  0.630 ±  0.033  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put      BucketSyncMap      presize     true                50  100000  thrpt    5  0.649 ±  0.187  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put      BucketSyncMap  prepopulate    false                50  100000  thrpt    5  0.630 ±  0.023  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put      BucketSyncMap  prepopulate     true                50  100000  thrpt    5  0.640 ±  0.035  ops/ns

Int2ObjectBucketSyncMapBenchmark.get_put    SynchronizedMap         none    false                50  100000  thrpt    5  0.018 ±  0.013  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put    SynchronizedMap         none     true                50  100000  thrpt    5  0.018 ±  0.014  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put    SynchronizedMap      presize    false                50  100000  thrpt    5  0.020 ±  0.013  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put    SynchronizedMap      presize     true                50  100000  thrpt    5  0.018 ±  0.016  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.021 ±  0.003  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.020 ±  0.003  ops/ns

Int2ObjectBucketSyncMapBenchmark.put_only     BucketSyncMap         none    false                50  100000  thrpt    5  0.282 ±  0.036  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only     BucketSyncMap         none     true                50  100000  thrpt    5  0.314 ±  0.115  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only     BucketSyncMap      presize    false                50  100000  thrpt    5  0.298 ±  0.051  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only     BucketSyncMap      presize     true                50  100000  thrpt    5  0.312 ±  0.073  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only     BucketSyncMap  prepopulate    false                50  100000  thrpt    5  0.282 ±  0.013  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only     BucketSyncMap  prepopulate     true                50  100000  thrpt    5  0.521 ±  0.062  ops/ns

Int2ObjectBucketSyncMapBenchmark.put_only   SynchronizedMap         none    false                50  100000  thrpt    5  0.020 ±  0.006  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only   SynchronizedMap         none     true                50  100000  thrpt    5  0.020 ±  0.005  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only   SynchronizedMap      presize    false                50  100000  thrpt    5  0.021 ±  0.005  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only   SynchronizedMap      presize     true                50  100000  thrpt    5  0.022 ±  0.001  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only   SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.021 ±  0.001  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only   SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.021 ±  0.001  ops/ns
```
