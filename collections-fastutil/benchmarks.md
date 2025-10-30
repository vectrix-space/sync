# Benchmarks

## Int2ObjectSyncMap

The following benchmarks are a comparison of `Int2ObjectSyncMap` vs `Int2ObjectMaps#synchronize()`.

### Summary

The `Int2ObjectSyncMap` has similar performance characteristics of the `Int2ObjectMaps#synchronize()`,
with an advantage in write speed.

The following results were recorded on an _M4 Macbook Pro_, using 8 threads and
100,000 entries for each benchmark:

- `Int2ObjectSyncMap` **get()** speed is...
  - 6122.2% **FASTER** than `Int2ObjectMaps#synchronize()` for an empty map.
  - 5319.3% **FASTER** than `Int2ObjectMaps#synchronize()` for a presized empty map.
  - 7666.7% **FASTER** than `Int2ObjectMaps#synchronize()` for a full map.

- `Int2ObjectSyncMap` **put()** speed is...
  - 1134.8% **FASTER** than `Int2ObjectMaps#synchronize()` for an empty map.
  - 1295.5% **FASTER** than `Int2ObjectMaps#synchronize()` for a presized empty map.
  - 2318.2% **FASTER** than `Int2ObjectMaps#synchronize()` for a full map.

- `Int2ObjectSyncMap` **put()** then **get()** speed is...
  - 3373.7% **FASTER** than `Int2ObjectMaps#synchronize()` for an empty map.
  - 3190.5% **FASTER** than `Int2ObjectMaps#synchronize()` for a presized empty map.
  - 2919.1% **FASTER** than `Int2ObjectMaps#synchronize()` for a full map.

### Results

```txt
Benchmark                            (implementation)       (mode)  (prime)  (readPercentage)  (size)   Mode  Cnt  Score   Error   Units
Int2ObjectSyncMapBenchmark.get_only           SyncMap         none    false                50  100000  thrpt    5  1.681 ± 0.001  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap         none     true                50  100000  thrpt    5  1.680 ± 0.001  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap      presize    false                50  100000  thrpt    5  1.680 ± 0.001  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap      presize     true                50  100000  thrpt    5  1.680 ± 0.001  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap  prepopulate    false                50  100000  thrpt    5  1.629 ± 0.004  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap  prepopulate     true                50  100000  thrpt    5  1.631 ± 0.016  ops/ns

Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap         none    false                50  100000  thrpt    5  0.035 ± 0.062  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap         none     true                50  100000  thrpt    5  0.027 ± 0.006  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap      presize    false                50  100000  thrpt    5  0.036 ± 0.069  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap      presize     true                50  100000  thrpt    5  0.031 ± 0.041  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.023 ± 0.011  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.021 ± 0.024  ops/ns

Int2ObjectSyncMapBenchmark.get_put            SyncMap         none    false                50  100000  thrpt    5  0.632 ± 0.047  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap         none     true                50  100000  thrpt    5  0.660 ± 0.218  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap      presize    false                50  100000  thrpt    5  0.645 ± 0.153  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap      presize     true                50  100000  thrpt    5  0.691 ± 0.367  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap  prepopulate    false                50  100000  thrpt    5  0.630 ± 0.071  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap  prepopulate     true                50  100000  thrpt    5  0.634 ± 0.095  ops/ns

Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap         none    false                50  100000  thrpt    5  0.021 ± 0.001  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap         none     true                50  100000  thrpt    5  0.019 ± 0.013  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap      presize    false                50  100000  thrpt    5  0.020 ± 0.010  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap      presize     true                50  100000  thrpt    5  0.021 ± 0.001  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.018 ± 0.014  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.021 ± 0.002  ops/ns

Int2ObjectSyncMapBenchmark.put_only           SyncMap         none    false                50  100000  thrpt    5  0.284 ± 0.044  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap         none     true                50  100000  thrpt    5  0.278 ± 0.086  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap      presize    false                50  100000  thrpt    5  0.307 ± 0.072  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap      presize     true                50  100000  thrpt    5  0.292 ± 0.044  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap  prepopulate    false                50  100000  thrpt    5  0.289 ± 0.043  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap  prepopulate     true                50  100000  thrpt    5  0.532 ± 0.137  ops/ns

Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap         none    false                50  100000  thrpt    5  0.023 ± 0.010  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap         none     true                50  100000  thrpt    5  0.023 ± 0.011  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap      presize    false                50  100000  thrpt    5  0.022 ± 0.002  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap      presize     true                50  100000  thrpt    5  0.025 ± 0.012  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.022 ± 0.007  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.022 ± 0.002  ops/ns
```
