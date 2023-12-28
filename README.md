# next.jdbc.xt

Experimental extension of
[`next.jdbc`](https://github.com/seancorfield/next-jdbc)
to work with
[XTDB 2.0](https://www.xtdb.com/v2) snapshots.

Add this library as a git dependency:

```clojure
  io.github.seancorfield/next.jdbc.xt
  {:git/sha "ef94a94d3f4baea6142bde64ce2c6dd3177d1b39"}
```

Require `next.jdbc.xt` to enable the protocol definitions for XTDB.

## XTDB In-Process

For this repo, `clj -A:xtdb` will start a REPL with XTDB available in-process.
This requires JDK 17+.

Start XTDB in-process and pass the node as a "connectable" into
the various `next.jdbc` functions:

```clojure
  (require '[next.jdbc.plan :as plan]
           '[next.jdbc.sql :as sql]
           '[next.jdbc.xt]
           '[xtdb.node :as xtn])

  (def my-node (xtn/start-node {}))

  (sql/insert! my-node :person {:xt$id "sean/1" :name "Sean Corfield" :state "CA"})
  ;; queries require qualified columns right now:
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p WHERE p.state = ?"
                      "CA"])
  (sql/insert! my-node :person {:xt$id "james/1" :name "James Rohen" :state "England"})
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p"])
  ;; but you can get all columns back with a wildcard:
  (sql/query my-node ["select * from person"])
  (sql/query my-node ["SELECT p.xt$id, p.name FROM person p WHERE p.state = ?"
                      "England"])
  (sql/update! my-node :person {:name "James A Rohen"} {:person.xt$id "james/1"})
  (sql/delete! my-node :person {:person.xt$id "james/1"})
  (plan/select! my-node :name ["select p.name from person p"])

  (.close my-node) ; when you are finished
```

## XTDB Client

If you have XTDB running as a server process, either via Docker or at AWS,
then `clj -A:xtdb-client` will start a REPL with an XTDB client available.
This requires JDK 11+.

Start an XTDB client node and pass it as a "connectable" into
the various `next.jdbc` functions:

```clojure
  (require '[next.jdbc.plan :as plan]
           '[next.jdbc.sql :as sql]
           '[next.jdbc.xt]
           '[xtdb.client :as xtc])

  (def my-node (xtc/start-node "http://localhost:3000")) ; or your AWS URL

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

  (.close my-node) ; when you are finished
```
