import org.scalatra.sbt.ScalatraPlugin

resolvers in ThisBuild ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"
)


def projectSettings = Seq(
  organization := "fr.iscpif",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  libraryDependencies ++= monocle,
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

val scalatraVersion = "2.4.+"
val jettyVersion = "9.3.7.v20160115"
val json4sVersion = "3.3.0"
val httpComponentsVersion = "4.5.1"
val scalatagsVersion = "0.5.4"
val autowireVersion = "0.2.5"
val upickleVersion = "0.4.1"
val apacheDirectoryVersion = "1.0.0-M33"
def jarName(version: String) = s"doors$version.jar"
val monocleVersion = "1.2.1"
val betterFileVersion = "2.15.0"


lazy val doors = project in file(".") settings(projectSettings) aggregate(ext, rest, server, client)

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
  file("rest")) settings (projectSettings: _*) settings(
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
    "fr.iscpif" %%% "scaladget" % "0.9.0-SNAPSHOT",
    "org.scala-js" %%% "scalajs-dom" % "0.9.0",
    "org.json4s" %% "json4s-jackson" % json4sVersion
  )
  ) dependsOn(shared, ext) enablePlugins (ScalaJSPlugin)

lazy val server = Project(
  "server",
  file("server")) settings (projectSettings: _*) settings (
  ScalatraPlugin.scalatraWithJRebel ++ Seq(
    unmanagedResourceDirectories in Compile <+= target(_ / "webapp"),
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
      "com.typesafe.slick" %% "slick" % "3.1.1",
      "com.h2database" % "h2" % "1.4.190",
      "com.github.pathikrit" %% "better-files" % betterFileVersion,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value)
  )) dependsOn(shared, ext, hasher) enablePlugins (JettyPlugin)


lazy val assemble = taskKey[File]("assemble")

lazy val lab = Project(
  "lab",
  file("lab")
) settings (projectSettings: _*) settings(
  assemblyJarName in assembly := jarName(version.value),
  assemblyMergeStrategy in assembly := {
    case PathList("JS_DEPENDENCIES") => MergeStrategy.rename
    case PathList("OSGI-INF", "bundle.info") => MergeStrategy.rename
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  runMain := ((runMain in Runtime) dependsOn assemble).evaluated,
  run := ((run in Runtime) dependsOn assemble).evaluated,
  assemble :=
    ((fastOptJS in client in Compile, resourceDirectory in client in Compile, classDirectory in Compile) map { (js, ressource, classDirectory) =>
      copy(js, ressource, classDirectory / "webapp").data
    }).value
  ) dependsOn (server)


lazy val app = Project("app", file("target/app")) settings (projectSettings: _*) settings(
    assemble :=
      ((fastOptJS in client in Compile, resourceDirectory in client in Compile, classDirectory in Compile, version) map { (js, ressource, classDirectory, version) =>
        val shFile = classDirectory / "doors"
        IO.write(shFile, "#!/bin/sh\njava -Xmx256M -jar " + jarName(version) + " \"$@\"")
        shFile.setExecutable(true)
        copy(js, ressource, classDirectory / "webapp").data
        classDirectory
      }).value
  ) dependsOn(client, server, lab)


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

