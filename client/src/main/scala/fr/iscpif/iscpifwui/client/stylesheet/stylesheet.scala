package fr.iscpif.doors.client

import fr.iscpif.scaladget.stylesheet.{all ⇒ sheet}
import scalatags.JsDom.all._
import sheet._

/*
 * Copyright (C) 27/04/16 // mathieu.leclaire@openmole.org
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

package object stylesheet {


  // SERVICE WALL
  lazy val user: ModifierSeq = Seq(
    textAlign := "center",
    fontSize := 15,
    color := "white",
    relativePosition,
    sheet.paddingTop(10)
  )

  lazy val serviceBox: ModifierSeq = Seq(
    maxWidth := 220,
    height := 116
  )

  lazy val serviceImage: ModifierSeq = Seq(
    maxHeight := 110
  )

  lazy val wall: ModifierSeq = Seq(
    relativePosition,
    width := "75%",
    top := 50
  )

  lazy val top100: ModifierSeq = Seq(
    absolutePosition,
    top := 100
  )

  // CONNECTION
  lazy val topLink: ModifierSeq = Seq(
    fontSize := (20),
    sheet.marginRight(10),
    absolutePosition,
    right := 0,
    top := 7,
    color := "white",
    pointer,
    zIndex := 1
  )

  lazy val connectionFailed: ModifierSeq = Seq(
    fontSize := 18,
    fontWeight := "bold",
    color := "CC3A36"
  )

  lazy val logoISC: ModifierSeq = Seq(
    fixedPosition,
    width := 200,
    bottom := 15,
    right := 15
  )
}
