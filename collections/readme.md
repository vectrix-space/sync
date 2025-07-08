# Sync Collections

Provides concurrent thread-safe collections for highly concurrent scenarios in Java.

* **SyncMap**: A high-performance implementation of `ConcurrentMap` in Java.

  * Fully compatible with the Java Collections Framework (`ConcurrentMap`).
  * Delivers up to **2× higher update throughput** than `ConcurrentHashMap` under heavy contention, while matching its performance for reads and other operations.

## Dependency

Sync is available at the Maven Central Repository.

**Gradle**

```kotlin
dependencies {
  implementation("space.vectrix:sync-collections:1.0.0-SNAPSHOT")
}
```

**Maven**

```xml
<dependency>
  <groupId>space.vectrix</groupId>
  <artifactId>sync-collections</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Performance

The benchmarks below were recorded on a _M4 Mac Pro_.

- [SyncMap Benchmarks](benchmarks.md#syncmap)

## Inspiration & Credits

`SyncMap` draws inspiration from Go’s `sync` package — particularly its `sync.Map` and synchronization primitives — to deliver efficient, lock-free behavior in Java.
