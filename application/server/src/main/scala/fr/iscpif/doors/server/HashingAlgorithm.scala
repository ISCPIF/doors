package fr.iscpif.doors.server

/*
 * Copyright (C) 18/03/16 // mathieu.leclaire@openmole.org
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

import com.roundeights.hasher.Algo
import Utils._

object HashingAlgorithm {
  val PBKDF2_METHOD = "PBKDF2"
  def apply(name: String, json: String): HashingAlgorithm =
    name match {
      case PBKDF2_METHOD => fromJSON[PBKDF2](json)
    }

  val default = PBKDF2(1000, 128)
}


sealed trait HashingAlgorithm {
  def apply(salt: String, s: String): String
  def name: String
}

case class PBKDF2(iterations: Int, keyLenght: Int) extends HashingAlgorithm {
  def name = HashingAlgorithm.PBKDF2_METHOD
  def apply(salt: String, s: String) = Algo.pbkdf2(salt, iterations, keyLenght)(s)
}


