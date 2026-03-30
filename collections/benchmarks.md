# Benchmarks

## BucketSyncMap

The following benchmarks are a comparison of `BucketSyncMap` vs `Collections#synchronizedMap()` vs `ConcurrentHashMap`.

### Summary

The `BucketSyncMap` has similar performance characteristics of the `ConcurrentHashMap`,
with an advantage in write speed.

The following results were recorded on an _M4 Macbook Pro_, using 8 threads and
100,000 entries for each benchmark:

- `BucketSyncMap` **get()** speed is...
  - +1.35x to 0x difference in comparison to `ConcurrentHashMap` for an empty map.
  - 0x difference in comparison to `ConcurrentHashMap` for a presized empty map.
  - -1.2x to -1.3x in comparison to `ConcurrentHashMap` for a full map.

- `BucketSyncMap` **put()** speed is...
  - +1.3x to +1.2x difference in comparison to `ConcurrentHashMap` for an empty map.
  - -1.2x to -1.25x difference in comparison to `ConcurrentHashMap` for a presized empty map.
  - +1.2x to -1.25x difference in comparison to `ConcurrentHashMap` for a full map.

- `BucketSyncMap` **put()** then **get()** speed is...
  - +1.75x to +1.7x difference in comparison to `ConcurrentHashMap` for an empty map.
  - +1.4x difference in comparison to `ConcurrentHashMap` for a presized empty map.
  - +1.5x difference in comparison to `ConcurrentHashMap` for a full map.

- `ForwardingSyncMap` **get()** speed is...
  - 0x to -1.4x difference in comparison to `ConcurrentHashMap` for an empty map.
  - 0x to -1.4x difference in comparison to `ConcurrentHashMap` for a presized empty map.
  - -1.2x to -1.5x in comparison to `ConcurrentHashMap` for a full map.

- `ForwardingSyncMap` **put()** speed is...
  - -2.6x difference in comparison to `ConcurrentHashMap` for an empty map.
  - -2.8x difference in comparison to `ConcurrentHashMap` for a presized empty map.
  - +1.35x to -2.75x difference in comparison to `ConcurrentHashMap` for a full map.

- `ForwardingSyncMap` **put()** then **get()** speed is...
  - +1.75x to +1.65x difference in comparison to `ConcurrentHashMap` for an empty map.
  - +1.45x to +1.4x difference in comparison to `ConcurrentHashMap` for a presized empty map.
  - +1.5x difference in comparison to `ConcurrentHashMap` for a full map.

### Results

