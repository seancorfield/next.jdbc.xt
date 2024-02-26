;; copyright (c) 2023-2024 sean corfield, all rights reserved

(ns next.jdbc.xt
  "Require this to automatically load support for
   either the in-memory Node or the HTTP XtdbClient.")

(try
  (require 'xtdb.node)
  (require 'next.jdbc.xt-node)
  (catch Exception _
    (try
      (require 'xtdb.client)
      (catch Exception _
        (throw (Exception. "next.jdbc.xt requires xtdb.node or xtdb.client"))))))

(try
  (require 'xtdb.client)
  (require 'next.jdbc.xt-client)
  (catch Exception _))

(comment
  ;; Once you have a REPL (started with clj -A:xtdb if you’re on JDK 16+), you can create an in-memory XTDB node with:
  (require '[next.jdbc :as jdbc]
           '[next.jdbc.plan :as plan]
           '[next.jdbc.sql :as sql]
           '[xtdb.api :as xt])
  (require '[xtdb.node :as xtn])
  (require '[xtdb.client :as xtc]) ; requires clj -A:xtdb-client & JDK 11+

  (def my-node (xtn/start-node {}))
  (def my-node (xtc/start-client "http://localhost:3000"))
  (.close my-node)
  ((juxt type (comp ancestors type)) my-node)

  ;; Confirm this API call returns successfully
  (xt/status my-node)

  (sql/insert! my-node :person {:xt$id "sean/1" :name "Sean Corfield" :state "CA"})
  (sql/insert! my-node :person {:xt$id "sean/1" :name "Sean Corfield" :state "TX"
                                :xt$valid_from #inst "2024-01-08T22:01:30Z"
                                :xt$valid_to #inst "2024-01-08T22:01:59Z"})
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p WHERE p.state = ?"
                      "CA"])
  (sql/query my-node ["select p.name, p.xt$valid_from, p.xt$valid_to, p.xt$system_from, p.xt$system_to from person for all system_time as p"])
  (sql/query my-node ["select \"p\".\"name\", \"p\".\"xt/valid-from\", \"p\".\"xt/valid-to\", \"p\".\"xt/system-from\", \"p\".\"xt/system-to\" from \"person\" for all system_time as \"p\""])
  (sql/query my-node ["select p.*, p.xt$system_to from person for all system_time as p"])
  (sql/query my-node ["select p.*, p.xt$system_to from person for all valid_time as p"])
  (sql/query my-node ["select p.name, p.state, p.xt$valid_from, p.xt$valid_to, p.xt$system_from, p.xt$system_to from person for all valid_time as p order by p.xt$valid_from"])
  (sql/query my-node ["select * from person for all valid_time"])
  (jdbc/execute! my-node ["select * from person"])
  (jdbc/execute-one! my-node ["select * from person"])
  (sql/insert! my-node :person {:xt$id "james/1" :name "James Rohen" :state "England"})
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p"])
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p WHERE p.state = ?"
                      "England"])
  (sql/update! my-node :person {:name "James A Rohen"} {:person.xt$id "james/1"})
  (plan/select! my-node :name ["select p.name from person p"])
  (sql/delete! my-node :person {:person.xt$id "james/1"})
  (plan/select! my-node :name ["select p.name from person p"])
  (sql/delete! my-node :person {:person.xt$id "sean/1"})
  (plan/select! my-node :name ["select p.name from person p"])
  )
