;; copyright (c) 2023-2024 sean corfield, all rights reserved

(ns next.jdbc.xt-core
  (:require [clojure.core.reducers :as r]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [next.jdbc.protocols :as p]
            [xtdb.api :as xt]))

(defn- is-query? [sql-params]
  (-> sql-params
      (first)
      (str/trim)
      (str/upper-case)
      (str/starts-with? "SELECT")))

(defn -execute* [this sql-params opts]
  (reify
    clojure.lang.IReduceInit
    (reduce [_ f init]
      (reduce f init (p/-execute-all this sql-params opts)))
    r/CollFold
    (coll-fold [_ n combinef reducef]
      (r/fold n combinef reducef
              (p/-execute-all this sql-params opts)))
    (toString [_] "`IReduceInit` from `plan` -- missing reduction?")))

(defn -execute-one* [this sql-params opts]
  (if (is-query? sql-params)
    (-> (xt/q this
              (first sql-params)
              (assoc opts
                     :args (rest sql-params)
                     :key-fn :snake-case-string))
        (first)
        (walk/keywordize-keys))
    (xt/submit-tx this [[:sql (first sql-params) (rest sql-params)]] opts)))

(defn -execute-all* [this sql-params opts]
  (if (is-query? sql-params)
    (-> (xt/q this
              (first sql-params)
              (assoc opts
                     :args (rest sql-params)
                     :key-fn :snake-case-string))
        (walk/keywordize-keys))
    (xt/submit-tx this [[:sql (first sql-params) (rest sql-params)]] opts)))
