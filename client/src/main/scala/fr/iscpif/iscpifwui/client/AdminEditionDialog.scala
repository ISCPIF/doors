package fr.iscpif.doors.client

import fr.iscpif.doors.ext.Data.{PartialUser, PassMatchOk, Password, User}
import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import fr.iscpif.scaladget.stylesheet.{all => sheet}
import autowire._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import shared.{Api, UnloggedApi}
import fr.iscpif.scaladget.tools.JsRxTags._

import scalatags.JsDom.{all => tags}
import tags._
import sheet._
import rx._
import bs._

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

object AdminEditionDialog {
  def apply = new AdminEditionDialog
}

class AdminEditionDialog {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val users: Var[Seq[User]] = Var(Seq())
  val user: Var[Option[User]] = Var(None)
  val userEdition: Var[Option[User]] = Var(None)

  lazy val modalDialog = bs.ModalDialog()

  Post[Api].loggedUser().call().foreach { u =>
    user() = u
  }

  val personalEditionPanel = new UserEdition(user)

  val passEdition = PassEdition.oldUser


  def save = {
    val pairOfPasses = passEdition.pairOfPasses
    pairOfPasses.status match {
      // input was not validated as passwords: do nothing (can't save)
      case ok: PassMatchOk => {
        user.now match {
          case Some(u: User) =>
            val puser = PartialUser(u.id, personalEditionPanel.name, personalEditionPanel.email)
            Post[Api].modifyPartialUser(
              puser,
              pairOfPasses.newpass,
              pairOfPasses.oldpass
            ).call().foreach { x =>
              userEdition() = None
              modalDialog.close
            }
          case _ =>
        }
      }
      case _ =>
    }
  }


  val panel = {
    Rx {
      val emptyUser = User.emptyUser
      personalEditionPanel.nameInput.value = user().getOrElse(emptyUser).name
      personalEditionPanel.emailInput.value = user().getOrElse(emptyUser).email
    }

    div(
      personalEditionPanel.panel,
      passEdition.panel
    )
  }


  val addUserButton = bs.button("Add", () => {
    Post[UnloggedApi].addUser(
      PartialUser(
        java.util.UUID.randomUUID().toString,
        java.util.UUID.randomUUID().toString,
        java.util.UUID.randomUUID().toString
      ),
      Password(
        Some(java.util.UUID.randomUUID().toString)
      )
    ).call().foreach { u =>
      getUsers
    }
  })(sheet.btn_primary +++ btn_right)


  def getUsers =
    Post[Api].allUsers.call().foreach { u =>
      users() = u
    }


  val userTable = tags.table(sheet.table +++ sheet.striped)(
    tbody(
      Rx {
        for {u <- users()} yield {
          ReactiveLine(u).render
        }
      }
    )
  )

  val lineHovered: Var[Option[User]] = Var(None)

  case class ReactiveLine(user: User) {

    val render = tr(row)(
      onmouseover := { () ⇒ lineHovered() = Some(user) },
      onmouseout := { () ⇒ lineHovered() = None },
      td(colMD(4), a(user.name, pointer, onclick := { () => userEdition() = Some(user) })),
      td(colMD(7), "States ..."),
      td(colMD(1),
        tags.span(Rx {
          glyph_trash +++ pointer +++ (lineHovered() == Some(user), opaque, transparent)
        },
          onclick := { () ⇒
            Post[Api].removeUser(user).call().foreach { u =>
              getUsers
            }
          }
        )
      )
    )
  }


  modalDialog.header(bs.ModalDialog.headerDialogShell(h3("Admin panel")/*, addUserButton*/))

  modalDialog.body(bs.ModalDialog.bodyDialogShell(
    panel,
    userTable)
  )


  // dialog.footer = bs.ModalDialog.footerDialogShell(closeButton)
  modalDialog.footer(bs.ModalDialog.footerDialogShell(
    bs.buttonGroup()(
      bs.button("Save", btn_primary, () => save),
      ModalDialog.closeButton(modalDialog, btn_default, "Cancel")
    )
  ))

  getUsers
}
