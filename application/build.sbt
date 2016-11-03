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

lazy val hasher =
  ProjectRef(uri("https://github.com/Nycto/Hasher.git#v1.2.0"), "hasher")

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
      "com.github.jurajburian" %% "mailer" % "1.2.1",
      "com.typesafe.slick" %% "slick" % "3.1.1",
      "com.h2database" % "h2" % "1.4.190",
      "com.github.pathikrit" %% "better-files" % betterFileVersion
    )
  )) dependsOn(shared, ext, hasher) enablePlugins (JettyPlugin)


lazy val runLab = taskKey[File]("runlab")

val lab = Project(
  "lab",
  file("lab")
) settings (projectSettings: _*) settings(
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  assemblyJarName in assembly := jarName(version.value),
  assemblyMergeStrategy in assembly := {
    //case _ => MergeStrategy.rename
    case PathList("JS_DEPENDENCIES") => MergeStrategy.rename
    case PathList("OSGI-INF", "bundle.info") => MergeStrategy.rename
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  runMain := ((runMain in Runtime) dependsOn runLab).evaluated,
  run := ((run in Runtime) dependsOn runLab).evaluated,
  runLab :=
    ((fastOptJS in client in Compile, resourceDirectory in client in Compile, classDirectory in Compile) map { (js, ressource, classDirectory) =>
      copy(js, ressource, classDirectory / "webapp").data
    }).value
  ) dependsOn (server)

lazy val go = taskKey[Unit]("go")
lazy val toJar = taskKey[Unit]("toJar")

lazy val bootstrap = Project(
  "bootstrap",
  file("target/bootstrap")) settings (projectSettings: _*) settings(
  go <<= (fastOptJS in client in Compile, resourceDirectory in client in Compile, target in lab in Compile, scalaBinaryVersion) map { (ct, r, st, version) =>
    copy(ct, r, new File(st, s"scala-$version/webapp"))
  },
  toJar <<= (go, assembly in lab in Compile, target in lab in Compile, scalaBinaryVersion, streams) map { (_, _, st, version, s) =>
    val shFile = new File(st, s"scala-$version/doors")
    shFile.createNewFile
    IO.write(shFile, "#!/bin/sh\njava -Xmx256M -jar " + jarName(version) + " \"$@\"")
    s.log.info(s"doors has been generated in ${shFile.getParent}")
    s.log.info(s"Now launch ./doors <port>")
  }) dependsOn(client, server, lab)


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

