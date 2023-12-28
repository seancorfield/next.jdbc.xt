;; copyright (c) 2023 sean corfield, all rights reserved

(ns next.jdbc.xt
  (:require [clojure.core.reducers :as r]
            [clojure.string :as str]
            [next.jdbc.protocols :as p]
            [xtdb.api :as xt]
            [xtdb.protocols]))

(extend-protocol p/Sourceable
  xtdb.protocols.PSubmitNode
  (get-datasource [this] this))

(extend-protocol p/Connectable
  xtdb.protocols.PSubmitNode
  (get-connection [this _opts] this))

(extend-protocol p/Transactable
  xtdb.protocols.PSubmitNode
  (-transact [this body-fn _opts] (body-fn this)))

(defn- is-query? [sql-params]
  (str/starts-with? (str/upper-case (first sql-params)) "SELECT"))

(extend-protocol p/Executable
  xtdb.protocols.PSubmitNode
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
                (if (is-query? sql-params)
                  (first
                   (xt/q this
                         (first sql-params)
                         (assoc opts :args (rest sql-params))))
                  (xt/submit-tx this
                                [(-> (xt/sql-op (first sql-params))
                                     (xt/with-op-args (rest sql-params)))]
                                opts)))
  (-execute-all [this sql-params opts]
                (if (is-query? sql-params)
                  (xt/q this
                        (first sql-params)
                        (assoc opts :args (rest sql-params)))
                   (xt/submit-tx this
                                 [(-> (xt/sql-op (first sql-params))
                                      (xt/with-op-args (rest sql-params)))]
                                 opts))))

(comment

  ;; Once you have a REPL (started with clj -A:xtdb if you’re on JDK 16+), you can create an in-memory XTDB node with:
  (require '[next.jdbc :as jdbc]
           '[next.jdbc.plan :as plan]
           '[next.jdbc.sql :as sql])
  (require '[xtdb.node :as xtn])

  (def my-node (xtn/start-node {}))

  ;; Confirm this API call returns successfully
  (xt/status my-node)

  (sql/insert! my-node :person {:xt$id "sean/1" :name "Sean Corfield" :state "CA"})
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p WHERE p.state = ?"
                      "CA"])
  (sql/query my-node ["select * from person"])
  (jdbc/execute! my-node ["select * from person"])
  (jdbc/execute-one! my-node ["select * from person"])
  (sql/insert! my-node :person {:xt$id "james/1" :name "James Rohen" :state "England"})
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p"])
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p WHERE p.state = ?"
                      "England"])
  (sql/update! my-node :person {:name "James A Rohen"} {:person.xt$id "james/1"})
  (sql/delete! my-node :person {:person.xt$id "james/1"})
  (plan/select! my-node :name ["select p.name from person p"])
  )
