package fr.iscpif.doors.api

import java.util.UUID

import com.typesafe.config.ConfigFactory
import slick.driver.H2Driver.api._
import fr.iscpif.doors.ext.Data.{AdminUser, Version}

import scala.util._

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

import slick.driver.H2Driver.api._
import better.files._

object Settings {

  val defaultLocation = {
    val dir = System.getProperty("user.home") / ".doors"
    dir.toJava.mkdirs
    dir
  }

  lazy val dbName = "h2"
  lazy val dbLocation = defaultLocation / dbName
  lazy val dbVersion = 2

  def configFile = defaultLocation / "doors.conf"

  def saltConfig = "salt"
  def adminLoginConfig = "adminLogin"
  def adminPassConfig = "adminPass"

  lazy val config = ConfigFactory.parseFile(configFile.toJava)

  def get(confKey: String) = fromConf(confKey).getOrElse("")

  def fromConf(confKey: String): Try[String] = Try {
    config.getString(confKey)
  }

  def updateDB = {
    val addVersionQuery = DBIO.seq(versions += Version(dbVersion))
    val v = query(versions.result)

    val updateQuery = {
      if (v.exists(_.id < dbVersion)) {
        println("TODO: UPDATE DB")
        addVersionQuery
      }
      else if (v.isEmpty) addVersionQuery
      else DBIO.seq()
    }

    db.run(updateQuery)
  }

  def initDB = {
    if (!(defaultLocation / s"$dbName.mv.db").exists) {
      query((users.schema ++ chronicles.schema ++ userChronicles.schema ++ emails.schema ++ versions.schema ++ emailConfirmations.schema).create)
    }
  }

  def salt: String = {
    Try(get(saltConfig)) match {
      case Success(s) => s
      case Failure(_) =>
        val s = UUID.randomUUID.toString
        configFile << s"$saltConfig = $s"
        s
    }
  }

  val adminUser: Try[AdminUser] = Try((get(adminLoginConfig), get(adminPassConfig))).map
    { case (l, p) => AdminUser(l, p) }

}
