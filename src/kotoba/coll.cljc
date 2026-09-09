(ns kotoba.coll
  "Assembled from one repo per definition.

  This namespace holds no implementation. It re-exports the definitions
  that each live in their own repo, so a call site can require one name
  and a library can require only the definitions it actually uses."
  (:require [kotoba.coll.assoc-some :as assoc-some-ns]
            [kotoba.coll.deep-merge :as deep-merge-ns]
            [kotoba.coll.filter-keys :as filter-keys-ns]
            [kotoba.coll.filter-vals :as filter-vals-ns]
            [kotoba.coll.index-by :as index-by-ns]
            [kotoba.coll.map-keys :as map-keys-ns]
            [kotoba.coll.map-vals :as map-vals-ns]))

(def assoc-some "See kotoba.coll.assoc-some/assoc-some." assoc-some-ns/assoc-some)
(def deep-merge "See kotoba.coll.deep-merge/deep-merge." deep-merge-ns/deep-merge)
(def filter-keys "See kotoba.coll.filter-keys/filter-keys." filter-keys-ns/filter-keys)
(def filter-vals "See kotoba.coll.filter-vals/filter-vals." filter-vals-ns/filter-vals)
(def index-by "See kotoba.coll.index-by/index-by." index-by-ns/index-by)
(def map-keys "See kotoba.coll.map-keys/map-keys." map-keys-ns/map-keys)
(def map-vals "See kotoba.coll.map-vals/map-vals." map-vals-ns/map-vals)
