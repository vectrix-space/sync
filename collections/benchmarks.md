# Benchmarks

## SyncMap

The following benchmarks are a comparison of `SyncMap` vs `Collections#synchronizedMap()` vs `ConcurrentHashMap`.

```txt
Benchmark                          (implementation)       (mode)  (size)   Mode  Cnt   Score   Error   Units
SyncMapBenchmark.getOnly          ConcurrentHashMap         none  100000  thrpt    5  29.402 ± 0.488  ops/ms
SyncMapBenchmark.getOnly          ConcurrentHashMap      presize  100000  thrpt    5  29.340 ± 0.010  ops/ms
SyncMapBenchmark.getOnly          ConcurrentHashMap  prepopulate  100000  thrpt    5  26.309 ± 0.219  ops/ms

SyncMapBenchmark.getOnly                    SyncMap         none  100000  thrpt    5  28.216 ± 0.112  ops/ms
SyncMapBenchmark.getOnly                    SyncMap      presize  100000  thrpt    5  28.045 ± 0.119  ops/ms
SyncMapBenchmark.getOnly                    SyncMap  prepopulate  100000  thrpt    5  23.847 ± 0.321  ops/ms

SyncMapBenchmark.getOnly            SynchronizedMap         none  100000  thrpt    5   0.291 ± 0.096  ops/ms
SyncMapBenchmark.getOnly            SynchronizedMap      presize  100000  thrpt    5   0.291 ± 0.068  ops/ms
SyncMapBenchmark.getOnly            SynchronizedMap  prepopulate  100000  thrpt    5   0.323 ± 0.603  ops/ms

SyncMapBenchmark.putAndGet                  SyncMap         none  100000  thrpt    5   3.523 ± 0.170  ops/ms
SyncMapBenchmark.putAndGet                  SyncMap      presize  100000  thrpt    5   3.381 ± 0.074  ops/ms
SyncMapBenchmark.putAndGet                  SyncMap  prepopulate  100000  thrpt    5   3.543 ± 0.071  ops/ms

SyncMapBenchmark.putAndGet        ConcurrentHashMap         none  100000  thrpt    5   1.572 ± 0.112  ops/ms
SyncMapBenchmark.putAndGet        ConcurrentHashMap      presize  100000  thrpt    5   2.300 ± 0.287  ops/ms
SyncMapBenchmark.putAndGet        ConcurrentHashMap  prepopulate  100000  thrpt    5   2.303 ± 0.582  ops/ms

SyncMapBenchmark.putAndGet          SynchronizedMap         none  100000  thrpt    5   0.133 ± 0.093  ops/ms
SyncMapBenchmark.putAndGet          SynchronizedMap      presize  100000  thrpt    5   0.126 ± 0.023  ops/ms
SyncMapBenchmark.putAndGet          SynchronizedMap  prepopulate  100000  thrpt    5   0.140 ± 0.189  ops/ms

SyncMapBenchmark.putOnly                    SyncMap         none  100000  thrpt    5   1.873 ± 0.477  ops/ms
SyncMapBenchmark.putOnly                    SyncMap      presize  100000  thrpt    5   1.796 ± 0.388  ops/ms
SyncMapBenchmark.putOnly                    SyncMap  prepopulate  100000  thrpt    5   3.511 ± 0.138  ops/ms

SyncMapBenchmark.putOnly          ConcurrentHashMap         none  100000  thrpt    5   1.630 ± 0.106  ops/ms
SyncMapBenchmark.putOnly          ConcurrentHashMap      presize  100000  thrpt    5   3.255 ± 0.218  ops/ms
SyncMapBenchmark.putOnly          ConcurrentHashMap  prepopulate  100000  thrpt    5   2.664 ± 0.356  ops/ms

SyncMapBenchmark.putOnly            SynchronizedMap         none  100000  thrpt    5   0.226 ± 0.016  ops/ms
SyncMapBenchmark.putOnly            SynchronizedMap      presize  100000  thrpt    5   0.262 ± 0.312  ops/ms
SyncMapBenchmark.putOnly            SynchronizedMap  prepopulate  100000  thrpt    5   0.260 ± 0.241  ops/ms

SyncMapBenchmark.randomPutAndGet            SyncMap         none  100000  thrpt    5   1.540 ± 0.023  ops/ms
SyncMapBenchmark.randomPutAndGet            SyncMap      presize  100000  thrpt    5   1.622 ± 0.030  ops/ms
SyncMapBenchmark.randomPutAndGet            SyncMap  prepopulate  100000  thrpt    5   1.620 ± 0.019  ops/ms

SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap         none  100000  thrpt    5   1.375 ± 0.017  ops/ms
SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap      presize  100000  thrpt    5   1.465 ± 0.028  ops/ms
SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap  prepopulate  100000  thrpt    5   1.431 ± 0.138  ops/ms

SyncMapBenchmark.randomPutAndGet    SynchronizedMap         none  100000  thrpt    5   0.178 ± 0.098  ops/ms
SyncMapBenchmark.randomPutAndGet    SynchronizedMap      presize  100000  thrpt    5   0.174 ± 0.124  ops/ms
SyncMapBenchmark.randomPutAndGet    SynchronizedMap  prepopulate  100000  thrpt    5   0.172 ± 0.120  ops/ms
```
