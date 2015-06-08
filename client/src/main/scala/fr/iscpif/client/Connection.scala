package fr.iscpif.client

import client.Post
import shared.Api
import fr.iscpif.scaladget.api.{BootstrapTags ⇒ bs}
import bs._
import fr.iscpif.scaladget.tools.JsRxTags._
import scalatags.JsDom.tags
import scalatags.JsDom.all._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._

/*
 * Copyright (C) 27/05/15 // mathieu.leclaire@openmole.org
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

class Connection {
  val loginInput = bs.input("")(
    placeholder := "Login",
    autofocus
  ).render

  val passwordInput = bs.input("")(
    `type` := "password",
    placeholder := "Password",
    autofocus
  ).render

  val connectButton = bs.button("Connect", btn_primary)(`type` := "submit", onclick := { () =>
    connect
  }).render

  def render = tags.form(
    loginInput,
    passwordInput,
    connectButton
  ).render

  def connect = {
    println("in connect method")
    Post[Api].connect.call().foreach { c =>
      println("Connected !")

    }
  }
}
