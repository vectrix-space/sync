import org.jspecify.annotations.NullMarked;

/**
 * Sync Collections: Thread-safe collections for highly concurrent scenarios.
 */
@NullMarked
module space.vectrix.sync.collections {
  requires transitive org.jspecify;
  requires transitive org.jetbrains.annotations;

  exports space.vectrix.sync.collections;
}
