# kotoba-lang/coll

[![CI](https://github.com/kotoba-lang/coll/actions/workflows/ci.yml/badge.svg)](https://github.com/kotoba-lang/coll/actions/workflows/ci.yml)

**Layer 1 (data) of the kotoba foundational stdlib** — portable collection
helpers every kotoba actor otherwise re-rolls. Zero third-party runtime deps;
every namespace is `.cljc`, so it runs on JVM / SCI / ClojureScript / GraalVM /
kotoba-WASM. See
[`docs/adr/ADR-kotoba-lang-foundational-stdlib.md`](https://github.com/kotoba-lang/kotoba-lang/blob/main/docs/adr/ADR-kotoba-lang-foundational-stdlib.md)
for the layering decision.

## Current surface

`kotoba.lang.coll` — map/seq helpers complementary to `clojure.core`:

- `map-vals`, `map-keys` — transform a map's values / keys
- `filter-vals`, `filter-keys` — select map entries by predicate
- `deep-merge` — recursive map merge (maps nested, non-maps last-wins)
- `index-by` — build a lookup map from a key fn
- `assoc-some` — `assoc` only when the value is not `nil`

These are pure functions with no host capability — the data-layer foundation
that `langchain` / `langgraph` / `statechart` / `num` and the rest of the
vertical `*-clj` libs stand on.

## Install

```clojure
io.github.kotoba-lang/coll {:git/sha "<sha>"}
```

## Use

```clojure
(require '[kotoba.lang.coll :as coll])

(coll/map-vals inc {:a 1 :b 2})        ;=> {:a 2 :b 3}
(coll/deep-merge {:a {:x 1}} {:a {:y 2}}) ;=> {:a {:x 1 :y 2}}
(coll/index-by :id [{:id 1} {:id 2}])  ;=> {1 {:id 1} 2 {:id 2}}
```

## Verify

```sh
clojure -M:test
```
