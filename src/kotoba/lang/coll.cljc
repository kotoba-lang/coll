(ns kotoba.lang.coll
  "Portable collection helpers for the kotoba foundational stdlib — the map/seq
  ops every actor otherwise re-rolls. Layer 1 (data): pure functions, no host
  capability, no third-party deps. Runs on JVM / SCI / ClojureScript / GraalVM /
  kotoba-WASM.

  These complement `clojure.core`: `clojure.core` already has `map`/`filter`/
  `reduce`/`merge`/`group-by`; this namespace adds the small map-shaping and
  recursive-merge helpers that are not in `clojure.core` but that every kotoba
  vertical lib re-implements."
  (:refer-clojure :exclude [merge]))

(defn map-vals
  "Return a map with the same keys as `m` and `f` applied to each value.
  Preserves the key's identity (objects/keywords/strings)."
  [f m]
  (reduce-kv (fn [out k v] (assoc out k (f v))) {} m))

(defn map-keys
  "Return a map with `f` applied to each key and the same values. If `f`
  produces duplicate keys, later entries win."
  [f m]
  (reduce-kv (fn [out k v] (assoc out (f k) v)) {} m))

(defn filter-vals
  "Return a map containing only the entries of `m` whose value satisfies
  `pred`."
  [pred m]
  (reduce-kv (fn [out k v]
               (if (pred v) (assoc out k v) out))
             {} m))

(defn filter-keys
  "Return a map containing only the entries of `m` whose key satisfies `pred`."
  [pred m]
  (reduce-kv (fn [out k v]
               (if (pred k) (assoc out k v) out))
             {} m))

(defn deep-merge
  "Recursively merge maps: when both values at a key are maps, they are merged
  recursively; otherwise the rightmost value wins. Scalars and collections that
  are not both maps are replaced (not concatenated) — this is merge semantics,
  not conj semantics."
  ([a] a)
  ([a b]
   (if (and (map? a) (map? b))
     (merge-with deep-merge a b)
     b))
  ([a b & more]
   (reduce deep-merge (deep-merge a b) more)))

(defn index-by
  "Return a map from `(keyfn item)` to `item` for each `item` in `coll`. Later
  items win on key collision. Nil keys are dropped (a nil key would collapse
  entries)."
  [keyfn coll]
  (reduce (fn [out item]
            (let [k (keyfn item)]
              (if (nil? k) out (assoc out k item))))
          {} coll))

(defn assoc-some
  "Assoc `k` to `v` in `m` only when `v` is not nil. Useful for building option
  maps without (when ...) scaffolding at every call site."
  ([m k v]
   (if (nil? v) m (assoc m k v)))
  ([m k v & kvs]
   (let [m (assoc-some m k v)]
     (if (seq kvs)
       (recur m (first kvs) (second kvs) (nnext kvs))
       m))))
