<div align="center">
  <h1>Sync Collections</h1>
  <br/>
</div>

<div align="center">

![Build Status](https://github.com/vectrix-space/sync/actions/workflows/build.yml/badge.svg)
[![MIT License](https://img.shields.io/badge/license-MIT-blue)](license.txt)
[![Discord](https://img.shields.io/discord/819522977586348052)](https://discord.gg/rYpaxPFQrj)
[![Maven Central](https://img.shields.io/maven-central/v/space.vectrix/sync-collections?label=stable)](https://search.maven.org/search?q=g:space.vectrix%20AND%20a:sync*)
![Maven Snapshot Version](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fspace%2Fvectrix%2Fsync-collections%2Fmaven-metadata.xml&query=%2Fmetadata%2Fversioning%2Flatest&label=dev)

</div>

> This project is experimental and not considered stable for production yet.

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

The benchmarks below were recorded on a _M4 Macbook Pro_.

- [SyncMap Benchmarks](benchmarks.md#syncmap)

## Inspiration & Credits

`SyncMap` draws inspiration from Go’s `sync` package — particularly its `sync.Map` and synchronization primitives — to deliver efficient, lock-free behavior in Java.
