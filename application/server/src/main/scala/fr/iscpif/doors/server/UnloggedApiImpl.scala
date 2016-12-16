package fr.iscpif.doors.server

/*
 * Copyright (C) 21/10/16 // mathieu.leclaire@openmole.org
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

import fr.iscpif.doors.ext.Data._

import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.H2Driver.api._
import cats.implicits._
import fr.iscpif.doors.server.DSL._

import scala.util.{Failure, Success, Try}

class UnloggedApiImpl(settings: Settings, database: db.Database) extends shared.UnloggedApi {

//  // TODO : consult the email DB
//  def isEmailUsed(email: String): Boolean =
//    DSL.emailTable.exists(email).
//      interpret(DSL.interpreter(settings.smtp, database))

  def addUser(name: String, email: EmailAddress, pass: Password) = {
    import DSL.dsl._
    db.query.user.add(name, pass, settings.hashingAlgorithm) chain
      { uid => settings.emailValidationInstance.start[M](uid, email) } execute(settings, database)
  }

//  def resetPassword(userID: UserID) = {
//    val chronicleID = LockID(Utils.uuid)
//    val secret = Utils.uuid
//    Utils.resetPassword(database)(userID)
//    Utils.email(database)(userID).foreach { email =>
//      println("SEnd to " + email)
//      Utils.sendResetPasswordEmail(settings.smtp, settings.publicURL, email, chronicleID, secret)
//    }
//  }

}
