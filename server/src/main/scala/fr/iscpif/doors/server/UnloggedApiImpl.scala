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

import fr.iscpif.doors.api._
import fr.iscpif.doors.ext.Data.{PartialUser, Password, State}
import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.H2Driver.api._

class UnloggedApiImpl extends shared.UnloggedApi {

  // TODO : consult the email DB
  def isEmailUsed(email: String): Boolean = !query(users.filter { u =>
    u.email === email
  }.result).isEmpty


  def addUser(partialUser: PartialUser, pass: Password): Unit = {
    val someUser = Utils.toUser(partialUser, pass)
    val currentTime = System.currentTimeMillis
    someUser.foreach { u =>
      val addUser =
        for {
          _ <- users += u
          _ <- states += State(u.id, locks.REGISTRATION, States.OPENED, currentTime)
        } yield ()

      val admins =
        states.filter { s => s.lock === locks.ADMIN }.result.map(_.size)

      val transaction = admins.flatMap {
        case 0 =>
          for {
            _ <- addUser
            _ <- states += State(u.id, locks.ADMIN, States.OPENED, currentTime)
          } yield ()
        case _ => addUser
      }

      db.run(transaction.transactionally)
    }
  }
}
