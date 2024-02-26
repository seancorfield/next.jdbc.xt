;; copyright (c) 2023-2024 sean corfield, all rights reserved

(ns next.jdbc.xt-node
  "Require this to support the in-memory Node."
  (:require [next.jdbc.protocols :as p]
            [next.jdbc.xt-core :as nx]
            [xtdb.node]
            [xtdb.node.impl]))

(extend-protocol p/Sourceable
  xtdb.node.impl.Node
  (get-datasource [this] this))

(extend-protocol p/Connectable
  xtdb.node.impl.Node
  (get-connection [this _opts] this))

(extend-protocol p/Transactable
  xtdb.node.impl.Node
  (-transact [this body-fn _opts] (body-fn this)))

(extend-protocol p/Executable
  xtdb.node.impl.Node
  (-execute [this sql-params opts]
    (nx/-execute* this sql-params opts))
  (-execute-one [this sql-params opts]
    (nx/-execute-one* this sql-params opts))
  (-execute-all [this sql-params opts]
    (nx/-execute-all* this sql-params opts)))
