/**
  * Created by Romain Reuillon on 02/11/16.
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  *
  */
package fr.iscpif.doors.server


import better.files.File
import fr.iscpif.doors.ext.Data._
import slick.dbio.DBIOAction
import slick.driver.H2Driver.api._
import slick.lifted.TableQuery

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util._


package object db {

  type Database = slick.driver.H2Driver.api.Database

  lazy val users = TableQuery[Users]
  lazy val chronicles = TableQuery[Chronicles]
  lazy val userChronicles = TableQuery[UserChronicles]
  lazy val emails = TableQuery[Emails]
  lazy val versions = TableQuery[Versions]
  lazy val emailConfirmations = TableQuery[EmailConfirmations]

  type DbQuery[T] = DBIOAction[T, slick.dbio.NoStream, scala.Nothing]

  def query[T](db: Database)(f: DbQuery[T]) = Await.result(db.run(f), Duration.Inf)

  def failure(s: String) = Failure(new RuntimeException(s))

  type Authorized = (Quests, UserID) => DbQuery[Boolean]

  def isAdmin: Authorized =
    (quests: Quests, uid: UserID) => {
      def adminUsers =
        for {
          c <- chronicles if c.lock === "admin"
          uc <- userChronicles.filter { uc=> uc.userID === uid.id && uc.chronicleID === c.chronicleID }
        } yield uc.userID

      adminUsers.result.map {
        _.contains(uid.id)
      }
    }

  lazy val dbVersion = 1


  //lazy val dbName = "h2"


  //def configFile = defaultLocation / "doors.conf"

  //  def saltConfig = "salt"
  //
  //  def adminLogin = "adminLogin"
  //
  //  def adminPass = "adminPass"
  //
  //  def smtpHostName = "smtpHostName"
  //
  //  def smtpPort = "smtpPort"

  //  lazy val config = ConfigFactory.parseFile(configFile.toJava)

  //  def get(confKey: String) = fromConf(confKey)
  //
  //  def fromConf(confKey: String): Try[String] = Try {
  //    config.getString(confKey)
  //  }

  def updateDB(db: Database) = {
    val addVersionQuery = DBIO.seq(versions += Version(dbVersion))
    val v = query(db)(versions.result)

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

  def initDB(location: File) = {

    lazy val db: Database = Database.forDriver(
      driver = new org.h2.Driver,
      url = s"jdbc:h2:/${location}"
    )
    def dbWorks =
      Try { Await.result(db.run(versions.length.result), Duration.Inf) } match {
        case Failure(_) ⇒ false
        case Success(_) ⇒ true
      }

    if (!dbWorks)
      query(db)((
        users.schema ++
          chronicles.schema ++
          userChronicles.schema ++
          emails.schema ++
          versions.schema ++
          emailConfirmations.schema).create)

    db
  }


}
