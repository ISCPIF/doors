import sbt._
import Keys._
import sbtassembly.AssemblyKeys._
import org.scalatra.sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import com.earldouglas.xwp._
import sbtassembly.{PathList, MergeStrategy, AssemblyPlugin}

object DoorsBuild extends Build {
  val Organization = "fr.iscpif"
  val Name = "doors"
  val Version = "0.1.0-SNAPSHOT"
  val ScalaVersion = "2.11.8"
  val scalatraVersion = "2.4.0"
  val jettyVersion = "9.3.7.v20160115"
  val json4sVersion = "3.3.0"
  val httpComponentsVersion = "4.5.1"
  val scalatagsVersion = "0.5.4"
  val autowireVersion = "0.2.5"
  val upickleVersion = "0.3.8"
  val apacheDirectoryVersion = "1.0.0-M33"
  val jarName = s"doors$Version.jar"

  lazy val ext = Project(
    "ext",
    file("ext"),
    settings = Seq(
      version := Version,
      scalaVersion := ScalaVersion,
      libraryDependencies ++= Seq(
        "org.apache.directory.api" % "api-all" % apacheDirectoryVersion
      )
    )
  ) enablePlugins (ScalaJSPlugin)

  lazy val rest = Project(
    "rest",
    file("rest"),
    settings = Seq(
      version := Version,
      organization := "fr.iscpif",
      scalaVersion := ScalaVersion,
      libraryDependencies ++= Seq(
        "org.apache.httpcomponents" % "httpclient" % httpComponentsVersion,
        "org.apache.httpcomponents" % "httpmime" % httpComponentsVersion,
        "org.json4s" %% "json4s-jackson" % json4sVersion
      )
    )
  ).dependsOn(ext)


  lazy val shared = project.in(file("./shared")).settings(
    scalaVersion := ScalaVersion
  ) dependsOn (ext)

  lazy val client = Project(
    "client",
    file("client"),
    settings = Seq(
      version := Version,
      scalaVersion := ScalaVersion,
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "autowire" % autowireVersion,
        "com.lihaoyi" %%% "upickle" % upickleVersion,
        "com.lihaoyi" %%% "scalatags" % scalatagsVersion,
        "com.lihaoyi" %%% "scalarx" % "0.2.9",
        "fr.iscpif" %%% "scaladget" % "0.8.0-SNAPSHOT",
        "org.scala-js" %%% "scalajs-dom" % "0.8.2",
        "org.json4s" %% "json4s-jackson" % json4sVersion
      )
    )
  ).dependsOn(shared, ext) enablePlugins (ScalaJSPlugin)

  lazy val server = Project(
    "server",
    file("server"),
    settings = ScalatraPlugin.scalatraWithJRebel ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      unmanagedResourceDirectories in Compile <+= target(_ / "webapp"),
      assemblyJarName in assembly := jarName,
      assemblyMergeStrategy in assembly := {
        case PathList("JS_DEPENDENCIES") => MergeStrategy.rename
        case PathList("OSGI-INF", "bundle.info") => MergeStrategy.rename
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      libraryDependencies ++= Seq(
        "com.lihaoyi" %% "autowire" % autowireVersion,
        "com.lihaoyi" %% "upickle" % upickleVersion,
        "com.lihaoyi" %% "scalatags" % scalatagsVersion,
        "com.typesafe.slick" %% "slick" % "3.1.1",
        "org.apache.httpcomponents" % "httpclient" % httpComponentsVersion,
        "org.apache.httpcomponents" % "httpmime" % httpComponentsVersion,
        "org.scalatra" %% "scalatra" % scalatraVersion,
        "ch.qos.logback" % "logback-classic" % "1.1.3" % "runtime",
        "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
        "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % "container;compile",
        "org.json4s" %% "json4s-jackson" % json4sVersion,
        "org.apache.directory.api" % "api-all" % apacheDirectoryVersion
      )
    )
  ).dependsOn(shared, ext) enablePlugins (JettyPlugin)

  lazy val go = taskKey[Unit]("go")

  lazy val toJar = taskKey[Unit]("toJar")

  lazy val bootstrap = Project(
    "bootstrap",
    file("target/bootstrap"),
    settings = Seq(
      version := Version,
      scalaVersion := ScalaVersion,
      go <<= (fullOptJS in client in Compile, resourceDirectory in client in Compile, target in server in Compile, scalaBinaryVersion) map { (ct, r, st, version) =>
        copy(ct, r, new File(st, s"scala-$version/webapp"))
      },
      toJar <<= (go, assembly in server in Compile, target in server in Compile, scalaBinaryVersion, streams) map { (_, _, st, version, s) =>
        val shFile = new File(st, s"scala-$version/doors")
        shFile.createNewFile
        IO.write(shFile, "#!/bin/sh\njava -Xmx256M -jar " + jarName + " \"$@\"")
        s.log.info(s"doors has been generated in ${shFile.getParent}")
        s.log.info(s"Now launch ./doors <port>")
      }

    )
  ) dependsOn(client, server)


  private def copy(clientTarget: Attributed[File], resources: File, webappServerTarget: File) = {
    clientTarget.map { ct =>
      recursiveCopy(new File(resources, "webapp"), webappServerTarget)
      recursiveCopy(ct, new File(webappServerTarget, "js/" + ct.getName))
    }
  }

  private def recursiveCopy(from: File, to: File): Unit = {
    if (from.isDirectory) {
      to.mkdirs()
      for {
        f ← from.listFiles()
      } recursiveCopy(f, new File(to, f.getName))
    }
    else if (!to.exists() || from.lastModified() > to.lastModified) {
      println(s"Copy file $from to $to ")
      from.getParentFile.mkdirs
      IO.copyFile(from, to, preserveLastModified = true)
    }
  }

}
