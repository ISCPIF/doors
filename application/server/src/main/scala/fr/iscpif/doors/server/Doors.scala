package fr.iscpif.doors.server

import better.files._

object Doors extends App {

  case class Config(settings: Option[String] = None)

  val parser = new scopt.OptionParser[Config]("doors") {
    head("doors", "0.1")

    opt[String]('s', "settings").action { (x, c) =>
      c.copy(settings = Some(File(x).lines.mkString("\n")))
    }.text("the path to the settings files")
  }

  val config = parser.parse(args, Config()).get

  config.settings match {
    case Some(s) => Launcher.run(Settings.compile(s))
    case None =>
      println("Missing argument settings.")
      parser.showUsageAsError()
  }

}
