import org.jspecify.annotations.NullMarked;

/**
 * Sync Collections: Thread-safe collections for highly concurrent scenarios.
 */
@NullMarked
module space.vectrix.sync.collections {
  requires transitive static org.jspecify;
  requires transitive static org.jetbrains.annotations;

  exports space.vectrix.sync.collections;
}