```txt
Benchmark                   (implementation)       (mode)  (prime)  (readPercentage)  (size)   Mode  Cnt  Score   Error   Units
SyncMapBenchmark.get_only  ConcurrentHashMap         none    false                50  100000  thrpt    5  1.623 ± 0.002  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap         none     true                50  100000  thrpt    5  2.378 ± 0.004  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap      presize    false                50  100000  thrpt    5  2.367 ± 0.013  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap      presize     true                50  100000  thrpt    5  2.371 ± 0.005  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  2.385 ± 0.023  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  2.393 ± 0.006  ops/ns

SyncMapBenchmark.get_only      BucketSyncMap         none    false                50  100000  thrpt    5  2.305 ± 0.069  ops/ns
SyncMapBenchmark.get_only      BucketSyncMap         none     true                50  100000  thrpt    5  2.307 ± 0.047  ops/ns
SyncMapBenchmark.get_only      BucketSyncMap      presize    false                50  100000  thrpt    5  2.293 ± 0.056  ops/ns
SyncMapBenchmark.get_only      BucketSyncMap      presize     true                50  100000  thrpt    5  2.283 ± 0.056  ops/ns
SyncMapBenchmark.get_only      BucketSyncMap  prepopulate    false                50  100000  thrpt    5  1.898 ± 0.027  ops/ns
SyncMapBenchmark.get_only      BucketSyncMap  prepopulate     true                50  100000  thrpt    5  1.817 ± 0.023  ops/ns

SyncMapBenchmark.get_only  ForwardingSyncMap         none    false                50  100000  thrpt    5  1.618 ± 0.005  ops/ns
SyncMapBenchmark.get_only  ForwardingSyncMap         none     true                50  100000  thrpt    5  1.623 ± 0.003  ops/ns
SyncMapBenchmark.get_only  ForwardingSyncMap      presize    false                50  100000  thrpt    5  2.372 ± 0.017  ops/ns
SyncMapBenchmark.get_only  ForwardingSyncMap      presize     true                50  100000  thrpt    5  1.616 ± 0.002  ops/ns
SyncMapBenchmark.get_only  ForwardingSyncMap  prepopulate    false                50  100000  thrpt    5  1.345 ± 0.012  ops/ns
SyncMapBenchmark.get_only  ForwardingSyncMap  prepopulate     true                50  100000  thrpt    5  1.947 ± 0.121  ops/ns

SyncMapBenchmark.get_only    SynchronizedMap         none    false                50  100000  thrpt    5  0.031 ± 0.044  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap         none     true                50  100000  thrpt    5  0.025 ± 0.002  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap      presize    false                50  100000  thrpt    5  0.026 ± 0.003  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap      presize     true                50  100000  thrpt    5  0.025 ± 0.001  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.025 ± 0.002  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.025 ± 0.005  ops/ns

SyncMapBenchmark.get_put       BucketSyncMap         none    false                50  100000  thrpt    5  0.559 ± 0.044  ops/ns
SyncMapBenchmark.get_put       BucketSyncMap         none     true                50  100000  thrpt    5  0.565 ± 0.045  ops/ns
SyncMapBenchmark.get_put       BucketSyncMap      presize    false                50  100000  thrpt    5  0.566 ± 0.105  ops/ns
SyncMapBenchmark.get_put       BucketSyncMap      presize     true                50  100000  thrpt    5  0.558 ± 0.082  ops/ns
SyncMapBenchmark.get_put       BucketSyncMap  prepopulate    false                50  100000  thrpt    5  0.556 ± 0.059  ops/ns
SyncMapBenchmark.get_put       BucketSyncMap  prepopulate     true                50  100000  thrpt    5  0.544 ± 0.100  ops/ns

SyncMapBenchmark.get_put   ForwardingSyncMap         none    false                50  100000  thrpt    5  0.534 ± 0.014  ops/ns
SyncMapBenchmark.get_put   ForwardingSyncMap         none     true                50  100000  thrpt    5  0.557 ± 0.015  ops/ns
SyncMapBenchmark.get_put   ForwardingSyncMap      presize    false                50  100000  thrpt    5  0.574 ± 0.062  ops/ns
SyncMapBenchmark.get_put   ForwardingSyncMap      presize     true                50  100000  thrpt    5  0.557 ± 0.033  ops/ns
SyncMapBenchmark.get_put   ForwardingSyncMap  prepopulate    false                50  100000  thrpt    5  0.562 ± 0.038  ops/ns
SyncMapBenchmark.get_put   ForwardingSyncMap  prepopulate     true                50  100000  thrpt    5  0.562 ± 0.067  ops/ns

SyncMapBenchmark.get_put   ConcurrentHashMap         none    false                50  100000  thrpt    5  0.270 ± 0.021  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap         none     true                50  100000  thrpt    5  0.257 ± 0.002  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap      presize    false                50  100000  thrpt    5  0.378 ± 0.096  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap      presize     true                50  100000  thrpt    5  0.362 ± 0.120  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  0.334 ± 0.012  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  0.333 ± 0.007  ops/ns

SyncMapBenchmark.get_put     SynchronizedMap         none    false                50  100000  thrpt    5  0.021 ± 0.001  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap         none     true                50  100000  thrpt    5  0.021 ± 0.001  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap      presize    false                50  100000  thrpt    5  0.019 ± 0.010  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap      presize     true                50  100000  thrpt    5  0.019 ± 0.013  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.019 ± 0.013  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.018 ± 0.014  ops/ns

SyncMapBenchmark.put_only      BucketSyncMap         none    false                50  100000  thrpt    5  0.270 ± 0.074  ops/ns
SyncMapBenchmark.put_only      BucketSyncMap         none     true                50  100000  thrpt    5  0.265 ± 0.018  ops/ns
SyncMapBenchmark.put_only      BucketSyncMap      presize    false                50  100000  thrpt    5  0.258 ± 0.033  ops/ns
SyncMapBenchmark.put_only      BucketSyncMap      presize     true                50  100000  thrpt    5  0.256 ± 0.035  ops/ns
SyncMapBenchmark.put_only      BucketSyncMap  prepopulate    false                50  100000  thrpt    5  0.255 ± 0.032  ops/ns
SyncMapBenchmark.put_only      BucketSyncMap  prepopulate     true                50  100000  thrpt    5  0.395 ± 0.066  ops/ns

SyncMapBenchmark.put_only  ForwardingSyncMap         none    false                50  100000  thrpt    5  0.022 ± 0.013  ops/ns
SyncMapBenchmark.put_only  ForwardingSyncMap         none     true                50  100000  thrpt    5  0.021 ± 0.011  ops/ns
SyncMapBenchmark.put_only  ForwardingSyncMap      presize    false                50  100000  thrpt    5  0.019 ± 0.006  ops/ns
SyncMapBenchmark.put_only  ForwardingSyncMap      presize     true                50  100000  thrpt    5  0.019 ± 0.007  ops/ns
SyncMapBenchmark.put_only  ForwardingSyncMap  prepopulate    false                50  100000  thrpt    5  0.021 ± 0.003  ops/ns
SyncMapBenchmark.put_only  ForwardingSyncMap  prepopulate     true                50  100000  thrpt    5  0.464 ± 0.039  ops/ns

SyncMapBenchmark.put_only  ConcurrentHashMap         none    false                50  100000  thrpt    5  0.210 ± 0.208  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap         none     true                50  100000  thrpt    5  0.196 ± 0.212  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap      presize    false                50  100000  thrpt    5  0.322 ± 0.050  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap      presize     true                50  100000  thrpt    5  0.329 ± 0.062  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  0.334 ± 0.022  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  0.329 ± 0.036  ops/ns

SyncMapBenchmark.put_only    SynchronizedMap         none    false                50  100000  thrpt    5  0.021 ± 0.003  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap         none     true                50  100000  thrpt    5  0.022 ± 0.006  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap      presize    false                50  100000  thrpt    5  0.020 ± 0.012  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap      presize     true                50  100000  thrpt    5  0.023 ± 0.005  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.021 ± 0.006  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.022 ± 0.002  ops/ns
```
