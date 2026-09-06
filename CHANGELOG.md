# Changelog

All notable changes to kotoba-lang/coll are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/). Semver per the
kotoba-lang stdlib compatibility policy (kotoba-lang/kotoba-lang/docs/lang/stdlib-versioning.md).

## [Unreleased]

`clojure.set`/`clojure.walk` gap-fill, per
adr-2809061500-clojure-namespace-to-kotoba-stdlib (com-junkawasaki/root).

### Added

- `clojure.set` coverage: `subset?`, `superset?`, `select`, `project`,
  `rename-keys`, `rename`, `index`, `join` (2-arg natural join and 3-arg
  explicit-key-mapping join).
- `clojure.walk` coverage: genuinely unbounded `walk`, `prewalk`,
  `postwalk`, `prewalk-replace`, `postwalk-replace` — new, separate
  functions alongside the existing (unchanged) `bounded-prewalk`/
  `bounded-postwalk`, not a widening of them. See README's "Tree
  walking" section for why the two pairs are permanent and parallel,
  not one superseding the other.

## [0.1.0] - 2026-07-01

Initial public release. kotoba.lang.coll — portable collection helpers (map-vals, deep-merge, index-by, assoc-some).

### Added

- Initial library surface, tests, and CI.
