package fr.iscpif.doors.server

import fr.iscpif.doors.ext.Data.User
import org.eclipse.jetty.server.{Server}
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import fr.iscpif.doors.server.Hashing.currentJson
import org.eclipse.jetty.util.log._

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
  // this is my entry object as specified in sbt project definition
  def main(args: Array[String]) {
    Log.setLog(null)
    val port = scala.util.Try(args(0).toInt).getOrElse(8080)

    val server = new Server(port)

    val context = new WebAppContext()
    context setContextPath "/"

    context.setResourceBase("webapp")

    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[Servlet], "/")

    server.setHandler(context)
    Settings.initDB
    Settings.checkConfFile

    //Stub for tests
    //ApiImpl.addUser(database.newUser(name = "Peter Corser", login = "corser", password = "doors",email = "peter@corser.co"))
    //ApiImpl.addUser(database.newUser(name = "Mike Horn", login = "horn", password = "doors", email = "mike@horn.ca"))
    ///

    server.start
    server.join
  }
}