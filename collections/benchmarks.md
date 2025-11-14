# Benchmarks

## BucketSyncMap

The following benchmarks are a comparison of `BucketSyncMap` vs `Collections#synchronizedMap()` vs `ConcurrentHashMap`.

### Summary

The `BucketSyncMap` has similar performance characteristics of the `ConcurrentHashMap`,
with an advantage in write speed.

The following results were recorded on an _M4 Macbook Pro_, using 8 threads and
100,000 entries for each benchmark:

- `BucketSyncMap` **get()** speed is...
  - 1.5x faster than `ConcurrentHashMap` for an empty map.
  - 1.5x faster than `ConcurrentHashMap` for a presized empty map.
  - 1.25x faster than `ConcurrentHashMap` for a full map.

- `BucketSyncMap` **put()** speed is...
  - 1.5x faster than `ConcurrentHashMap` for an empty map.
  - 1x faster than `ConcurrentHashMap` for a presized empty map.
  - 1.25x faster than `ConcurrentHashMap` for a full map.

- `BucketSyncMap` **put()** then **get()** speed is...
  - 2x faster than `ConcurrentHashMap` for an empty map.
  - 1.75x faster than `ConcurrentHashMap` for a presized empty map.
  - 1.5x faster than `ConcurrentHashMap` for a full map.

### Results

