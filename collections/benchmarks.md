# Benchmarks

## SyncMap

The following benchmarks are a comparison of `SyncMap` vs `Collections#synchronizedMap()` vs `ConcurrentHashMap`.

### Summary

The `SyncMap` has similar performance characteristics of the `ConcurrentHashMap`,
with an advantage in write speed.

The following results were recorded on an _M4 Macbook Pro_, using 8 threads and
100,000 entries for each benchmark:

- `SyncMap` **get()** speed is...
  - 47.7% **FASTER** than `ConcurrentHashMap` for an empty map.
  - 48.5% **FASTER** than `ConcurrentHashMap` for a presized empty map.
  - 18.9% **SLOWER** than `ConcurrentHashMap` for a full map.

- `SyncMap` **put()** speed is...
  - 16.3% **FASTER** than `ConcurrentHashMap` for an empty map.
  - 19.6% **SLOWER** than `ConcurrentHashMap` for a presized empty map.
  - 37.8% **FASTER** than `ConcurrentHashMap` for a full map.

- `SyncMap` **put()** then **get()** speed is...
  - 127.0% **FASTER** than `ConcurrentHashMap` for an empty map.
  - 68.6% **FASTER** than `ConcurrentHashMap` for a presized empty map.
  - 56.7% **FASTER** than `ConcurrentHashMap` for a full map.

### Results

```txt
Benchmark                   (implementation)       (mode)  (prime)  (readPercentage)  (size)   Mode  Cnt  Score   Error   Units
SyncMapBenchmark.get_only            SyncMap         none    false                50  100000  thrpt    5  2.408 ± 0.029  ops/ns
SyncMapBenchmark.get_only            SyncMap         none     true                50  100000  thrpt    5  2.410 ± 0.004  ops/ns
SyncMapBenchmark.get_only            SyncMap      presize    false                50  100000  thrpt    5  2.413 ± 0.006  ops/ns
SyncMapBenchmark.get_only            SyncMap      presize     true                50  100000  thrpt    5  2.405 ± 0.014  ops/ns
SyncMapBenchmark.get_only            SyncMap  prepopulate    false                50  100000  thrpt    5  1.922 ± 0.014  ops/ns
SyncMapBenchmark.get_only            SyncMap  prepopulate     true                50  100000  thrpt    5  1.932 ± 0.030  ops/ns

SyncMapBenchmark.get_only  ConcurrentHashMap         none    false                50  100000  thrpt    5  1.630 ± 0.002  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap         none     true                50  100000  thrpt    5  2.403 ± 0.017  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap      presize    false                50  100000  thrpt    5  1.625 ± 0.002  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap      presize     true                50  100000  thrpt    5  2.408 ± 0.001  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  2.379 ± 0.020  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  2.382 ± 0.012  ops/ns

SyncMapBenchmark.get_only    SynchronizedMap         none    false                50  100000  thrpt    5  0.025 ± 0.002  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap         none     true                50  100000  thrpt    5  0.025 ± 0.003  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap      presize    false                50  100000  thrpt    5  0.026 ± 0.005  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap      presize     true                50  100000  thrpt    5  0.032 ± 0.052  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.030 ± 0.045  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.024 ± 0.001  ops/ns

SyncMapBenchmark.get_put             SyncMap         none    false                50  100000  thrpt    5  0.527 ± 0.031  ops/ns
SyncMapBenchmark.get_put             SyncMap         none     true                50  100000  thrpt    5  0.547 ± 0.071  ops/ns
SyncMapBenchmark.get_put             SyncMap      presize    false                50  100000  thrpt    5  0.570 ± 0.089  ops/ns
SyncMapBenchmark.get_put             SyncMap      presize     true                50  100000  thrpt    5  0.539 ± 0.120  ops/ns
SyncMapBenchmark.get_put             SyncMap  prepopulate    false                50  100000  thrpt    5  0.520 ± 0.019  ops/ns
SyncMapBenchmark.get_put             SyncMap  prepopulate     true                50  100000  thrpt    5  0.528 ± 0.043  ops/ns

SyncMapBenchmark.get_put   ConcurrentHashMap         none    false                50  100000  thrpt    5  0.253 ± 0.096  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap         none     true                50  100000  thrpt    5  0.241 ± 0.005  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap      presize    false                50  100000  thrpt    5  0.338 ± 0.030  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap      presize     true                50  100000  thrpt    5  0.320 ± 0.017  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  0.357 ± 0.044  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  0.337 ± 0.015  ops/ns

SyncMapBenchmark.get_put     SynchronizedMap         none    false                50  100000  thrpt    5  0.018 ± 0.012  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap         none     true                50  100000  thrpt    5  0.018 ± 0.015  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap      presize    false                50  100000  thrpt    5  0.018 ± 0.012  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap      presize     true                50  100000  thrpt    5  0.019 ± 0.013  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.019 ± 0.013  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.019 ± 0.010  ops/ns

SyncMapBenchmark.put_only            SyncMap         none    false                50  100000  thrpt    5  0.222 ± 0.027  ops/ns
SyncMapBenchmark.put_only            SyncMap         none     true                50  100000  thrpt    5  0.235 ± 0.041  ops/ns
SyncMapBenchmark.put_only            SyncMap      presize    false                50  100000  thrpt    5  0.238 ± 0.012  ops/ns
SyncMapBenchmark.put_only            SyncMap      presize     true                50  100000  thrpt    5  0.230 ± 0.019  ops/ns
SyncMapBenchmark.put_only            SyncMap  prepopulate    false                50  100000  thrpt    5  0.232 ± 0.052  ops/ns
SyncMapBenchmark.put_only            SyncMap  prepopulate     true                50  100000  thrpt    5  0.412 ± 0.048  ops/ns

SyncMapBenchmark.put_only  ConcurrentHashMap         none    false                50  100000  thrpt    5  0.237 ± 0.257  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap         none     true                50  100000  thrpt    5  0.202 ± 0.171  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap      presize    false                50  100000  thrpt    5  0.300 ± 0.029  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap      presize     true                50  100000  thrpt    5  0.286 ± 0.040  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  0.309 ± 0.044  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  0.299 ± 0.029  ops/ns

SyncMapBenchmark.put_only    SynchronizedMap         none    false                50  100000  thrpt    5  0.024 ± 0.026  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap         none     true                50  100000  thrpt    5  0.029 ± 0.042  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap      presize    false                50  100000  thrpt    5  0.021 ± 0.006  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap      presize     true                50  100000  thrpt    5  0.021 ± 0.005  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.026 ± 0.032  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.027 ± 0.032  ops/ns
```
