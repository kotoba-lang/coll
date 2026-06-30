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
