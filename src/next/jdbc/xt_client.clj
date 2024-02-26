;; copyright (c) 2023-2024 sean corfield, all rights reserved

(ns next.jdbc.xt-client
  "Require this to support the XtdbClient."
  (:require [next.jdbc.protocols :as p]
            [next.jdbc.xt-core :as nx]
            [xtdb.client]
            [xtdb.client.impl]))

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
    (nx/-execute* this sql-params opts))
  (-execute-one [this sql-params opts]
    (nx/-execute-one* this sql-params opts))
  (-execute-all [this sql-params opts]
    (nx/-execute-all* this sql-params opts)))
