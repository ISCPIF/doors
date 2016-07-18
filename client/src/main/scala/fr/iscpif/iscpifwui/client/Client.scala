package fr.iscpif.doors.client

/*
 * Copyright (C) 08/06/15 // mathieu.leclaire@openmole.org
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

import fr.iscpif.doors.client.stylesheet._
import fr.iscpif.doors.ext.Data.User
import fr.iscpif.scaladget.stylesheet.all
import org.scalajs
import org.scalajs.dom
import org.scalajs.dom._
import scala.concurrent.Future
import rx._
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import fr.iscpif.scaladget.stylesheet.{all ⇒ sheet}
import sheet._
import scalatags.JsDom.tags
import scalatags.JsDom.all._

@JSExport("Client")
object Client {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val userConnection = new UserConnection

  val shutdownButton =  tags.button(topLink +++ btn_primary, `type` := "submit")("Logout")


  dom.window.sessionStorage
  @JSExport
  def connection(): Unit = {
    userConnection.render.map { r =>
      dom.document.body.appendChild(r)
    }

  }

  @JSExport
  def application(): Unit = {
    dom.document.body.appendChild(
      tags.div(
        tags.form(
          action := "/logout",
          method := "post",
          shutdownButton
        ).render,
        //TO BE CHANGED !!!
        new ServiceWall(User("Toto", "tata", "auie", "nstrnst", "strnst", "stt")).render
      ).render
    )
  }

}

object Post extends autowire.Client[String, upickle.default.Reader, upickle.default.Writer] {

  override def doCall(req: Request): Future[String] = {
    val url = req.path.mkString("/")
    val host = window.document.location.host

    ext.Ajax.post(
      url = s"http://$host/$url",
      data = upickle.default.write(req.args)
    ).map {
      _.responseText
    }
  }

  def read[Result: upickle.default.Reader](p: String) = upickle.default.read[Result](p)

  def write[Result: upickle.default.Writer](r: Result) = upickle.default.write(r)
}