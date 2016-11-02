package fr.iscpif.doors.server

import fr.iscpif.doors.api.{AccessQuest, Settings}
import fr.iscpif.doors.ext.Data.{PartialUser, Password, User}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import org.eclipse.jetty.util.log._
import fr.iscpif.doors.api._
import slick.driver.H2Driver.api._
import Utils._

import scala.util.{Failure, Success}

/*
 * Copyright (C) 18/02/16 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


object Launcher {
  val arguments = "arguments"

  case class Parameter(quests: Map[String, AccessQuest])

  // this is my entry object as specified in sbt project definition
  def run(quests: => Quests, port: Int) = {
    Log.setLog(null)

    Settings.initDB
    Settings.updateDB

    val server = new Server(port)

    val context = new WebAppContext()
    context setContextPath "/"
    context.setResourceBase("webapp")
    context.setAttribute(arguments, Parameter(quests))
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[Servlet], "/")

    server.setHandler(context)

    Utils.sendEmailConfirmation("leclairem@gmail.com", "bbppp", "12233")
    query(users.result).foreach {
      println
    }

    server.start
    server.join
  }
}