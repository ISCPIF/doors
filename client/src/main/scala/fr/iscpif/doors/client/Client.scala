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
import fr.iscpif.doors.ext.Data.UserData
import org.scalajs.dom
import org.scalajs.dom._
import shared.Api
import fr.iscpif.doors.ext.route._

import scala.concurrent.Future
import rx._

import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import fr.iscpif.scaladget.stylesheet.{all => sheet}
import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import org.scalajs.dom.html.Element
import sheet._

import scalatags.JsDom.tags
import scalatags.JsDom.all._

@JSExport("Client")
object  Client {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val userConnection = new UserConnection

  val shutdownButton = tags.button(topLink +++ btn_primary, `type` := "submit")("Logout")


  dom.window.sessionStorage

  @JSExport
  def connection(): Unit = {
    bs.withBootstrapNative(userConnection.render)
  }

  @JSExport
  def emailValidatedMessage(): Unit = {
    new MessageDisplay("Your email was successfully validated !").render
  }

  @JSExport
  def application(): Unit = {
    Post[Api].loggedUser.call().foreach {
      _ match {
        case Some(u: UserData) =>
          bs.withBootstrapNative(
            tags.div(
              tags.form(
                action := logoutRoute,
                method := "post",
                shutdownButton
              ).render,
              new ServiceWall(u).render
            ).render
          )
        case _ =>
      }
    }
  }

  // Body:  factorizable with MessageDisplay
  def panelInBody(heading: String, content: Element) =
    div(sheet.panel +++ panelDefault +++ Seq(boxShadow := "0 6px 12px rgba(0,0,0,.175)"))(
      div(panelHeading +++ Seq(fontWeight := "bold"))(heading),
      div(panelBody)(content)
    )

  @JSExport
  def askNewPassword(): Unit = {

    // Content: the input ("newUser" form because we don't want the previous pass)
    val pE = PassEdition.newUser

    bs.withBootstrapNative(
      tags.div()(
        tags.div(wall +++ Seq(width := "400px", margin := "auto"))(
          panelInBody(
            "Please enter the new password for your account",
            tags.div(
              pE.panelWithError.render,
              bs.button("OK", btn_primary, () => {
                pE.isStatusOK.foreach { passOK =>
                  if (passOK) {
                    val currentGetArgs = dom.window.location.search.substring(1)
                    val senddata = currentGetArgs+"&newpass="+pE.newPassword
                    // send the form with the secret added inside
                    dom.ext.Ajax.post(
                      dom.window.location.origin + dom.window.location.pathname,
                      data = senddata,
                      headers = Map("Content-Type" -> "application/x-www-form-urlencoded; charset=UTF-8")
                    )
                    // POSS: wait for response + conditions ?
                    new MessageDisplay("Your password will be updated in a few seconds").render
                  }
                }
              })
            ).render
          ).render
        ).render
      ).render
    )
  }
}

object Post extends autowire.Client[String, upickle.default.Reader, upickle.default.Writer] {

  override def doCall(req: Request): Future[String] = {
    val url = req.path.mkString("/")
    val host = window.document.location.host
    val protocol = window.document.location.protocol

    ext.Ajax.post(
      url = s"$protocol//$host/$url",
      data = upickle.default.write(req.args)
    ).map {
      _.responseText
    }
  }

  def read[Result: upickle.default.Reader](p: String) = upickle.default.read[Result](p)

  def write[Result: upickle.default.Writer](r: Result) = upickle.default.write(r)
}