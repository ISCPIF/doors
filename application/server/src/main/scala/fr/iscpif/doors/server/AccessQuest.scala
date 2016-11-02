package fr.iscpif.doors.server

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

import fr.iscpif.doors.ext.Data._
import fr.iscpif.doors.server.db._

import scala.util._

trait AccessQuest {
  def promote(database: Database)(requester: User.Id, currentState: State.Id): Try[State.Id]
  def status(database: Database)(currentState: State.Id): Try[String]
}

case class ManualValidation(validators: DbQuery[Seq[User]]) extends AccessQuest {

  def promote(database: Database)(requester: User.Id, state: State.Id): Try[State.Id] = {
    val vs = query(database)(validators).map(_.id).toSet
    if(vs.isEmpty) failure("validator not set")
    else if(!vs.contains(requester)) failure("you are not a validator")
    else Success(States.OPEN)
  }

  def status(database: Database)(state: State.Id): Try[String] = state match {
    case States.LOCKED => Try(s"Validation waiting approval of one of the users: ${query(database)(validators).map(_.name).mkString(",")}.")
    case States.OPEN => Success("Approved")
    case x => failure(s"Invalid state $x")
  }
}
