# Benchmarks

## SyncMap

The following benchmarks are a comparison of `SyncMap` vs `Collections#synchronizedMap()` vs `ConcurrentHashMap`.

### Summary

The `SyncMap` has similar performance characteristics of the `ConcurrentHashMap`,
with an advantage in write speed.

The following results were recorded on an _M4 Macbook Pro_, using 8 threads and
100,000 entries for each benchmark:

- `SyncMap` **get()** speed is...
  - 4.1% **SLOWER** than `ConcurrentHashMap` for an empty map.
  - 3.9% **SLOWER** than `ConcurrentHashMap` for a presized empty map.
  - 6.7% **SLOWER** than `ConcurrentHashMap` for a full map.

- `SyncMap` **put()** speed is...
  - 44.8% **FASTER** than `ConcurrentHashMap` for an empty map.
  - 30.0% **SLOWER** than `ConcurrentHashMap` for a presized empty map.
  - 53.9% **FASTER** than `ConcurrentHashMap` for a full map.

- `SyncMap` **put()** then **get()** speed is...
  - 135.6% **FASTER** than `ConcurrentHashMap` for an empty map.
  - 66.1% **FASTER** than `ConcurrentHashMap` for a presized empty map.
  - 86.0% **FASTER** than `ConcurrentHashMap` for a full map.

- `SyncMap` random **put()** and **get()** speed is...
  - 39.1% **FASTER** than `ConcurrentHashMap` for an empty map.
  - 38.1% **FASTER** than `ConcurrentHashMap` for a presized empty map.
  - 37.0% **FASTER** than `ConcurrentHashMap` for a full map.

### Results

```txt
Benchmark                          (implementation)       (mode)   Mode  Cnt  Score   Error   Units
SyncMapBenchmark.getOnly          ConcurrentHashMap         none  thrpt    5  2.954 ± 0.016  ops/ns
SyncMapBenchmark.getOnly          ConcurrentHashMap      presize  thrpt    5  2.936 ± 0.005  ops/ns
SyncMapBenchmark.getOnly          ConcurrentHashMap  prepopulate  thrpt    5  2.658 ± 0.011  ops/ns

SyncMapBenchmark.getOnly                    SyncMap         none  thrpt    5  2.833 ± 0.018  ops/ns
SyncMapBenchmark.getOnly                    SyncMap      presize  thrpt    5  2.821 ± 0.004  ops/ns
SyncMapBenchmark.getOnly                    SyncMap  prepopulate  thrpt    5  2.479 ± 0.021  ops/ns

SyncMapBenchmark.getOnly            SynchronizedMap         none  thrpt    5  0.033 ± 0.045  ops/ns
SyncMapBenchmark.getOnly            SynchronizedMap      presize  thrpt    5  0.027 ± 0.004  ops/ns
SyncMapBenchmark.getOnly            SynchronizedMap  prepopulate  thrpt    5  0.035 ± 0.045  ops/ns

SyncMapBenchmark.putAndGet                  SyncMap         none  thrpt    5  0.391 ± 0.066  ops/ns
SyncMapBenchmark.putAndGet                  SyncMap      presize  thrpt    5  0.427 ± 0.039  ops/ns
SyncMapBenchmark.putAndGet                  SyncMap  prepopulate  thrpt    5  0.424 ± 0.020  ops/ns

SyncMapBenchmark.putAndGet        ConcurrentHashMap         none  thrpt    5  0.166 ± 0.113  ops/ns
SyncMapBenchmark.putAndGet        ConcurrentHashMap      presize  thrpt    5  0.257 ± 0.030  ops/ns
SyncMapBenchmark.putAndGet        ConcurrentHashMap  prepopulate  thrpt    5  0.228 ± 0.064  ops/ns

SyncMapBenchmark.putAndGet          SynchronizedMap         none  thrpt    5  0.012 ± 0.002  ops/ns
SyncMapBenchmark.putAndGet          SynchronizedMap      presize  thrpt    5  0.012 ± 0.003  ops/ns
SyncMapBenchmark.putAndGet          SynchronizedMap  prepopulate  thrpt    5  0.012 ± 0.007  ops/ns

SyncMapBenchmark.putOnly                    SyncMap         none  thrpt    5  0.239 ± 0.025  ops/ns
SyncMapBenchmark.putOnly                    SyncMap      presize  thrpt    5  0.235 ± 0.044  ops/ns
SyncMapBenchmark.putOnly                    SyncMap  prepopulate  thrpt    5  0.457 ± 0.026  ops/ns

SyncMapBenchmark.putOnly          ConcurrentHashMap         none  thrpt    5  0.165 ± 0.008  ops/ns
SyncMapBenchmark.putOnly          ConcurrentHashMap      presize  thrpt    5  0.336 ± 0.010  ops/ns
SyncMapBenchmark.putOnly          ConcurrentHashMap  prepopulate  thrpt    5  0.297 ± 0.043  ops/ns

SyncMapBenchmark.putOnly            SynchronizedMap         none  thrpt    5  0.023 ± 0.004  ops/ns
SyncMapBenchmark.putOnly            SynchronizedMap      presize  thrpt    5  0.023 ± 0.005  ops/ns
SyncMapBenchmark.putOnly            SynchronizedMap  prepopulate  thrpt    5  0.022 ± 0.003  ops/ns

SyncMapBenchmark.randomPutAndGet            SyncMap         none  thrpt    5  0.192 ± 0.009  ops/ns
SyncMapBenchmark.randomPutAndGet            SyncMap      presize  thrpt    5  0.203 ± 0.007  ops/ns
SyncMapBenchmark.randomPutAndGet            SyncMap  prepopulate  thrpt    5  0.200 ± 0.002  ops/ns

SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap         none  thrpt    5  0.138 ± 0.002  ops/ns
SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap      presize  thrpt    5  0.147 ± 0.001  ops/ns
SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap  prepopulate  thrpt    5  0.146 ± 0.003  ops/ns

SyncMapBenchmark.randomPutAndGet    SynchronizedMap         none  thrpt    5  0.017 ± 0.009  ops/ns
SyncMapBenchmark.randomPutAndGet    SynchronizedMap      presize  thrpt    5  0.016 ± 0.013  ops/ns
SyncMapBenchmark.randomPutAndGet    SynchronizedMap  prepopulate  thrpt    5  0.018 ± 0.004  ops/ns
```
