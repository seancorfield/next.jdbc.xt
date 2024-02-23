# CHANGELOG

2024-02-23
* Queries now pass `:key-fn :snake-case-string` to `xt/q` and then call `clojure.walk/keywordize-keys` on the result, which produces data that will round-trip through HoneySQL and `next.jdbc`.
* Updated to latest XTDB 2 snapshots, which changed the client artifact name, the default result format (keys), and some of the internals that `next.jdbc.xt` previously relied on via PR [#6](https://github.com/seancorfield/next.jdbc.xt/pull/6) from [@jarohen](https://github.com/jarohen).

2023-12-28 ef94a94d3f4baea6142bde64ce2c6dd3177d1b39
* Updated to work with current XTDB 2 snapshots. Requires JDK 17+ for the in-process node or JDK 11+ for the client node. README has examples for both.

2023-04-28 4db5c7c30c005a159d8637a273b2c05d369f7f6e
* Initial version with very early prerelease build of XTDB 2.
