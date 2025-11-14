# Benchmarks

## Int2ObjectBucketSyncMap

The following benchmarks are a comparison of `Int2ObjectBucketSyncMap` vs `Int2ObjectMaps#synchronize()`.

### Summary

The `Int2ObjectBucketSyncMap` has similar performance characteristics of the `Int2ObjectMaps#synchronize()`,
with an advantage in write speed.

The following results were recorded on an _M4 Macbook Pro_, using 8 threads and
100,000 entries for each benchmark:

- `Int2ObjectBucketSyncMap` **get()** speed is...
  - 50x faster than `Int2ObjectMaps#synchronize()` for an empty map.
  - 50x faster than `Int2ObjectMaps#synchronize()` for a presized empty map.
  - 75x faster than `Int2ObjectMaps#synchronize()` for a full map.

- `Int2ObjectBucketSyncMap` **put()** speed is...
  - 15x faster than `Int2ObjectMaps#synchronize()` for an empty map.
  - 15x faster than `Int2ObjectMaps#synchronize()` for a presized empty map.
  - 25x faster than `Int2ObjectMaps#synchronize()` for a full map.

- `Int2ObjectBucketSyncMap` **put()** then **get()** speed is...
  - 35x faster than `Int2ObjectMaps#synchronize()` for an empty map.
  - 40x faster than `Int2ObjectMaps#synchronize()` for a presized empty map.
  - 35x faster than `Int2ObjectMaps#synchronize()` for a full map.

### Results

```txt
Benchmark                                  (implementation)       (mode)  (prime)  (readPercentage)  (size)   Mode  Cnt  Score   Error   Units
Int2ObjectBucketSyncMapBenchmark.get_only     BucketSyncMap         none    false                50  100000  thrpt    5  1.680 ± 0.001  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only     BucketSyncMap         none     true                50  100000  thrpt    5  1.680 ± 0.001  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only     BucketSyncMap      presize    false                50  100000  thrpt    5  1.681 ± 0.001  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only     BucketSyncMap      presize     true                50  100000  thrpt    5  1.681 ± 0.001  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only     BucketSyncMap  prepopulate    false                50  100000  thrpt    5  1.637 ± 0.005  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only     BucketSyncMap  prepopulate     true                50  100000  thrpt    5  1.634 ± 0.002  ops/ns

Int2ObjectBucketSyncMapBenchmark.get_only   SynchronizedMap         none    false                50  100000  thrpt    5  0.041 ± 0.074  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only   SynchronizedMap         none     true                50  100000  thrpt    5  0.039 ± 0.068  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only   SynchronizedMap      presize    false                50  100000  thrpt    5  0.038 ± 0.077  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only   SynchronizedMap      presize     true                50  100000  thrpt    5  0.038 ± 0.075  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only   SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.023 ± 0.035  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_only   SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.021 ± 0.016  ops/ns

Int2ObjectBucketSyncMapBenchmark.get_put      BucketSyncMap         none    false                50  100000  thrpt    5  0.632 ± 0.058  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put      BucketSyncMap         none     true                50  100000  thrpt    5  0.639 ± 0.061  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put      BucketSyncMap      presize    false                50  100000  thrpt    5  0.638 ± 0.031  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put      BucketSyncMap      presize     true                50  100000  thrpt    5  0.665 ± 0.278  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put      BucketSyncMap  prepopulate    false                50  100000  thrpt    5  0.645 ± 0.068  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put      BucketSyncMap  prepopulate     true                50  100000  thrpt    5  0.635 ± 0.083  ops/ns

Int2ObjectBucketSyncMapBenchmark.get_put    SynchronizedMap         none    false                50  100000  thrpt    5  0.018 ± 0.010  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put    SynchronizedMap         none     true                50  100000  thrpt    5  0.019 ± 0.013  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put    SynchronizedMap      presize    false                50  100000  thrpt    5  0.020 ± 0.002  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put    SynchronizedMap      presize     true                50  100000  thrpt    5  0.015 ± 0.010  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.017 ± 0.014  ops/ns
Int2ObjectBucketSyncMapBenchmark.get_put    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.019 ± 0.010  ops/ns

Int2ObjectBucketSyncMapBenchmark.put_only     BucketSyncMap         none    false                50  100000  thrpt    5  0.303 ± 0.027  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only     BucketSyncMap         none     true                50  100000  thrpt    5  0.309 ± 0.062  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only     BucketSyncMap      presize    false                50  100000  thrpt    5  0.297 ± 0.028  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only     BucketSyncMap      presize     true                50  100000  thrpt    5  0.309 ± 0.027  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only     BucketSyncMap  prepopulate    false                50  100000  thrpt    5  0.288 ± 0.041  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only     BucketSyncMap  prepopulate     true                50  100000  thrpt    5  0.508 ± 0.036  ops/ns

Int2ObjectBucketSyncMapBenchmark.put_only   SynchronizedMap         none    false                50  100000  thrpt    5  0.022 ± 0.002  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only   SynchronizedMap         none     true                50  100000  thrpt    5  0.022 ± 0.008  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only   SynchronizedMap      presize    false                50  100000  thrpt    5  0.026 ± 0.026  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only   SynchronizedMap      presize     true                50  100000  thrpt    5  0.023 ± 0.011  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only   SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.026 ± 0.028  ops/ns
Int2ObjectBucketSyncMapBenchmark.put_only   SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.022 ± 0.002  ops/ns
```
