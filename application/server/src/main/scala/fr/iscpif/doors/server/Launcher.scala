package fr.iscpif.doors.server

import java.util.Properties

import fr.iscpif.doors.ext.Data.{PartialUser, Password, User, UserID}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import org.eclipse.jetty.util.log._
import db._
import slick.driver.H2Driver.api._

import scala.util.{Failure, Success, Try}

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

  // this is my entry object as specified in sbt project definition
  def run(settings: Settings) = {
    Log.setLog(null)

    val database = db.initDB(settings.dbLocation)
    db.updateDB(database)

    val server = new Server(settings.port)
    val args = new Servlet.Arguments(settings, database)

    val context = new WebAppContext()
    context setContextPath "/"

    val webapp = getClass.getClassLoader.getResource("webapp").toExternalForm
    context.setResourceBase(webapp)

    context.setAttribute(arguments, args)
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[Servlet], "/")

    server.setHandler(context)

    db.query(database)(users.result).foreach {
      println
    }

    //val api = new UnloggedApiImpl(settings, database)
    //api.resetPassword(UserID("6e601bad-067c-4721-945d-9c9cccd1eb22"))
    server.start
    server.join


  }
}