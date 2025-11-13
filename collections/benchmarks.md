# Benchmarks

## SyncMap

The following benchmarks are a comparison of `SyncMap` vs `Collections#synchronizedMap()` vs `ConcurrentHashMap`.

### Summary

The `SyncMap` has similar performance characteristics of the `ConcurrentHashMap`,
with an advantage in write speed.

The following results were recorded on an _M4 Macbook Pro_, using 8 threads and
100,000 entries for each benchmark:

- `SyncMap` **get()** speed is...
  - 46.4% **FASTER** than `ConcurrentHashMap` for an empty map.
  - 46.6% **FASTER** than `ConcurrentHashMap` for a presized empty map.
  - 24.1% **FASTER** than `ConcurrentHashMap` for a full map.

- `SyncMap` **put()** speed is...
  - 61.6% **FASTER** than `ConcurrentHashMap` for an empty map.
  - 1.9% **FASTER** than `ConcurrentHashMap` for a presized empty map.
  - 22.9% **FASTER** than `ConcurrentHashMap` for a full map.

- `SyncMap` **put()** then **get()** speed is...
  - 118.9% **FASTER** than `ConcurrentHashMap` for an empty map.
  - 74.1% **FASTER** than `ConcurrentHashMap` for a presized empty map.
  - 69.1% **FASTER** than `ConcurrentHashMap` for a full map.

### Results

```txt
Benchmark                   (implementation)       (mode)  (prime)  (readPercentage)  (size)   Mode  Cnt  Score   Error   Units
SyncMapBenchmark.get_only            SyncMap         none    false                50  100000  thrpt    5  2.368 ± 0.083  ops/ns
SyncMapBenchmark.get_only            SyncMap         none     true                50  100000  thrpt    5  2.356 ± 0.009  ops/ns
SyncMapBenchmark.get_only            SyncMap      presize    false                50  100000  thrpt    5  2.179 ± 0.009  ops/ns
SyncMapBenchmark.get_only            SyncMap      presize     true                50  100000  thrpt    5  2.347 ± 0.030  ops/ns
SyncMapBenchmark.get_only            SyncMap  prepopulate    false                50  100000  thrpt    5  1.853 ± 0.017  ops/ns
SyncMapBenchmark.get_only            SyncMap  prepopulate     true                50  100000  thrpt    5  1.948 ± 0.022  ops/ns

SyncMapBenchmark.get_only  ConcurrentHashMap         none    false                50  100000  thrpt    5  1.631 ± 0.002  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap         none     true                50  100000  thrpt    5  2.416 ± 0.001  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap      presize    false                50  100000  thrpt    5  1.630 ± 0.003  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap      presize     true                50  100000  thrpt    5  2.407 ± 0.004  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  1.595 ± 0.003  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  2.376 ± 0.024  ops/ns

SyncMapBenchmark.get_only    SynchronizedMap         none    false                50  100000  thrpt    5  0.030 ± 0.034  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap         none     true                50  100000  thrpt    5  0.026 ± 0.009  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap      presize    false                50  100000  thrpt    5  0.025 ± 0.005  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap      presize     true                50  100000  thrpt    5  0.026 ± 0.008  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.032 ± 0.050  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.027 ± 0.016  ops/ns

SyncMapBenchmark.get_put             SyncMap         none    false                50  100000  thrpt    5  0.545 ± 0.015  ops/ns
SyncMapBenchmark.get_put             SyncMap         none     true                50  100000  thrpt    5  0.539 ± 0.032  ops/ns
SyncMapBenchmark.get_put             SyncMap      presize    false                50  100000  thrpt    5  0.558 ± 0.126  ops/ns
SyncMapBenchmark.get_put             SyncMap      presize     true                50  100000  thrpt    5  0.544 ± 0.014  ops/ns
SyncMapBenchmark.get_put             SyncMap  prepopulate    false                50  100000  thrpt    5  0.541 ± 0.027  ops/ns
SyncMapBenchmark.get_put             SyncMap  prepopulate     true                50  100000  thrpt    5  0.535 ± 0.037  ops/ns

SyncMapBenchmark.get_put   ConcurrentHashMap         none    false                50  100000  thrpt    5  0.249 ± 0.004  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap         none     true                50  100000  thrpt    5  0.257 ± 0.008  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap      presize    false                50  100000  thrpt    5  0.344 ± 0.040  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap      presize     true                50  100000  thrpt    5  0.319 ± 0.023  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  0.374 ± 0.032  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  0.382 ± 0.100  ops/ns

SyncMapBenchmark.get_put     SynchronizedMap         none    false                50  100000  thrpt    5  0.021 ± 0.002  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap         none     true                50  100000  thrpt    5  0.018 ± 0.011  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap      presize    false                50  100000  thrpt    5  0.020 ± 0.008  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap      presize     true                50  100000  thrpt    5  0.021 ± 0.003  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.017 ± 0.015  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.018 ± 0.015  ops/ns

SyncMapBenchmark.put_only            SyncMap         none    false                50  100000  thrpt    5  0.268 ± 0.024  ops/ns
SyncMapBenchmark.put_only            SyncMap         none     true                50  100000  thrpt    5  0.255 ± 0.020  ops/ns
SyncMapBenchmark.put_only            SyncMap      presize    false                50  100000  thrpt    5  0.259 ± 0.028  ops/ns
SyncMapBenchmark.put_only            SyncMap      presize     true                50  100000  thrpt    5  0.249 ± 0.030  ops/ns
SyncMapBenchmark.put_only            SyncMap  prepopulate    false                50  100000  thrpt    5  0.262 ± 0.065  ops/ns
SyncMapBenchmark.put_only            SyncMap  prepopulate     true                50  100000  thrpt    5  0.397 ± 0.056  ops/ns

SyncMapBenchmark.put_only  ConcurrentHashMap         none    false                50  100000  thrpt    5  0.203 ± 0.190  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap         none     true                50  100000  thrpt    5  0.204 ± 0.199  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap      presize    false                50  100000  thrpt    5  0.264 ± 0.047  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap      presize     true                50  100000  thrpt    5  0.281 ± 0.046  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  0.327 ± 0.037  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  0.284 ± 0.020  ops/ns

SyncMapBenchmark.put_only    SynchronizedMap         none    false                50  100000  thrpt    5  0.028 ± 0.040  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap         none     true                50  100000  thrpt    5  0.024 ± 0.032  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap      presize    false                50  100000  thrpt    5  0.021 ± 0.003  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap      presize     true                50  100000  thrpt    5  0.023 ± 0.018  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.020 ± 0.006  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.024 ± 0.018  ops/ns
```
