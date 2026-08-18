(ns kotoba.lang.coll-test
  (:require [clojure.test :refer [deftest is testing]]
            [kotoba.lang.coll :as coll]))

(deftest map-vals-and-keys
  (is (= {:a 2 :b 3} (coll/map-vals inc {:a 1 :b 2})))
  (is (= {} (coll/map-vals inc {})))
  ;; map-keys applies f to each KEY (keyword -> string via name)
  (is (= {"a" 1 "b" 2} (coll/map-keys name {:a 1 :b 2}))))

(deftest filter-vals-and-keys
  (is (= {:b 2} (coll/filter-vals even? {:a 1 :b 2 :c 3})))
  (is (= {:a 1 :c 3} (coll/filter-keys #{:a :c} {:a 1 :b 2 :c 3})))
  (is (= {} (coll/filter-vals pos? {}))))

(deftest deep-merge-nests-maps-replaces-scalars
  (is (= {:a {:x 1 :y 2}} (coll/deep-merge {:a {:x 1}} {:a {:y 2}})))
  (is (= {:a 2} (coll/deep-merge {:a 1} {:a 2})))
  (is (= {:a {:x 1 :y 2} :b 1} (coll/deep-merge {:a {:x 1} :b 1} {:a {:y 2}})))
  (is (= {:a {:x 1 :y 2 :z 3}}
         (coll/deep-merge {:a {:x 1}} {:a {:y 2}} {:a {:z 3}})))
  ;; vectors are not maps → replaced, not concatenated
  (is (= {:a [2]} (coll/deep-merge {:a [1]} {:a [2]})))
  ;; nil wins as a scalar (does not erase a map unless right side is a map)
  (is (= {:a nil} (coll/deep-merge {:a {:x 1}} {:a nil}))))

(deftest index-by-builds-lookup
  (is (= {1 {:id 1} 2 {:id 2}}
         (coll/index-by :id [{:id 1} {:id 2}])))
  ;; collision: later wins
  (is (= {1 {:id 1 :v 2}} (coll/index-by :id [{:id 1 :v 1} {:id 1 :v 2}])))
  ;; nil keys dropped
  (is (= {1 {:id 1}} (coll/index-by :id [{:id 1} {:id nil}])))
  (is (= {} (coll/index-by :id []))))

(deftest assoc-some-skips-nil
  (is (= {:a 1 :b 2} (coll/assoc-some {:a 1} :b 2 :c nil)))
  (is (= {:a 1} (coll/assoc-some {:a 1} :b nil)))
  (is (= {} (coll/assoc-some {} :a nil :b nil)))
  (is (= {:a 1 :b 2} (coll/assoc-some {} :a 1 :b 2 :c nil))))

(deftest edge-and-nil-cases
  ;; deep-merge: a map and a non-map scalar — scalar wins, does not merge
  (is (= {:a 1} (coll/deep-merge {:a {:x 1}} {:a 1})))
  ;; deep-merge: nil right side is a scalar and wins (erases a map)
  (is (= {:a nil} (coll/deep-merge {:a {:x 1}} {:a nil})))
  ;; deep-merge: empty maps are transparent
  (is (= {:a 1} (coll/deep-merge {:a 1} {})))
  (is (= {:a 1} (coll/deep-merge {} {:a 1})))
  ;; index-by: nil keys are dropped (never collapse into a nil-keyed entry)
  (is (= {} (coll/index-by (constantly nil) [{:id 1} {:id 2}])))
  ;; index-by: non-collection input is tolerated
  (is (= {} (coll/index-by :id [])))
  ;; map-vals/map-keys over empty map is empty
  (is (= {} (coll/map-vals inc {})))
  (is (= {} (coll/map-keys name {}))))

(deftest set-union-variadic
  (is (= #{} (coll/set-union)))
  (is (= #{1 2} (coll/set-union #{1 2})))
  (is (= #{1 2 3} (coll/set-union #{1 2} #{2 3})))
  (is (= #{1 2 3 4} (coll/set-union #{1} #{2} #{3} #{4})))
  (is (= #{} (coll/set-union #{} #{}))))

(deftest set-intersection-variadic
  (is (= #{2} (coll/set-intersection #{1 2} #{2 3})))
  (is (= #{} (coll/set-intersection #{1} #{2})))
  (is (= #{2} (coll/set-intersection #{1 2 3} #{2 3 4} #{2 5})))
  (is (= #{1 2} (coll/set-intersection #{1 2}))))

(deftest set-difference-variadic
  (is (= #{1} (coll/set-difference #{1 2} #{2 3})))
  (is (= #{1 2} (coll/set-difference #{1 2} #{3})))
  (is (= #{1} (coll/set-difference #{1 2 3} #{2} #{3})))
  (is (= #{1 2} (coll/set-difference #{1 2}))))

(deftest bounded-prewalk-transforms-top-down
  ;; every number doubled, top-down traversal order does not affect this
  ;; particular transform's result but does affect side-effecting order
  (is (= {:a 2 :b [4 6]}
         (coll/bounded-prewalk (fn [x] (if (number? x) (* x 2) x))
                                {:a 1 :b [2 3]})))
  (is (= [1 2 3] (coll/bounded-prewalk identity [1 2 3])))
  (is (= '(1 2 3) (coll/bounded-prewalk identity '(1 2 3))))
  (is (= #{1 2 3} (coll/bounded-prewalk identity #{1 2 3}))))

(deftest bounded-postwalk-transforms-bottom-up
  (is (= {:a 2 :b [4 6]}
         (coll/bounded-postwalk (fn [x] (if (number? x) (* x 2) x))
                                 {:a 1 :b [2 3]})))
  (is (= [1 2 3] (coll/bounded-postwalk identity [1 2 3]))))

(deftest bounded-walk-rejects-past-depth-limit
  (let [deep (reduce (fn [acc _] {:n acc}) 0 (range 5))]
    ;; depth 0 is the root map itself; five nested maps need depth >= 5
    (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo :cljs ExceptionInfo)
                           #"bounded depth limit"
                           (coll/bounded-prewalk identity 2 deep)))
    (is (thrown-with-msg? #?(:clj clojure.lang.ExceptionInfo :cljs ExceptionInfo)
                           #"bounded depth limit"
                           (coll/bounded-postwalk identity 2 deep)))
    ;; a depth ceiling that fits does not throw
    (is (= deep (coll/bounded-prewalk identity 10 deep)))
    (is (= deep (coll/bounded-postwalk identity 10 deep)))))
