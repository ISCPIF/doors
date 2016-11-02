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
import fr.iscpif.doors.server.Hashing.{HashingAlgorithm, UnsaltAlgorithm}
import Utils._

object Hashing {

  trait UnsaltAlgorithm {
    def apply(salt: String): HashingAlgorithm
  }

  trait HashingAlgorithm {
    def apply(s: String): String
  }

  val PBKDF2_METHOD = "PBKDF2"
  val currentMethod = PBKDF2_METHOD
  val currentParameters = PBKDF2(1000, 128)
  val currentParametersJson = currentParameters.toJson

  def apply[T](s: String, salt: String, hash: T = currentParameters)(implicit hashing: HashingMethod[T]) = (hashing(hash)(salt)(s))
}

sealed trait HashingMethod[T] {
  def apply(t: T): Hashing.UnsaltAlgorithm
}

//PBKDF2
case class PBKDF2(iterations: Int, keyLenght: Int)

object PBKDF2 {
  implicit def hashingMethod = new HashingMethod[PBKDF2] {
    def apply(t: PBKDF2): UnsaltAlgorithm =
      new UnsaltAlgorithm {
        override def apply(salt: String): HashingAlgorithm = new HashingAlgorithm {
          def apply(s: String) = Algo.pbkdf2(salt, t.iterations, t.keyLenght)(s)
        }
      }
  }
}

