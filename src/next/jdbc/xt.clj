;; copyright (c) 2023 sean corfield, all rights reserved

(ns next.jdbc.xt
  (:require [clojure.core.reducers :as r]
            [clojure.string :as str]
            [xtdb.api :as xt]
            [next.jdbc.protocols :as p]
            [xtdb.node :as xt.node]))

(extend-protocol p/Sourceable
  xtdb.node.Node
  (get-datasource [this] this))

(extend-protocol p/Connectable
  xtdb.node.Node
  (get-connection [this _opts] this))

(extend-protocol p/Transactable
  xtdb.node.Node
  (-transact [this body-fn _opts] (body-fn this)))

(defn- is-query? [sql-params]
  (str/starts-with? (str/upper-case (first sql-params)) "SELECT"))

(extend-protocol p/Executable
  xtdb.node.Node
  (-execute [this sql-params opts]
            (reify
              clojure.lang.IReduceInit
              (reduce [_ f init]
                      (reduce f init (p/-execute-all this sql-params opts)))
              r/CollFold
              (coll-fold [_ n combinef reducef]
                         (r/fold n combinef reducef
                                 (p/-execute-all this sql-params opts)))
              (toString [_] "`IReduceInit` from `plan` -- missing reduction?")))
  (-execute-one [this sql-params opts]
                (first
                 (if (is-query? sql-params)
                   (xt/q this sql-params opts)
                   (xt/submit-tx this [[:sql sql-params]]))))
  (-execute-all [this sql-params opts]
                (if (is-query? sql-params)
                  (xt/q this sql-params opts)
                   (xt/submit-tx this [[:sql sql-params]]))))

(comment

  ;; Once you have a REPL (started with clj -A:xtdb if you’re on JDK 16+), you can create an in-memory XTDB node with:
  (require '[next.jdbc.plan :as plan]
           '[next.jdbc.sql :as sql]
           '[xtdb.datalog :as xtd])

  (def my-node (xt.node/start-node {:xtdb/server {:port 3001}}))

  ;; Confirm this API call returns successfully
  (xtd/status my-node)

  (sql/insert! my-node :person {:xt$id "sean/1" :name "Sean Corfield" :state "CA"})
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p WHERE p.state = ?"
                      "CA"])
  (sql/insert! my-node :person {:xt$id "james/1" :name "James Rohen" :state "England"})
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p"])
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p WHERE p.state = ?"
                      "England"])
  (sql/update! my-node :person {:name "James A Rohen"} {:person.xt$id "james/1"})
  (sql/delete! my-node :person {:person.xt$id "james/1"})
  (plan/select! my-node :name ["select p.name from person p"])
  )
