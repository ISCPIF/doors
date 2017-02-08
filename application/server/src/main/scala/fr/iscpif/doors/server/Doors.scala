package fr.iscpif.doors.server

import better.files._

object Doors extends App {

  case class Config(settings: Option[Settings] = None)

  val parser = new scopt.OptionParser[Config]("scopt") {
    head("doors", "0.1")

    opt[String]('s', "settings").action { (x, c) =>
      c.copy(settings = Some(Settings.compile(File(x).lines.mkString("\n"))))
    }.text("the path to the settings files")
  }

  val config = parser.parse(args, Config()).get

  config.settings match {
    case Some(s) => Launcher.run(s)
    case None =>
      println("Missing argument settings.")
      parser.showUsageAsError()
  }

}
