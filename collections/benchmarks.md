# Benchmarks

## SyncMap

The following benchmarks are a comparison of `SyncMap` vs `Collections#synchronizedMap()` vs `ConcurrentHashMap`.

### Summary

The `SyncMap` has similar performance characteristics of the `ConcurrentHashMap`,
with an advantage in write speed.

The following results were recorded on an _M4 Macbook Pro_, using 8 threads and
100,000 entries for each benchmark:

- `SyncMap` **get()** speed is...
  - 47.8% **FASTER** than `ConcurrentHashMap` for an empty map.
  - 48.1% **FASTER** than `ConcurrentHashMap` for a presized empty map.
  - 21.0% **FASTER** than `ConcurrentHashMap` for a full map.

- `SyncMap` **put()** speed is...
  - 23.8% **FASTER** than `ConcurrentHashMap` for an empty map.
  - 14.5% **SLOWER** than `ConcurrentHashMap` for a presized empty map.
  - 32.0% **FASTER** than `ConcurrentHashMap` for a full map.

- `SyncMap` **put()** then **get()** speed is...
  - 109.7% **FASTER** than `ConcurrentHashMap` for an empty map.
  - 70.4% **FASTER** than `ConcurrentHashMap` for a presized empty map.
  - 55.1% **FASTER** than `ConcurrentHashMap` for a full map.

### Results

```txt
Benchmark                   (implementation)       (mode)  (prime)  (readPercentage)  (size)   Mode  Cnt  Score   Error   Units
SyncMapBenchmark.get_only            SyncMap         none    false                50  100000  thrpt    5  2.411 ± 0.004  ops/ns
SyncMapBenchmark.get_only            SyncMap         none     true                50  100000  thrpt    5  2.405 ± 0.006  ops/ns
SyncMapBenchmark.get_only            SyncMap      presize    false                50  100000  thrpt    5  2.416 ± 0.006  ops/ns
SyncMapBenchmark.get_only            SyncMap      presize     true                50  100000  thrpt    5  2.398 ± 0.003  ops/ns
SyncMapBenchmark.get_only            SyncMap  prepopulate    false                50  100000  thrpt    5  1.928 ± 0.024  ops/ns
SyncMapBenchmark.get_only            SyncMap  prepopulate     true                50  100000  thrpt    5  1.948 ± 0.035  ops/ns

SyncMapBenchmark.get_only  ConcurrentHashMap         none    false                50  100000  thrpt    5  1.631 ± 0.002  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap         none     true                50  100000  thrpt    5  2.418 ± 0.002  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap      presize    false                50  100000  thrpt    5  1.631 ± 0.004  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap      presize     true                50  100000  thrpt    5  2.414 ± 0.001  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  1.593 ± 0.004  ops/ns
SyncMapBenchmark.get_only  ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  2.372 ± 0.049  ops/ns

SyncMapBenchmark.get_only    SynchronizedMap         none    false                50  100000  thrpt    5  0.026 ± 0.005  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap         none     true                50  100000  thrpt    5  0.027 ± 0.006  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap      presize    false                50  100000  thrpt    5  0.034 ± 0.044  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap      presize     true                50  100000  thrpt    5  0.031 ± 0.043  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.026 ± 0.007  ops/ns
SyncMapBenchmark.get_only    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.026 ± 0.006  ops/ns

SyncMapBenchmark.get_put             SyncMap         none    false                50  100000  thrpt    5  0.534 ± 0.053  ops/ns
SyncMapBenchmark.get_put             SyncMap         none     true                50  100000  thrpt    5  0.539 ± 0.064  ops/ns
SyncMapBenchmark.get_put             SyncMap      presize    false                50  100000  thrpt    5  0.532 ± 0.042  ops/ns
SyncMapBenchmark.get_put             SyncMap      presize     true                50  100000  thrpt    5  0.542 ± 0.041  ops/ns
SyncMapBenchmark.get_put             SyncMap  prepopulate    false                50  100000  thrpt    5  0.526 ± 0.047  ops/ns
SyncMapBenchmark.get_put             SyncMap  prepopulate     true                50  100000  thrpt    5  0.517 ± 0.025  ops/ns

SyncMapBenchmark.get_put   ConcurrentHashMap         none    false                50  100000  thrpt    5  0.241 ± 0.002  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap         none     true                50  100000  thrpt    5  0.257 ± 0.006  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap      presize    false                50  100000  thrpt    5  0.329 ± 0.040  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap      presize     true                50  100000  thrpt    5  0.318 ± 0.006  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  0.339 ± 0.095  ops/ns
SyncMapBenchmark.get_put   ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  0.349 ± 0.022  ops/ns

SyncMapBenchmark.get_put     SynchronizedMap         none    false                50  100000  thrpt    5  0.020 ± 0.002  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap         none     true                50  100000  thrpt    5  0.020 ± 0.009  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap      presize    false                50  100000  thrpt    5  0.021 ± 0.003  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap      presize     true                50  100000  thrpt    5  0.018 ± 0.013  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.019 ± 0.012  ops/ns
SyncMapBenchmark.get_put     SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.019 ± 0.014  ops/ns

SyncMapBenchmark.put_only            SyncMap         none    false                50  100000  thrpt    5  0.255 ± 0.027  ops/ns
SyncMapBenchmark.put_only            SyncMap         none     true                50  100000  thrpt    5  0.242 ± 0.014  ops/ns
SyncMapBenchmark.put_only            SyncMap      presize    false                50  100000  thrpt    5  0.213 ± 0.017  ops/ns
SyncMapBenchmark.put_only            SyncMap      presize     true                50  100000  thrpt    5  0.265 ± 0.012  ops/ns
SyncMapBenchmark.put_only            SyncMap  prepopulate    false                50  100000  thrpt    5  0.239 ± 0.015  ops/ns
SyncMapBenchmark.put_only            SyncMap  prepopulate     true                50  100000  thrpt    5  0.408 ± 0.053  ops/ns

SyncMapBenchmark.put_only  ConcurrentHashMap         none    false                50  100000  thrpt    5  0.206 ± 0.219  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap         none     true                50  100000  thrpt    5  0.160 ± 0.025  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap      presize    false                50  100000  thrpt    5  0.273 ± 0.029  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap      presize     true                50  100000  thrpt    5  0.310 ± 0.047  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap  prepopulate    false                50  100000  thrpt    5  0.313 ± 0.022  ops/ns
SyncMapBenchmark.put_only  ConcurrentHashMap  prepopulate     true                50  100000  thrpt    5  0.309 ± 0.027  ops/ns

SyncMapBenchmark.put_only    SynchronizedMap         none    false                50  100000  thrpt    5  0.032 ± 0.040  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap         none     true                50  100000  thrpt    5  0.023 ± 0.016  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap      presize    false                50  100000  thrpt    5  0.025 ± 0.019  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap      presize     true                50  100000  thrpt    5  0.026 ± 0.025  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap  prepopulate    false                50  100000  thrpt    5  0.021 ± 0.001  ops/ns
SyncMapBenchmark.put_only    SynchronizedMap  prepopulate     true                50  100000  thrpt    5  0.022 ± 0.004  ops/ns
```
