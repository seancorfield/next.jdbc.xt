# CHANGELOG

2024-02-22 e8b8659c78425e9bfd72074babbda2470a96df47
* Reverted most of the previous change because it was susceptible to order of compilation problems with the protocol extensions. NOTE: round-tripping of SQL is BROKEN since SQL requires `xt$id` and `first_name` (unquoted) but returns `:xt/id` and `:first-name`.

2024-02-03 f5ca836a9126b351183d1018dcd0d731d93debea
* Updated to latest XTDB 2 snapshots, which changed the client artifact name and a lot of the internals that `next.jdbc.xt` previously relied on via PR [#6](https://github.com/seancorfield/next.jdbc.xt/pull/6) from [@jarohen](https://github.com/jarohen).

2023-12-28 ef94a94d3f4baea6142bde64ce2c6dd3177d1b39
* Updated to work with current XTDB 2 snapshots. Requires JDK 17+ for the in-process node or JDK 11+ for the client node. README has examples for both.

2023-04-28 4db5c7c30c005a159d8637a273b2c05d369f7f6e
* Initial version with very early prerelease build of XTDB 2.
