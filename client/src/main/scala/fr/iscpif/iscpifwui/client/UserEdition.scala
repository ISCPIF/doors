package fr.iscpif.doors.client


import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import bs._
import shared.{Api, UnloggedApi}

import scalatags.JsDom.all._
import fr.iscpif.doors.ext.Data.{PartialUser, Password, User}
import fr.iscpif.scaladget.stylesheet.{all => sheet}
import fr.iscpif.scaladget.tools.JsRxTags._
import sheet._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import rx._


class UserEdition(user: Var[Option[User]] = Var(None)) {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val nameInput = bs.input("")(placeholder := "Given name", width := "100%").render
  val emailInput = bs.input("")(placeholder := "Email", width := "100%").render

  val isPanelValid = Var(true)
  lazy val stringErrors: Var[Seq[String]] = Var(Seq())

  user.trigger {
    user.now match {
      case Some(u: User) =>
        nameInput.value = u.name
        emailInput.value = u.email
      case _ =>
    }
  }

  def name = nameInput.value

  def email = emailInput.value

  def checkData = {
    val emailCheck = if (email.isEmpty) Some("The email cannot be empty") else None
    val nameCheck = if (name.isEmpty) Some("The name cannot be empty") else None
    Post[UnloggedApi].isEmailUsed(email).call().map { b =>
      stringErrors() = (Seq(emailCheck, nameCheck) :+ (b match {
        case true => Some("This email is already used")
        case _ => None
      })).flatten
      isPanelValid() = stringErrors.now.isEmpty
      isPanelValid.now
    }
  }

  /*{
    val somePairOfPasses = passEditionDiv.pairOfPasses
    somePairOfPasses match {
      // input was not validated as passwords: do nothing (can't save)
      case None =>
        // TODO user message
        println("invalid password input: can't save")

      case Some(p) => {
        val puser = PartialUser(user.id, user.login, nameInput.value, emailInput.value)

        somePairOfPasses.foreach { pairOfPasses =>
          // add/modify
          // ----------
          // NB: both methods know how to handle password None

          if (isNewUser) {
            // user to add
            Post[Api].addUser(
              puser,
              pairOfPasses.newpass
            ).call().foreach(x => onsaved())
          }
          else {
            // pre-existing user
            Post[Api].modifyPartialUser(
              puser,
              // passwords will contain None simultaneously
              pairOfPasses.newpass,
              pairOfPasses.oldpass
            ).call().foreach(x => onsaved())
          }
        }
      }
    }
  }*/


  lazy val errorPanel = Rx {
    bs.dangerAlert("", stringErrors().mkString(", "), isPanelValid.map{b=> ()=> b})()
  }

  lazy val panel =
    bs.vForm(width := "100%")(
      nameInput.withLabel("Given name"),
      emailInput.withLabel("Email")
    )

  lazy val panelWithError = div(
    panel,
    errorPanel
  )

}

