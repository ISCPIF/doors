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
import fr.iscpif.doors.server.db.Database
import DSL._
import dsl._
import dsl.implicits._


class UnloggedApiImpl(settings: Settings, database: Database) extends shared.UnloggedApi {

  // TODO : consult the email DB
  def isEmailUsed(email: String): ApiRep[Boolean] = db.query.email.exists(email) execute(settings, database)


  //TODO: take the first validated email or the primary one
  def resetPassword(email: String): ApiRep[Boolean] = {
    settings.resetPassword.start[M](EmailAddress(email)) execute(settings, database) match {
      case Right(_) => Right(true)
      case Left(e) => Left(DSLError)
    }
  }

  //TODO: take the first validated email or the primary one
  //def resetPassword(userID: UserID) = {
  //  import DSL.dsl._
  //  dbAndSettings.settings.resetPassword.start[M](userID)
  //}

  override def addUser(name: String, email: EmailAddress, pass: Password): ApiRep[UserID] =
    db.query.user.add(name, pass, settings.hashingAlgorithm) chain { uid =>
      settings.emailValidationInstance.start[M] (uid, email).map(_ => uid)
    } execute(settings, database)

}
