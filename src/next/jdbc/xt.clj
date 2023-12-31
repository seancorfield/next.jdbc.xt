;; copyright (c) 2023 sean corfield, all rights reserved

(ns next.jdbc.xt
  (:require [clojure.core.reducers :as r]
            [clojure.string :as str]
            [next.jdbc.protocols :as p]
            [xtdb.api :as xt]))

(defn- is-query? [sql-params]
  (-> sql-params
      (first)
      (str/trim)
      (str/upper-case)
      (str/starts-with? "SELECT")))

(defn- -execute* [this sql-params opts]
  (reify
    clojure.lang.IReduceInit
    (reduce [_ f init]
      (reduce f init (p/-execute-all this sql-params opts)))
    r/CollFold
    (coll-fold [_ n combinef reducef]
      (r/fold n combinef reducef
              (p/-execute-all this sql-params opts)))
    (toString [_] "`IReduceInit` from `plan` -- missing reduction?")))

(defn- -execute-one* [this sql-params opts]
  (if (is-query? sql-params)
    (first
     (xt/q this
           (first sql-params)
           (assoc opts :args (rest sql-params))))
    (xt/submit-tx this
                  [(-> (xt/sql-op (first sql-params))
                       (xt/with-op-args (rest sql-params)))]
                  opts)))

(defn- -execute-all* [this sql-params opts]
  (if (is-query? sql-params)
    (xt/q this
          (first sql-params)
          (assoc opts :args (rest sql-params)))
    (xt/submit-tx this
                  [(-> (xt/sql-op (first sql-params))
                       (xt/with-op-args (rest sql-params)))]
                  opts)))

(defmacro for-node [& body]
  (try
    (require 'xtdb.node.impl)
    (Class/forName "xtdb.node.impl.Node")
    (Class/forName "xtdb.node.impl.SubmitNode")
    `(do ~@body)
    (catch Exception _)))

(for-node
 (extend-protocol p/Sourceable
   xtdb.node.impl.Node
   (get-datasource [this] this)
   xtdb.node.impl.SubmitNode
   (get-datasource [this] this))

 (extend-protocol p/Connectable
   xtdb.node.impl.Node
   (get-connection [this _opts] this)
   xtdb.node.impl.SubmitNode
   (get-connection [this _opts] this))

 (extend-protocol p/Transactable
   xtdb.node.impl.Node
   (-transact [this body-fn _opts] (body-fn this))
   xtdb.node.impl.SubmitNode
   (-transact [this body-fn _opts] (body-fn this)))

 (extend-protocol p/Executable
   xtdb.node.impl.Node
   (-execute [this sql-params opts]
     (-execute* this sql-params opts))
   (-execute-one [this sql-params opts]
     (-execute-one* this sql-params opts))
   (-execute-all [this sql-params opts]
     (-execute-all* this sql-params opts))
   xtdb.node.impl.SubmitNode
   (-execute [this sql-params opts]
     (-execute* this sql-params opts))
   (-execute-one [this sql-params opts]
     (-execute-one* this sql-params opts))
   (-execute-all [this sql-params opts]
     (-execute-all* this sql-params opts))))

(defmacro for-client [& body]
  (try
    (require 'xtdb.client.impl)
    (Class/forName "xtdb.client.impl.XtdbClient")
    `(do ~@body)
    (catch Exception _)))

(for-client
 (extend-protocol p/Sourceable
   xtdb.client.impl.XtdbClient
   (get-datasource [this] this))

 (extend-protocol p/Connectable
   xtdb.client.impl.XtdbClient
   (get-connection [this _opts] this))

 (extend-protocol p/Transactable
   xtdb.client.impl.XtdbClient
   (-transact [this body-fn _opts] (body-fn this)))

 (extend-protocol p/Executable
   xtdb.client.impl.XtdbClient
   (-execute [this sql-params opts]
     (-execute* this sql-params opts))
   (-execute-one [this sql-params opts]
     (-execute-one* this sql-params opts))
   (-execute-all [this sql-params opts]
     (-execute-all* this sql-params opts))))

(try
  (require 'xtdb.node.impl)
  (catch Exception _
    (try
      (require 'xtdb.client.impl)
      (catch Exception _
        (throw (Exception. "next.jdbc.xt requires xtdb.node or xtdb.client"))))))

(comment
  ;; Once you have a REPL (started with clj -A:xtdb if you’re on JDK 16+), you can create an in-memory XTDB node with:
  (require '[next.jdbc :as jdbc]
           '[next.jdbc.plan :as plan]
           '[next.jdbc.sql :as sql])
  (require '[xtdb.node :as xtn])
  (require '[xtdb.client :as xtc]) ; requires clj -A:xtdb-client & JDK 11+

  (def my-node (xtn/start-node {}))
  (def my-node (xtc/start-client "http://localhost:3000"))
  (.close my-node)

  ;; Confirm this API call returns successfully
  (xt/status my-node)

  (sql/insert! my-node :person {:xt$id "sean/1" :name "Sean Corfield" :state "CA"})
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p WHERE p.state = ?"
                      "CA"])
  (sql/query my-node ["select * from person for all system_time"])
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
