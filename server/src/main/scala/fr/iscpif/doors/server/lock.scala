/**
  * Created by Romain Reuillon on 28/04/16.
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

import fr.iscpif.doors.ext.Data._
import fr.iscpif.doors.api._
import slick.driver.H2Driver.api._

object lock {

  def create(user: User.Id, lock: Lock.Id, date: Long = System.currentTimeMillis()) = {
    query(states += State(user, lock, States.Locked.id, date))
  }

}
