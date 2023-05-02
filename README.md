# next.jdbc.xt

Experimental extension of
[`next.jdbc`](https://github.com/seancorfield/next-jdbc)
to work with
[XTDB 2.0](https://www.xtdb.com/v2) snapshots.

Add this library as a git dependency:

```clojure
  io.github.seancorfield/next.jdbc.xt
  {:git/sha "4db5c7c30c005a159d8637a273b2c05d369f7f6e"}
```

Require `next.jdbc.xt` to enable the protocol definitions for XTDB.

Start XTDB as normal and pass the node as a "connectable" into
the various `next.jdbc` functions:

```clojure
  (require '[next.jdbc.plan :as plan]
           '[next.jdbc.sql :as sql]
           '[next.jdbc.xt]
           '[xtdb.node :as xt.node])

  (def my-node (xt.node/start-node {:xtdb/server {:port 3001}}))

  (sql/insert! my-node :person {:xt$id "sean/1" :name "Sean Corfield" :state "CA"})
  ;; queries require qualified columns right now:
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p WHERE p.state = ?"
                      "CA"])
  (sql/insert! my-node :person {:xt$id "james/1" :name "James Rohen" :state "England"})
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p"])
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p WHERE p.state = ?"
                      "England"])
  (sql/update! my-node :person {:name "James A Rohen"} {:person.xt$id "james/1"})
  (sql/delete! my-node :person {:person.xt$id "james/1"})
  (plan/select! my-node :name ["select p.name from person p"])
```
