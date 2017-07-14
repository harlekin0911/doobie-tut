scalaVersion := "2.11.8" // no support for 2.10 or 2.12 at the moment

lazy val doobieVersion = "0.4.0"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core"       % doobieVersion,
  "org.tpolecat" %% "doobie-postgres"   % doobieVersion,
  "org.tpolecat" %% "doobie-specs2"     % doobieVersion
)
