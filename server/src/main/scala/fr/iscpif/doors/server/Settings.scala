package fr.iscpif.doors.server

import java.io.File
import com.typesafe.config.ConfigFactory
import slick.driver.H2Driver.api._
import database._
import scala.util.Try

/*
 * Copyright (C) 17/03/16 // mathieu.leclaire@openmole.org
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

object Settings {

  val defaultLocation = {
    val dir = new File(System.getProperty("user.home"), ".doors")
    dir.mkdirs
    dir
  }

  val dbName = "h2"
  val dbLocation = new File(defaultLocation, dbName)

  lazy val database = Database.forDriver(
    driver = new org.h2.Driver,
    url = s"jdbc:h2:/$dbLocation"
  )

  lazy val config = ConfigFactory.parseFile(new File(defaultLocation, "doors.conf"))


  def get(confKey: String) = fromConf(confKey).getOrElse("")

  def fromConf(confKey: String): Try[String] = Try {
    config.getString(confKey)
  }

  def initDB = {
    if (!new File(defaultLocation, s"$dbName.mv.db").exists) {
      database.run((users.schema ++ states.schema).create)
    }
  }

  def checkConfFile = {
    Seq("salt").foreach { e =>
      //FIXME: LOGGER
      if (fromConf(e).isFailure) println(s"$e is not defined in doors.conf")
    }
  }
}
