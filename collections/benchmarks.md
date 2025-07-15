# Benchmarks

## SyncMap

The following benchmarks are a comparison of `SyncMap` vs `Collections#synchronizedMap()` vs `ConcurrentHashMap`.

```txt
Benchmark                          (implementation)       (mode)  (size)   Mode  Cnt   Score   Error   Units
SyncMapBenchmark.getOnly          ConcurrentHashMap         none  100000  thrpt    5  29.332 ± 0.053  ops/ms
SyncMapBenchmark.getOnly          ConcurrentHashMap      presize  100000  thrpt    5  29.289 ± 0.015  ops/ms
SyncMapBenchmark.getOnly          ConcurrentHashMap  prepopulate  100000  thrpt    5  26.329 ± 0.124  ops/ms

SyncMapBenchmark.getOnly                    SyncMap         none  100000  thrpt    5  28.158 ± 0.222  ops/ms
SyncMapBenchmark.getOnly                    SyncMap      presize  100000  thrpt    5  27.957 ± 0.252  ops/ms
SyncMapBenchmark.getOnly                    SyncMap  prepopulate  100000  thrpt    5  23.932 ± 0.333  ops/ms

SyncMapBenchmark.getOnly            SynchronizedMap         none  100000  thrpt    5   0.272 ± 0.012  ops/ms
SyncMapBenchmark.getOnly            SynchronizedMap      presize  100000  thrpt    5   0.275 ± 0.030  ops/ms
SyncMapBenchmark.getOnly            SynchronizedMap  prepopulate  100000  thrpt    5   0.294 ± 0.227  ops/ms

SyncMapBenchmark.putAndGet                  SyncMap         none  100000  thrpt    5   3.244 ± 0.057  ops/ms
SyncMapBenchmark.putAndGet                  SyncMap      presize  100000  thrpt    5   3.503 ± 0.281  ops/ms
SyncMapBenchmark.putAndGet                  SyncMap  prepopulate  100000  thrpt    5   3.512 ± 0.079  ops/ms

SyncMapBenchmark.putAndGet        ConcurrentHashMap         none  100000  thrpt    5   1.511 ± 0.024  ops/ms
SyncMapBenchmark.putAndGet        ConcurrentHashMap      presize  100000  thrpt    5   2.343 ± 0.336  ops/ms
SyncMapBenchmark.putAndGet        ConcurrentHashMap  prepopulate  100000  thrpt    5   2.438 ± 0.424  ops/ms

SyncMapBenchmark.putAndGet          SynchronizedMap         none  100000  thrpt    5   0.123 ± 0.022  ops/ms
SyncMapBenchmark.putAndGet          SynchronizedMap      presize  100000  thrpt    5   0.122 ± 0.014  ops/ms
SyncMapBenchmark.putAndGet          SynchronizedMap  prepopulate  100000  thrpt    5   0.137 ± 0.180  ops/ms

SyncMapBenchmark.putOnly                    SyncMap         none  100000  thrpt    5   2.023 ± 0.286  ops/ms
SyncMapBenchmark.putOnly                    SyncMap      presize  100000  thrpt    5   2.036 ± 0.602  ops/ms
SyncMapBenchmark.putOnly                    SyncMap  prepopulate  100000  thrpt    5   4.163 ± 0.137  ops/ms

SyncMapBenchmark.putOnly          ConcurrentHashMap         none  100000  thrpt    5   1.663 ± 0.088  ops/ms
SyncMapBenchmark.putOnly          ConcurrentHashMap      presize  100000  thrpt    5   3.116 ± 0.592  ops/ms
SyncMapBenchmark.putOnly          ConcurrentHashMap  prepopulate  100000  thrpt    5   3.060 ± 0.251  ops/ms

SyncMapBenchmark.putOnly            SynchronizedMap         none  100000  thrpt    5   0.277 ± 0.294  ops/ms
SyncMapBenchmark.putOnly            SynchronizedMap      presize  100000  thrpt    5   0.228 ± 0.036  ops/ms
SyncMapBenchmark.putOnly            SynchronizedMap  prepopulate  100000  thrpt    5   0.271 ± 0.253  ops/ms

SyncMapBenchmark.randomPutAndGet            SyncMap         none  100000  thrpt    5   1.560 ± 0.009  ops/ms
SyncMapBenchmark.randomPutAndGet            SyncMap      presize  100000  thrpt    5   1.514 ± 0.037  ops/ms
SyncMapBenchmark.randomPutAndGet            SyncMap  prepopulate  100000  thrpt    5   1.480 ± 0.034  ops/ms

SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap         none  100000  thrpt    5   1.278 ± 0.056  ops/ms
SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap      presize  100000  thrpt    5   1.430 ± 0.204  ops/ms
SyncMapBenchmark.randomPutAndGet  ConcurrentHashMap  prepopulate  100000  thrpt    5   1.437 ± 0.134  ops/ms

SyncMapBenchmark.randomPutAndGet    SynchronizedMap         none  100000  thrpt    5   0.176 ± 0.116  ops/ms
SyncMapBenchmark.randomPutAndGet    SynchronizedMap      presize  100000  thrpt    5   0.165 ± 0.142  ops/ms
SyncMapBenchmark.randomPutAndGet    SynchronizedMap  prepopulate  100000  thrpt    5   0.188 ± 0.006  ops/ms
```
