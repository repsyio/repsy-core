# core-parent

The Maven parent of the Repsy applications: `core-build-parent` (build configuration, quality gates)
joined with `core-dependencies` (third-party versions), with no content of its own. The library
modules of this repository use `core-build-parent` instead. The name is kept because `repsy` (through
the root `core` aggregator) and `repsy-mono` reference it.
