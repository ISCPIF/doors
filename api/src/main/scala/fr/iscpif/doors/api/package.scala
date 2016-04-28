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
package fr.iscpif.doors

import fr.iscpif.doors.ext.Data
import fr.iscpif.doors.ext.Data.User
import slick.dbio.DBIOAction
import slick.lifted.TableQuery

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import slick.driver.H2Driver.api._

import scala.util.Failure

package object api {

  lazy val users = TableQuery[Users]
  lazy val states = TableQuery[States]

  lazy val db = Database.forDriver(
    driver = new org.h2.Driver,
    url = s"jdbc:h2:/${Settings.dbLocation}"
  )

  type DbQuery[T] = DBIOAction[T, slick.dbio.NoStream, scala.Nothing]

  def query[T](f: DbQuery[T]) = Await.result(db.run(f), Duration.Inf)

  def failure(s: String) = Failure(new RuntimeException(s))



}
