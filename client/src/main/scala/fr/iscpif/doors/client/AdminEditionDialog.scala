package fr.iscpif.doors.client

import fr.iscpif.doors.ext.Data._
import scaladget.api.{BootstrapTags => bs}
import scaladget.stylesheet.{all => sheet}
import autowire._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import shared.Api
import scaladget.tools.JsRxTags._

import scalatags.JsDom.{TypedTag, all => tags}
import tags._
import sheet._
import rx._
import bs._
import org.scalajs.dom.raw.HTMLDivElement

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

  val users: Var[Seq[UserData]] = Var(Seq())
  val user: Var[Option[UserData]] = Var(None)
  val canModify: Var[Boolean] = Var(false)
  val userEdition: Var[Option[UserData]] = Var(None)

  lazy val modalDialog = bs.ModalDialog()

  Post[Api].loggedUser().call().foreach { uopt =>
    user() = uopt
    uopt.foreach { u =>

      //FIXME, updating API_IMPL
      //      Post[Api].canModifyPartialUser(u.id).call().foreach { ok =>
      //        canModify() = ok.authorized
      //      }
    }
  }


  val personalEditionPanel = new UserEdition(user)

  val passEdition = PassEdition.oldUser


  def save = {
    // passEdition.check
    passEdition.isStatusOK.foreach { pOK =>
      // input was not validated as passwords: do nothing (can't save)
      if (pOK) {
        user.now match {
          case Some(u: UserData) =>
            val puser = PartialUser(u.id, personalEditionPanel.firstName, personalEditionPanel.lastName, personalEditionPanel.affiliation)

          //            Post[Api].updatePartialUser(
          //              puser
          //            ).call().foreach { x =>
          //              userEdition() = None
          //              modalDialog.close
          //            }
          case _ =>
        }
      }
    }
  }


  val addUserButton = bs.button("Add", () => {
    /* Post[UnloggedApi].addUser(
       PartialUser(
         Utils.uuid,
         ""
       ),
       Password(
         Some(Utils.uuid)
       )
     ).call().foreach { u =>
       getUsers
     }*/
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

  val lineHovered: Var[Option[UserData]] = Var(None)


  def divIfAuthorized(d: UserData => TypedTag[HTMLDivElement]) = tags.div(
    Rx {
      if (canModify()) {
        user().map { u =>
          d(u)
        }.getOrElse(tags.div())
      } else tags.div()
    }
  )

  val panel = {
    Rx {
      val emptyUser = UserData.empty
      personalEditionPanel.firstNameInput.value = user().getOrElse(emptyUser).firstName
      personalEditionPanel.lastNameInput.value = user().getOrElse(emptyUser).lastName
      personalEditionPanel.affiliationInput.value = user().getOrElse(emptyUser).affiliation
      // personalEditionPanel.emailInput.value = user().getOrElse(emptyUser).email
    }
    

    val tabs = bs.tabs
      .add("Personal info",
        divIfAuthorized { u =>
          Rx { personalEditionPanel.panel() }.now
        })
      .add("Change Password",
        divIfAuthorized { u =>
          tags.div(
            passEdition.panelWithError,
            bs.button("Change password", btn_primary, () => {
              passEdition.isStatusOK.foreach { pOK =>
                if (pOK)
                //FIXME, updating API_IMPL
                // //   Post[Api].updatePassword(u.id, passEdition.newPassword).call().foreach { p =>
                  println("Pass updated")
                //   }
              }
            }))
        }
      )
      .add("Administration", userTable)

    tabs.render(stacked_pills)
  }

  case class ReactiveLine(user: UserData) {

    val render = tr(row)(
      onmouseover := { () ⇒ lineHovered() = Some(user) },
      onmouseout := { () ⇒ lineHovered() = None },
      td(colMD(6), a(s"${user.lastName} ${user.firstName}, (${user.affiliation})", pointer, onclick := { () => userEdition() = Some(user) })),
      td(colMD(5), "States ..."),
      td(colMD(1),
        tags.span(Rx {
          glyph_trash +++ pointer +++ (lineHovered() == Some(user), opaque, transparent)
        },
          onclick := { () ⇒

            //FIXME, updating API_IMPL
            //            Post[Api].removeUser(user).call().foreach { u =>
            //              getUsers
            //            }
          }
        )
      )
    )
  }


  modalDialog.header(bs.ModalDialog.headerDialogShell(h3("Admin panel") /*, addUserButton*/))

  modalDialog.body(bs.ModalDialog.bodyDialogShell(panel)
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
