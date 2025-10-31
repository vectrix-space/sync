# Benchmarks

## Int2ObjectSyncMap

The following benchmarks are a comparison of `Int2ObjectSyncMap` vs `Int2ObjectMaps#synchronize()`.

### Summary

The `Int2ObjectSyncMap` has similar performance characteristics of the `Int2ObjectMaps#synchronize()`,
with an advantage in write speed.

The following results were recorded on an _M4 Macbook Pro_, using 8 threads and
100,000 entries for each benchmark:

- `Int2ObjectSyncMap` **get()** speed is...
  - 6369.2% **FASTER** than `Int2ObjectMaps#synchronize()` for an empty map.
  - 4702.9% **FASTER** than `Int2ObjectMaps#synchronize()` for a presized empty map.
  - 7676.2% **FASTER** than `Int2ObjectMaps#synchronize()` for a full map.

- `Int2ObjectSyncMap` **put()** speed is...
  - 1168.2% **FASTER** than `Int2ObjectMaps#synchronize()` for an empty map.
  - 1252.2% **FASTER** than `Int2ObjectMaps#synchronize()` for a presized empty map.
  - 2380.9% **FASTER** than `Int2ObjectMaps#synchronize()` for a full map.

- `Int2ObjectSyncMap` **put()** then **get()** speed is...
  - 3741.2% **FASTER** than `Int2ObjectMaps#synchronize()` for an empty map.
  - 3644.4% **FASTER** than `Int2ObjectMaps#synchronize()` for a presized empty map.
  - 2985.7% **FASTER** than `Int2ObjectMaps#synchronize()` for a full map.

### Results

```txt
Benchmark                            (implementation)       (mode)  (prime)  (readPercentage)  (size)   Mode  Cnt  Score   Error   Units
Int2ObjectSyncMapBenchmark.get_only           SyncMap         none    false                50  100000  thrpt    5  1.681 ± 0.001  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap         none     true                50  100000  thrpt    5  1.682 ± 0.001  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap      presize    false                50  100000  thrpt    5  1.681 ± 0.001  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap      presize     true                50  100000  thrpt    5  1.681 ± 0.002  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap  prepopulate    false                50  100000  thrpt    5  1.640 ± 0.013  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap  prepopulate     true                50  100000  thrpt    5  1.633 ± 0.010  ops/ns

Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap         none    false                50  100000  thrpt    5  0.042 ± 0.080  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap         none     true                50  100000  thrpt    5  0.026 ± 0.002  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap      presize    false                50  100000  thrpt    5  0.026 ± 0.006  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap      presize     true                50  100000  thrpt    5  0.035 ± 0.055  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.022 ± 0.004  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.021 ± 0.010  ops/ns

Int2ObjectSyncMapBenchmark.get_put            SyncMap         none    false                50  100000  thrpt    5  0.653 ± 0.329  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap         none     true                50  100000  thrpt    5  0.646 ± 0.096  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap      presize    false                50  100000  thrpt    5  0.650 ± 0.045  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap      presize     true                50  100000  thrpt    5  0.674 ± 0.272  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap  prepopulate    false                50  100000  thrpt    5  0.648 ± 0.059  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap  prepopulate     true                50  100000  thrpt    5  0.627 ± 0.049  ops/ns

Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap         none    false                50  100000  thrpt    5  0.017 ± 0.013  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap         none     true                50  100000  thrpt    5  0.018 ± 0.013  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap      presize    false                50  100000  thrpt    5  0.018 ± 0.011  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap      presize     true                50  100000  thrpt    5  0.018 ± 0.015  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.021 ± 0.003  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.018 ± 0.015  ops/ns

Int2ObjectSyncMapBenchmark.put_only           SyncMap         none    false                50  100000  thrpt    5  0.291 ± 0.121  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap         none     true                50  100000  thrpt    5  0.279 ± 0.056  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap      presize    false                50  100000  thrpt    5  0.304 ± 0.022  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap      presize     true                50  100000  thrpt    5  0.311 ± 0.080  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap  prepopulate    false                50  100000  thrpt    5  0.284 ± 0.065  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap  prepopulate     true                50  100000  thrpt    5  0.521 ± 0.028  ops/ns

Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap         none    false                50  100000  thrpt    5  0.024 ± 0.023  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap         none     true                50  100000  thrpt    5  0.022 ± 0.007  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap      presize    false                50  100000  thrpt    5  0.022 ± 0.002  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap      presize     true                50  100000  thrpt    5  0.023 ± 0.005  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.022 ± 0.001  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.021 ± 0.001  ops/ns
```
