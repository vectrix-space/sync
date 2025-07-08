# Benchmarks

## SyncMap

The following benchmarks are a comparison of `SyncMap` vs `Collections#synchronizedMap()` vs `ConcurrentHashMap`.

```txt
Benchmark                          (implementation)       (mode)  (size)   Mode  Cnt   Score   Error   Units
SyncMapBenchmark.getOnly          ConcurrentHashMap         none  100000  thrpt    5  28.412 ± 0.114  ops/ms
SyncMapBenchmark.getOnly          ConcurrentHashMap      presize  100000  thrpt    5  28.349 ± 0.440  ops/ms
SyncMapBenchmark.getOnly          ConcurrentHashMap  prepopulate  100000  thrpt    5  22.157 ± 0.216  ops/ms

SyncMapBenchmark.getOnly                    SyncMap         none  100000  thrpt    5  26.269 ± 0.059  ops/ms
SyncMapBenchmark.getOnly                    SyncMap      presize  100000  thrpt    5  26.239 ± 0.112  ops/ms
SyncMapBenchmark.getOnly                    SyncMap  prepopulate  100000  thrpt    5  20.710 ± 0.596  ops/ms

SyncMapBenchmark.getOnly            SynchronizedMap         none  100000  thrpt    5   0.097 ± 0.117  ops/ms
SyncMapBenchmark.getOnly            SynchronizedMap      presize  100000  thrpt    5   0.235 ± 0.603  ops/ms
SyncMapBenchmark.getOnly            SynchronizedMap  prepopulate  100000  thrpt    5   0.290 ± 0.630  ops/ms

SyncMapBenchmark.putAndGet                  SyncMap         none  100000  thrpt    5   3.134 ± 0.091  ops/ms
SyncMapBenchmark.putAndGet                  SyncMap      presize  100000  thrpt    5   3.123 ± 0.108  ops/ms
SyncMapBenchmark.putAndGet                  SyncMap  prepopulate  100000  thrpt    5   2.947 ± 0.053  ops/ms

SyncMapBenchmark.putAndGet        ConcurrentHashMap         none  100000  thrpt    5   1.269 ± 0.007  ops/ms
SyncMapBenchmark.putAndGet        ConcurrentHashMap      presize  100000  thrpt    5   2.180 ± 0.141  ops/ms
SyncMapBenchmark.putAndGet        ConcurrentHashMap  prepopulate  100000  thrpt    5   1.999 ± 0.264  ops/ms

SyncMapBenchmark.putAndGet          SynchronizedMap         none  100000  thrpt    5   0.085 ± 0.254  ops/ms
SyncMapBenchmark.putAndGet          SynchronizedMap      presize  100000  thrpt    5   0.065 ± 0.169  ops/ms
SyncMapBenchmark.putAndGet          SynchronizedMap  prepopulate  100000  thrpt    5   0.068 ± 0.123  ops/ms

SyncMapBenchmark.putOnly                    SyncMap         none  100000  thrpt    5   1.682 ± 0.269  ops/ms
SyncMapBenchmark.putOnly                    SyncMap      presize  100000  thrpt    5   1.847 ± 0.738  ops/ms
SyncMapBenchmark.putOnly                    SyncMap  prepopulate  100000  thrpt    5   3.320 ± 0.115  ops/ms

SyncMapBenchmark.putOnly          ConcurrentHashMap         none  100000  thrpt    5   1.289 ± 0.050  ops/ms
SyncMapBenchmark.putOnly          ConcurrentHashMap      presize  100000  thrpt    5   2.174 ± 0.359  ops/ms
SyncMapBenchmark.putOnly          ConcurrentHashMap  prepopulate  100000  thrpt    5   2.342 ± 0.244  ops/ms

SyncMapBenchmark.putOnly            SynchronizedMap         none  100000  thrpt    5   0.154 ± 0.331  ops/ms
SyncMapBenchmark.putOnly            SynchronizedMap      presize  100000  thrpt    5   0.126 ± 0.414  ops/ms
SyncMapBenchmark.putOnly            SynchronizedMap  prepopulate  100000  thrpt    5   0.110 ± 0.382  ops/ms

SyncMapBenchmark.randomPutAndGet            SyncMap         none  100000  thrpt    5   1.484 ± 0.072  ops/ms
SyncMapBenchmark.randomPutAndGet            SyncMap      presize  100000  thrpt    5   1.517 ± 0.012  ops/ms
SyncMapBenchmark.randomPutAndGet            SyncMap  prepopulate  100000  thrpt    5   1.606 ± 0.007  ops/ms

SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap         none  100000  thrpt    5   1.149 ± 0.010  ops/ms
SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap      presize  100000  thrpt    5   1.215 ± 0.034  ops/ms
SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap  prepopulate  100000  thrpt    5   1.188 ± 0.023  ops/ms

SyncMapBenchmark.randomPutAndGet    SynchronizedMap         none  100000  thrpt    5   0.130 ± 0.023  ops/ms
SyncMapBenchmark.randomPutAndGet    SynchronizedMap      presize  100000  thrpt    5   0.137 ± 0.034  ops/ms
SyncMapBenchmark.randomPutAndGet    SynchronizedMap  prepopulate  100000  thrpt    5   0.112 ± 0.033  ops/ms
```
