resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "RoundEights" at "http://maven.spikemark.net/roundeights"
)

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.18")

addSbtPlugin("org.scalatra.sbt" % "scalatra-sbt" % "0.5.1")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.9")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.4")

