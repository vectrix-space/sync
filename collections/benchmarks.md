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
SyncMapBenchmark.get_only            SyncMap         none    false                50  100000  thrpt    5  2.381 ± 0.062  ops/ns
SyncMapBenchmark.get_only            SyncMap         none     true                50  100000  thrpt    5  2.383 ± 0.010  ops/ns
SyncMapBenchmark.get_only            SyncMap      presize    false                50  100000  thrpt    5  2.391 ± 0.008  ops/ns
SyncMapBenchmark.get_only            SyncMap      presize     true                50  100000  thrpt    5  2.392 ± 0.006  ops/ns
SyncMapBenchmark.get_only            SyncMap  prepopulate    false                50  100000  thrpt    5  1.948 ± 0.017  ops/ns
SyncMapBenchmark.get_only            SyncMap  prepopulate     true                50  100000  thrpt    5  1.981 ± 0.035  ops/ns

SyncMapBenchmark.get_only  ConcurrentHashMap         none    false                50  100000  thrpt    5  1.628 ± 0.002  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap         none     true                50  100000  thrpt    5  2.417 ± 0.003  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap      presize    false                50  100000  thrpt    5  1.632 ± 0.001  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap      presize     true                50  100000  thrpt    5  2.405 ± 0.010  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  1.596 ± 0.002  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  2.377 ± 0.033  ops/ns

SyncMapBenchmark.get_only    SynchronizedMap         none    false                50  100000  thrpt    5  0.031 ± 0.052  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap         none     true                50  100000  thrpt    5  0.025 ± 0.001  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap      presize    false                50  100000  thrpt    5  0.025 ± 0.002  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap      presize     true                50  100000  thrpt    5  0.040 ± 0.064  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.033 ± 0.051  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.025 ± 0.005  ops/ns

SyncMapBenchmark.get_put             SyncMap         none    false                50  100000  thrpt    5  0.545 ± 0.096  ops/ns
SyncMapBenchmark.get_put             SyncMap         none     true                50  100000  thrpt    5  0.537 ± 0.051  ops/ns
SyncMapBenchmark.get_put             SyncMap      presize    false                50  100000  thrpt    5  0.552 ± 0.086  ops/ns
SyncMapBenchmark.get_put             SyncMap      presize     true                50  100000  thrpt    5  0.544 ± 0.033  ops/ns
SyncMapBenchmark.get_put             SyncMap  prepopulate    false                50  100000  thrpt    5  0.570 ± 0.110  ops/ns
SyncMapBenchmark.get_put             SyncMap  prepopulate     true                50  100000  thrpt    5  0.528 ± 0.046  ops/ns

SyncMapBenchmark.get_put   ConcurrentHashMap         none    false                50  100000  thrpt    5  0.249 ± 0.005  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap         none     true                50  100000  thrpt    5  0.261 ± 0.009  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap      presize    false                50  100000  thrpt    5  0.317 ± 0.012  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap      presize     true                50  100000  thrpt    5  0.327 ± 0.036  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  0.337 ± 0.024  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  0.360 ± 0.071  ops/ns

SyncMapBenchmark.get_put     SynchronizedMap         none    false                50  100000  thrpt    5  0.019 ± 0.012  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap         none     true                50  100000  thrpt    5  0.019 ± 0.012  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap      presize    false                50  100000  thrpt    5  0.020 ± 0.012  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap      presize     true                50  100000  thrpt    5  0.019 ± 0.012  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.018 ± 0.012  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.019 ± 0.011  ops/ns

SyncMapBenchmark.put_only            SyncMap         none    false                50  100000  thrpt    5  0.242 ± 0.056  ops/ns
SyncMapBenchmark.put_only            SyncMap         none     true                50  100000  thrpt    5  0.244 ± 0.024  ops/ns
SyncMapBenchmark.put_only            SyncMap      presize    false                50  100000  thrpt    5  0.273 ± 0.029  ops/ns
SyncMapBenchmark.put_only            SyncMap      presize     true                50  100000  thrpt    5  0.263 ± 0.044  ops/ns
SyncMapBenchmark.put_only            SyncMap  prepopulate    false                50  100000  thrpt    5  0.252 ± 0.037  ops/ns
SyncMapBenchmark.put_only            SyncMap  prepopulate     true                50  100000  thrpt    5  0.391 ± 0.057  ops/ns

SyncMapBenchmark.put_only  ConcurrentHashMap         none    false                50  100000  thrpt    5  0.168 ± 0.007  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap         none     true                50  100000  thrpt    5  0.151 ± 0.014  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap      presize    false                50  100000  thrpt    5  0.268 ± 0.033  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap      presize     true                50  100000  thrpt    5  0.278 ± 0.043  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  0.318 ± 0.018  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  0.321 ± 0.038  ops/ns

SyncMapBenchmark.put_only    SynchronizedMap         none    false                50  100000  thrpt    5  0.024 ± 0.015  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap         none     true                50  100000  thrpt    5  0.022 ± 0.006  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap      presize    false                50  100000  thrpt    5  0.023 ± 0.017  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap      presize     true                50  100000  thrpt    5  0.025 ± 0.024  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.023 ± 0.007  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.023 ± 0.009  ops/ns
```
