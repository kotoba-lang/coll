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

;; -- Set algebra ------------------------------------------------------------
;; Replaces clojure.set/union|intersection|difference in the frontier
;; (90-docs/migration/kotoba-cljc-common-lib-frontier.edn, :collection-walk-set).
;; General, unbounded-arity oracle form. The sovereign .kotoba kernel keeps
;; only the bounded 32-item typed-set primitives -- general set algebra stays
;; here because restricted-JS KIR rejects it (measured 2026-08-02, see
;; migration/bounded-coll-v1.edn :retained :set-algebra).

(defn set-union
  "Union of zero or more sets. Mirrors clojure.set/union without the
  clojure.set dependency."
  ([] #{})
  ([s1] (or s1 #{}))
  ([s1 s2] (into (or s1 #{}) (or s2 #{})))
  ([s1 s2 & sets] (reduce set-union (set-union s1 s2) sets)))

(defn set-intersection
  "Intersection of one or more sets. Mirrors clojure.set/intersection."
  ([s1] s1)
  ([s1 s2] (into (empty s1) (filter #(contains? s2 %)) s1))
  ([s1 s2 & sets] (reduce set-intersection (set-intersection s1 s2) sets)))

(defn set-difference
  "Elements of s1 not present in any of the other sets. Mirrors
  clojure.set/difference."
  ([s1] s1)
  ([s1 s2] (into (empty s1) (remove #(contains? s2 %)) s1))
  ([s1 s2 & sets] (reduce set-difference (set-difference s1 s2) sets)))

;; -- Bounded walk -------------------------------------------------------
;; Replaces clojure.walk/prewalk|postwalk in the frontier
;; (:collection-walk-set, forbidden [:unbounded-recursion]). Unlike
;; clojure.walk, both variants take an explicit depth ceiling and throw
;; instead of recursing without bound, so a cyclical or adversarial input
;; cannot exhaust the stack.

(def default-max-walk-depth
  "Depth ceiling used when bounded-prewalk/bounded-postwalk are called
  without an explicit max-depth."
  1024)

(defn- walk-depth-exceeded! [limit]
  (throw (ex-info "coll walk exceeds bounded depth limit"
                   {:kotoba.lang.coll/reason :walk/depth-exceeded :limit limit})))

(defn- walk-children [walk-one x depth]
  (cond
    (map? x) (into (empty x)
                    (map (fn [[k v]] [(walk-one k depth) (walk-one v depth)]))
                    x)
    (seq? x) (doall (map #(walk-one % depth) x))
    (coll? x) (into (empty x) (map #(walk-one % depth)) x)
    :else x))

(defn bounded-prewalk
  "Like clojure.walk/prewalk: apply f to form and then to its children,
  top-down. Bounded by max-depth (default default-max-walk-depth); throws
  ex-info rather than recursing without limit once the ceiling is crossed."
  ([f form] (bounded-prewalk f default-max-walk-depth form))
  ([f max-depth form]
   (letfn [(walk [x depth]
             (when (> depth max-depth) (walk-depth-exceeded! max-depth))
             (walk-children walk (f x) (inc depth)))]
     (walk form 0))))

(defn bounded-postwalk
  "Like clojure.walk/postwalk: apply f to form's children first, then to
  form itself, bottom-up. Bounded by max-depth (default
  default-max-walk-depth); throws ex-info rather than recursing without
  limit once the ceiling is crossed."
  ([f form] (bounded-postwalk f default-max-walk-depth form))
  ([f max-depth form]
   (letfn [(walk [x depth]
             (when (> depth max-depth) (walk-depth-exceeded! max-depth))
             (f (walk-children walk x (inc depth))))]
     (walk form 0))))
