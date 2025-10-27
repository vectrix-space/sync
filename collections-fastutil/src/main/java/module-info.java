import org.jspecify.annotations.NullMarked;

/**
 * Sync Collections `fastutil`: Thread-safe collection implementations for highly concurrent scenarios with `fastutil`.
 */
@NullMarked
module space.vectrix.sync.collections.fastutil {
  requires transitive static org.jspecify;
  requires transitive static org.jetbrains.annotations;
  requires transitive it.unimi.dsi.fastutil;

  exports space.vectrix.sync.collections.fastutil;
}
