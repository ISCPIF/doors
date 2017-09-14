import org.scalatra.sbt.ScalatraPlugin


scalaOrganization in ThisBuild := "org.typelevel"

def projectSettings = Seq(
  organization := "fr.iscpif",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  libraryDependencies ++= monocle,
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven",
  resolvers += Resolver.bintrayRepo("projectseptemberinc", "maven"),
  resolvers += Resolver.sonatypeRepo("public"),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
  addCompilerPlugin("com.milessabin" % "si2712fix-plugin" % "1.2.0" cross CrossVersion.full),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

val scalatraVersion = "2.4.+"
val jettyVersion = "9.4.0.v20161208"
val json4sVersion = "3.3.0"
val httpComponentsVersion = "4.5.1"
val scalatagsVersion = "0.6.2"
val autowireVersion = "0.2.5"
val upickleVersion = "0.4.1"
val apacheDirectoryVersion = "1.0.0-M33"
def jarName(version: String) = s"doors$version.jar"
val monocleVersion = "1.2.1"
val betterFileVersion = "2.15.0"


lazy val doors = project in file(".") settings (projectSettings) aggregate(ext, rest, server, client)

lazy val hasher = ProjectRef(uri("https://github.com/Nycto/Hasher.git#v1.2.0"), "hasher")

def monocle = Seq(
  "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-generic" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-state" % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-refined" % monocleVersion
)


lazy val ext = Project(
  "ext",
  file("ext")) settings (projectSettings: _*) settings (
  libraryDependencies ++= Seq(
    "org.apache.directory.api" % "api-all" % apacheDirectoryVersion
  )) enablePlugins (ScalaJSPlugin)

lazy val rest = Project(
  "rest",
  file("rest")) settings (projectSettings: _*) settings (
  libraryDependencies ++= Seq(
    "org.apache.httpcomponents" % "httpclient" % httpComponentsVersion,
    "org.apache.httpcomponents" % "httpmime" % httpComponentsVersion,
    "org.json4s" %% "json4s-jackson" % json4sVersion
  )
  ) dependsOn (ext)


lazy val shared = project.in(file("./shared")) settings (projectSettings: _*) dependsOn (ext)

lazy val client = Project(
  "client",
  file("client")) settings (projectSettings: _*) settings (
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "autowire" % autowireVersion,
    "com.lihaoyi" %%% "upickle" % upickleVersion,
    "com.lihaoyi" %%% "scalatags" % scalatagsVersion,
    "com.lihaoyi" %%% "scalarx" % "0.3.1",
    "fr.iscpif" %%% "scaladget" % "0.9.5-SNAPSHOT",
    "org.scala-js" %%% "scalajs-dom" % "0.9.1",
    "org.json4s" %% "json4s-jackson" % json4sVersion
  )
  ) dependsOn(shared, ext) enablePlugins (ScalaJSPlugin)

def freedslVersion = "0.7"

lazy val server = Project(
  "server",
  file("server")) settings (projectSettings: _*) settings (
  ScalatraPlugin.scalatraWithJRebel ++ Seq(
    unmanagedResourceDirectories in Compile += target.value / "webapp",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "autowire" % autowireVersion,
      "com.lihaoyi" %% "upickle" % upickleVersion,
      "com.lihaoyi" %% "scalatags" % scalatagsVersion,
      "com.lihaoyi" %% "scalarx" % "0.3.1",
      "org.apache.httpcomponents" % "httpclient" % httpComponentsVersion,
      "org.apache.httpcomponents" % "httpmime" % httpComponentsVersion,
      "org.scalatra" %% "scalatra" % scalatraVersion,
      "org.scalatra" %% "scalatra-auth" % scalatraVersion,
      "ch.qos.logback" % "logback-classic" % "1.1.3" % "runtime",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
      "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % "container;compile",
      "org.json4s" %% "json4s-jackson" % json4sVersion,
      "org.apache.directory.api" % "api-all" % apacheDirectoryVersion,
      "com.sun.mail" % "javax.mail" % "1.5.6",
      "com.typesafe.slick" %% "slick" % "3.2.0-M2",
      "com.h2database" % "h2" % "1.4.190",
      "com.github.pathikrit" %% "better-files" % betterFileVersion,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "fr.iscpif.freedsl" %% "dsl" % freedslVersion,
      "fr.iscpif.freedsl" %% "io" % freedslVersion,
      "com.github.scopt" %% "scopt" % "3.5.0"
    )
  ),
  assemble := {
    val js = (fastOptJS in client in Compile).value
    val res = (resourceDirectory in client in Compile).value
    val cd = (classDirectory in Compile).value
    copy(js, res, cd / "webapp").data
  }
) dependsOn(shared, ext, hasher) enablePlugins (JettyPlugin)


lazy val assemble = taskKey[File]("assemble")

lazy val lab = Project(
  "lab",
  file("lab")
) settings (projectSettings: _*) enablePlugins (JavaAppPackaging) settings(
  runMain := ((runMain in Runtime) dependsOn assemble).evaluated,
  run := ((run in Runtime) dependsOn assemble).evaluated,
  assemble := {
    val js = (fastOptJS in client in Compile).value
    val res = (resourceDirectory in client in Compile).value
    val cd = (classDirectory in Compile).value
    copy(js, res, cd / "webapp").data
  },
  (stage in Universal) := {
    (stage in Universal).value
    val js = (fastOptJS in client in Compile).value
    val res = (resourceDirectory in client in Compile).value
    val std = (stagingDirectory in Universal).value
    copy(js, res, std / "webapp").data
  }
) dependsOn(server, client)

def copy(clientTarget: Attributed[File], resources: File, webappServerTarget: File) =
  clientTarget.map { ct =>
    recursiveCopy(new File(resources, "webapp"), webappServerTarget)
    recursiveCopy(ct, new File(webappServerTarget, "js/" + ct.getName))
    webappServerTarget
  }

def recursiveCopy(from: File, to: File): Unit = {
  if (from.isDirectory) {
    to.mkdirs()
    for {
      f ← from.listFiles()
    } recursiveCopy(f, new File(to, f.getName))
  }
  else if (!to.exists() || from.lastModified() > to.lastModified) {
    from.getParentFile.mkdirs
    IO.copyFile(from, to, preserveLastModified = true)
  }
}

