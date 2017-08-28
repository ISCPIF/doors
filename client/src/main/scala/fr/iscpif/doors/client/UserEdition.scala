package fr.iscpif.doors.client

import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import bs._
import shared.{Api, UnloggedApi}

import scalatags.JsDom.all._
import fr.iscpif.doors.ext.Data.UserData
import fr.iscpif.scaladget.stylesheet.{all => sheet}
import fr.iscpif.scaladget.tools.JsRxTags._
import sheet._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import rx._


class UserEdition(user: Var[Option[UserData]] = Var(None)) {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val firstNameInput = bs.input("")(placeholder := "First name", width := "100%").render
  val lastNameInput = bs.input("")(placeholder := "Given name", width := "100%").render
  val affiliationInput = bs.input("")(placeholder := "Affiliation", width := "100%", list := "affiliations").render
  val emailInput = bs.input("")(placeholder := "Email", width := "100%").render


  // TODO check if we want an AJAX API here ?
  val autocompletionList = datalist(Seq(id := "affiliations"))(
    option(Seq(value := "CNRS"))(),
    option(Seq(value := "ISCPIF"))(),
    option(Seq(value := "Université Paris 6"))()
  ).render


  lazy val stringErrors: Var[Seq[String]] = Var(Seq())

  val isPanelValid = Rx {
    stringErrors().isEmpty
  }

  user.trigger {
    user.now match {
      case Some(u: UserData) =>
        lastNameInput.value = u.lastName
        firstNameInput.value = u.firstName
        affiliationInput.value = u.affiliation
      case _ =>
    }
  }

  def lastName = lastNameInput.value
  def firstName = firstNameInput.value
  def affiliation = affiliationInput.value
  def email = emailInput.value

  def checkData = {
    val emailCheck = if (email.isEmpty) Some("The email cannot be empty") else None
    val nameCheck = if (lastName.isEmpty) Some("Your last name cannot be empty") else None
    Post[UnloggedApi].isEmailUsed(email).call().map { b =>
      stringErrors() = (Seq(emailCheck, nameCheck) :+ (b match {
        case Right(true) => Some("This email is already used")
        case _ => None
      })).flatten
      isPanelValid.now
    }
  }

  lazy val errorPanel = Rx {
    bs.dangerAlerts("", stringErrors(), isPanelValid)()
  }

  lazy val panel =
    bs.vForm(width := "100%")(
      firstNameInput.withLabel("First name"),
      lastNameInput.withLabel("* Last name"),
      emailInput.withLabel("* Email"),
      affiliationInput.withLabel("Affiliation"),
      autocompletionList
    )

  lazy val panelWithError = div(
    panel,
    errorPanel
  )

}

