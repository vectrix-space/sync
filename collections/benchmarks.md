# Benchmarks

## SyncMap

The following benchmarks are a comparison of `SyncMap` vs `Collections#synchronizedMap()` vs `ConcurrentHashMap`.

```txt
Benchmark                          (implementation)       (mode)   Mode  Cnt  Score   Error   Units
SyncMapBenchmark.getOnly          ConcurrentHashMap         none  thrpt    5  2.950 ± 0.024  ops/ns
SyncMapBenchmark.getOnly          ConcurrentHashMap      presize  thrpt    5  2.949 ± 0.003  ops/ns
SyncMapBenchmark.getOnly          ConcurrentHashMap  prepopulate  thrpt    5  2.664 ± 0.033  ops/ns

SyncMapBenchmark.getOnly                    SyncMap         none  thrpt    5  2.840 ± 0.016  ops/ns
SyncMapBenchmark.getOnly                    SyncMap      presize  thrpt    5  2.827 ± 0.004  ops/ns
SyncMapBenchmark.getOnly                    SyncMap  prepopulate  thrpt    5  2.481 ± 0.041  ops/ns

SyncMapBenchmark.getOnly            SynchronizedMap         none  thrpt    5  0.027 ± 0.003  ops/ns
SyncMapBenchmark.getOnly            SynchronizedMap      presize  thrpt    5  0.028 ± 0.001  ops/ns
SyncMapBenchmark.getOnly            SynchronizedMap  prepopulate  thrpt    5  0.034 ± 0.049  ops/ns

SyncMapBenchmark.putAndGet                  SyncMap         none  thrpt    5  0.426 ± 0.029  ops/ns
SyncMapBenchmark.putAndGet                  SyncMap      presize  thrpt    5  0.440 ± 0.075  ops/ns
SyncMapBenchmark.putAndGet                  SyncMap  prepopulate  thrpt    5  0.428 ± 0.077  ops/ns

SyncMapBenchmark.putAndGet        ConcurrentHashMap         none  thrpt    5  0.156 ± 0.006  ops/ns
SyncMapBenchmark.putAndGet        ConcurrentHashMap      presize  thrpt    5  0.231 ± 0.058  ops/ns
SyncMapBenchmark.putAndGet        ConcurrentHashMap  prepopulate  thrpt    5  0.239 ± 0.021  ops/ns

SyncMapBenchmark.putAndGet          SynchronizedMap         none  thrpt    5  0.013 ± 0.008  ops/ns
SyncMapBenchmark.putAndGet          SynchronizedMap      presize  thrpt    5  0.012 ± 0.002  ops/ns
SyncMapBenchmark.putAndGet          SynchronizedMap  prepopulate  thrpt    5  0.012 ± 0.003  ops/ns

SyncMapBenchmark.putOnly                    SyncMap         none  thrpt    5  0.202 ± 0.027  ops/ns
SyncMapBenchmark.putOnly                    SyncMap      presize  thrpt    5  0.249 ± 0.047  ops/ns
SyncMapBenchmark.putOnly                    SyncMap  prepopulate  thrpt    5  0.437 ± 0.014  ops/ns

SyncMapBenchmark.putOnly          ConcurrentHashMap         none  thrpt    5  0.164 ± 0.003  ops/ns
SyncMapBenchmark.putOnly          ConcurrentHashMap      presize  thrpt    5  0.324 ± 0.040  ops/ns
SyncMapBenchmark.putOnly          ConcurrentHashMap  prepopulate  thrpt    5  0.264 ± 0.029  ops/ns

SyncMapBenchmark.putOnly            SynchronizedMap         none  thrpt    5  0.023 ± 0.004  ops/ns
SyncMapBenchmark.putOnly            SynchronizedMap      presize  thrpt    5  0.026 ± 0.027  ops/ns
SyncMapBenchmark.putOnly            SynchronizedMap  prepopulate  thrpt    5  0.027 ± 0.029  ops/ns

SyncMapBenchmark.randomPutAndGet            SyncMap         none  thrpt    5  0.193 ± 0.002  ops/ns
SyncMapBenchmark.randomPutAndGet            SyncMap      presize  thrpt    5  0.205 ± 0.010  ops/ns
SyncMapBenchmark.randomPutAndGet            SyncMap  prepopulate  thrpt    5  0.202 ± 0.001  ops/ns

SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap         none  thrpt    5  0.138 ± 0.003  ops/ns
SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap      presize  thrpt    5  0.146 ± 0.003  ops/ns
SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap  prepopulate  thrpt    5  0.145 ± 0.002  ops/ns

SyncMapBenchmark.randomPutAndGet    SynchronizedMap         none  thrpt    5  0.018 ± 0.003  ops/ns
SyncMapBenchmark.randomPutAndGet    SynchronizedMap      presize  thrpt    5  0.019 ± 0.009  ops/ns
SyncMapBenchmark.randomPutAndGet    SynchronizedMap  prepopulate  thrpt    5  0.017 ± 0.012  ops/ns
```