```txt
Benchmark                         (implementation)       (mode)  (prime)  (readPercentage)  (size)   Mode  Cnt  Score   Error   Units
BucketSyncMapBenchmark.get_only      BucketSyncMap         none    false                50  100000  thrpt    5  2.352 ± 0.075  ops/ns
BucketSyncMapBenchmark.get_only      BucketSyncMap         none     true                50  100000  thrpt    5  2.340 ± 0.025  ops/ns
BucketSyncMapBenchmark.get_only      BucketSyncMap      presize    false                50  100000  thrpt    5  2.346 ± 0.011  ops/ns
BucketSyncMapBenchmark.get_only      BucketSyncMap      presize     true                50  100000  thrpt    5  2.331 ± 0.037  ops/ns
BucketSyncMapBenchmark.get_only      BucketSyncMap  prepopulate    false                50  100000  thrpt    5  1.947 ± 0.033  ops/ns
BucketSyncMapBenchmark.get_only      BucketSyncMap  prepopulate     true                50  100000  thrpt    5  1.936 ± 0.026  ops/ns

BucketSyncMapBenchmark.get_only  ConcurrentHashMap         none    false                50  100000  thrpt    5  1.630 ± 0.002  ops/ns
BucketSyncMapBenchmark.get_only  ConcurrentHashMap         none     true                50  100000  thrpt    5  2.415 ± 0.001  ops/ns
BucketSyncMapBenchmark.get_only  ConcurrentHashMap      presize    false                50  100000  thrpt    5  1.631 ± 0.001  ops/ns
BucketSyncMapBenchmark.get_only  ConcurrentHashMap      presize     true                50  100000  thrpt    5  2.415 ± 0.002  ops/ns
BucketSyncMapBenchmark.get_only  ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  1.593 ± 0.002  ops/ns
BucketSyncMapBenchmark.get_only  ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  2.376 ± 0.032  ops/ns

BucketSyncMapBenchmark.get_only    SynchronizedMap         none    false                50  100000  thrpt    5  0.026 ± 0.005  ops/ns
BucketSyncMapBenchmark.get_only    SynchronizedMap         none     true                50  100000  thrpt    5  0.031 ± 0.041  ops/ns
BucketSyncMapBenchmark.get_only    SynchronizedMap      presize    false                50  100000  thrpt    5  0.027 ± 0.007  ops/ns
BucketSyncMapBenchmark.get_only    SynchronizedMap      presize     true                50  100000  thrpt    5  0.028 ± 0.014  ops/ns
BucketSyncMapBenchmark.get_only    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.031 ± 0.050  ops/ns
BucketSyncMapBenchmark.get_only    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.025 ± 0.003  ops/ns

BucketSyncMapBenchmark.get_put       BucketSyncMap         none    false                50  100000  thrpt    5  0.550 ± 0.109  ops/ns
BucketSyncMapBenchmark.get_put       BucketSyncMap         none     true                50  100000  thrpt    5  0.536 ± 0.013  ops/ns
BucketSyncMapBenchmark.get_put       BucketSyncMap      presize    false                50  100000  thrpt    5  0.575 ± 0.136  ops/ns
BucketSyncMapBenchmark.get_put       BucketSyncMap      presize     true                50  100000  thrpt    5  0.554 ± 0.072  ops/ns
BucketSyncMapBenchmark.get_put       BucketSyncMap  prepopulate    false                50  100000  thrpt    5  0.539 ± 0.024  ops/ns
BucketSyncMapBenchmark.get_put       BucketSyncMap  prepopulate     true                50  100000  thrpt    5  0.533 ± 0.031  ops/ns

BucketSyncMapBenchmark.get_put   ConcurrentHashMap         none    false                50  100000  thrpt    5  0.261 ± 0.115  ops/ns
BucketSyncMapBenchmark.get_put   ConcurrentHashMap         none     true                50  100000  thrpt    5  0.267 ± 0.203  ops/ns
BucketSyncMapBenchmark.get_put   ConcurrentHashMap      presize    false                50  100000  thrpt    5  0.327 ± 0.077  ops/ns
BucketSyncMapBenchmark.get_put   ConcurrentHashMap      presize     true                50  100000  thrpt    5  0.324 ± 0.030  ops/ns
BucketSyncMapBenchmark.get_put   ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  0.344 ± 0.018  ops/ns
BucketSyncMapBenchmark.get_put   ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  0.342 ± 0.021  ops/ns

BucketSyncMapBenchmark.get_put     SynchronizedMap         none    false                50  100000  thrpt    5  0.017 ± 0.012  ops/ns
BucketSyncMapBenchmark.get_put     SynchronizedMap         none     true                50  100000  thrpt    5  0.017 ± 0.016  ops/ns
BucketSyncMapBenchmark.get_put     SynchronizedMap      presize    false                50  100000  thrpt    5  0.018 ± 0.014  ops/ns
BucketSyncMapBenchmark.get_put     SynchronizedMap      presize     true                50  100000  thrpt    5  0.017 ± 0.015  ops/ns
BucketSyncMapBenchmark.get_put     SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.016 ± 0.014  ops/ns
BucketSyncMapBenchmark.get_put     SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.019 ± 0.013  ops/ns

BucketSyncMapBenchmark.put_only      BucketSyncMap         none    false                50  100000  thrpt    5  0.272 ± 0.024  ops/ns
BucketSyncMapBenchmark.put_only      BucketSyncMap         none     true                50  100000  thrpt    5  0.264 ± 0.019  ops/ns
BucketSyncMapBenchmark.put_only      BucketSyncMap      presize    false                50  100000  thrpt    5  0.257 ± 0.035  ops/ns
BucketSyncMapBenchmark.put_only      BucketSyncMap      presize     true                50  100000  thrpt    5  0.253 ± 0.027  ops/ns
BucketSyncMapBenchmark.put_only      BucketSyncMap  prepopulate    false                50  100000  thrpt    5  0.247 ± 0.048  ops/ns
BucketSyncMapBenchmark.put_only      BucketSyncMap  prepopulate     true                50  100000  thrpt    5  0.420 ± 0.167  ops/ns

BucketSyncMapBenchmark.put_only  ConcurrentHashMap         none    false                50  100000  thrpt    5  0.176 ± 0.026  ops/ns
BucketSyncMapBenchmark.put_only  ConcurrentHashMap         none     true                50  100000  thrpt    5  0.190 ± 0.092  ops/ns
BucketSyncMapBenchmark.put_only  ConcurrentHashMap      presize    false                50  100000  thrpt    5  0.313 ± 0.046  ops/ns
BucketSyncMapBenchmark.put_only  ConcurrentHashMap      presize     true                50  100000  thrpt    5  0.262 ± 0.095  ops/ns
BucketSyncMapBenchmark.put_only  ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  0.327 ± 0.026  ops/ns
BucketSyncMapBenchmark.put_only  ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  0.333 ± 0.124  ops/ns

BucketSyncMapBenchmark.put_only    SynchronizedMap         none    false                50  100000  thrpt    5  0.026 ± 0.039  ops/ns
BucketSyncMapBenchmark.put_only    SynchronizedMap         none     true                50  100000  thrpt    5  0.027 ± 0.039  ops/ns
BucketSyncMapBenchmark.put_only    SynchronizedMap      presize    false                50  100000  thrpt    5  0.024 ± 0.019  ops/ns
BucketSyncMapBenchmark.put_only    SynchronizedMap      presize     true                50  100000  thrpt    5  0.021 ± 0.009  ops/ns
BucketSyncMapBenchmark.put_only    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.022 ± 0.002  ops/ns
BucketSyncMapBenchmark.put_only    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.022 ± 0.005  ops/ns
```
