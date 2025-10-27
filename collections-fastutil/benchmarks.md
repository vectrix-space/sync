# Benchmarks

## Int2ObjectSyncMap

The following benchmarks are a comparison of `Int2ObjectSyncMap` vs `Int2ObjectMaps#synchronize()`.

### Summary

The `Int2ObjectSyncMap` has similar performance characteristics of the `Int2ObjectMaps#synchronize()`,
with an advantage in write speed.

The following results were recorded on an _M4 Macbook Pro_, using 8 threads and
100,000 entries for each benchmark:

- `Int2ObjectSyncMap` **get()** speed is...
  - 5693.1% **FASTER** than `Int2ObjectMaps#synchronize()` for an empty map.
  - 6365.4% **FASTER** than `Int2ObjectMaps#synchronize()` for a presized empty map.
  - 12794.7% **FASTER** than `Int2ObjectMaps#synchronize()` for a full map.

- `Int2ObjectSyncMap` **put()** speed is...
  - 1052.4% **FASTER** than `Int2ObjectMaps#synchronize()` for an empty map.
  - 869.2% **FASTER** than `Int2ObjectMaps#synchronize()` for a presized empty map.
  - 2309.5% **FASTER** than `Int2ObjectMaps#synchronize()` for a full map.

- `Int2ObjectSyncMap` **put()** then **get()** speed is...
  - 3221.0% **FASTER** than `Int2ObjectMaps#synchronize()` for an empty map.
  - 3342.1% **FASTER** than `Int2ObjectMaps#synchronize()` for a presized empty map.
  - 4000.0% **FASTER** than `Int2ObjectMaps#synchronize()` for a full map.

### Results

```txt
Benchmark                            (implementation)       (mode)  (prime)  (readPercentage)  (size)   Mode  Cnt  Score    Error   Units
Int2ObjectSyncMapBenchmark.get_only           SyncMap         none    false                50  100000  thrpt    5  1.680 ±  0.001  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap         none     true                50  100000  thrpt    5  1.680 ±  0.001  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap      presize    false                50  100000  thrpt    5  1.681 ±  0.003  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap      presize     true                50  100000  thrpt    5  1.679 ±  0.003  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap  prepopulate    false                50  100000  thrpt    5  1.752 ±  1.043  ops/ns
Int2ObjectSyncMapBenchmark.get_only           SyncMap  prepopulate     true                50  100000  thrpt    5  2.450 ±  0.029  ops/ns

Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap         none    false                50  100000  thrpt    5  0.029 ±  0.032  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap         none     true                50  100000  thrpt    5  0.034 ±  0.069  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap      presize    false                50  100000  thrpt    5  0.026 ±  0.007  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap      presize     true                50  100000  thrpt    5  0.036 ±  0.079  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.023 ±  0.023  ops/ns
Int2ObjectSyncMapBenchmark.get_only   SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.019 ±  0.016  ops/ns

Int2ObjectSyncMapBenchmark.get_put            SyncMap         none    false                50  100000  thrpt    5  0.621 ±  0.063  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap         none     true                50  100000  thrpt    5  0.631 ±  0.078  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap      presize    false                50  100000  thrpt    5  0.634 ±  0.081  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap      presize     true                50  100000  thrpt    5  0.654 ±  0.157  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap  prepopulate    false                50  100000  thrpt    5  0.638 ±  0.053  ops/ns
Int2ObjectSyncMapBenchmark.get_put            SyncMap  prepopulate     true                50  100000  thrpt    5  0.615 ±  0.039  ops/ns

Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap         none    false                50  100000  thrpt    5  0.019 ±  0.009  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap         none     true                50  100000  thrpt    5  0.019 ±  0.010  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap      presize    false                50  100000  thrpt    5  0.018 ±  0.014  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap      presize     true                50  100000  thrpt    5  0.019 ±  0.011  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.020 ±  0.011  ops/ns
Int2ObjectSyncMapBenchmark.get_put    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.015 ±  0.012  ops/ns

Int2ObjectSyncMapBenchmark.put_only           SyncMap         none    false                50  100000  thrpt    5  0.223 ±  0.082  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap         none     true                50  100000  thrpt    5  0.242 ±  0.035  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap      presize    false                50  100000  thrpt    5  0.239 ±  0.128  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap      presize     true                50  100000  thrpt    5  0.252 ±  0.115  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap  prepopulate    false                50  100000  thrpt    5  0.258 ±  0.046  ops/ns
Int2ObjectSyncMapBenchmark.put_only           SyncMap  prepopulate     true                50  100000  thrpt    5  0.506 ±  0.055  ops/ns

Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap         none    false                50  100000  thrpt    5  0.022 ±  0.001  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap         none     true                50  100000  thrpt    5  0.021 ±  0.001  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap      presize    false                50  100000  thrpt    5  0.023 ±  0.011  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap      presize     true                50  100000  thrpt    5  0.026 ±  0.025  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.022 ±  0.005  ops/ns
Int2ObjectSyncMapBenchmark.put_only   SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.021 ±  0.001  ops/ns
```
