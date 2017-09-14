package fr.iscpif.doors.client

import scaladget.api.{BootstrapTags => bs}
import bs._
import shared.{Api, UnloggedApi}

import scalatags.JsDom.all._
import fr.iscpif.doors.ext.Data.{ApiRep, UserData}

import scaladget.stylesheet.{all => sheet}
import scaladget.tools.JsRxTags._
import sheet._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.scalajs.dom.html
import rx._
import scalatags.JsDom


class UserEdition(user: Var[Option[UserData]] = Var(None)) {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val firstNameInput = bs.input("")(placeholder := "First name", width := "100%").render
  val lastNameInput = bs.input("")(placeholder := "Given name", width := "100%").render
  val affiliationInput = bs.input("")(placeholder := "Affiliation", width := "100%", list := "affiliations").render
  val emailInput = bs.input("")(placeholder := "Email", width := "100%").render


  val affListDefaults = Seq(
    "CNRS",
    "ISCPIF",
    "Université Paris 6"
  )

  def strToHtmlOption(s: String): JsDom.TypedTag[html.Option] = option(Seq(value := s))()

  val affUpdated: Var[Boolean] = Var(false)

  val affAutocompleteList: Var[html.Element] = Var(
    datalist(Seq(id := "affiliations"))(affListDefaults.map(strToHtmlOption):_*).render
  )

  def updateAffiliationAutocompleteList(defaultList: Seq[String] = affListDefaults): Unit = {
    // fetching DB affiliations, concat with defaultList, and render to DataList
    Post[UnloggedApi].affiliationsList().call().foreach {
      resp: ApiRep[Seq[String]] => resp match {
        case Right(dbAffiliationsList: Seq[String]) =>
          affAutocompleteList() = datalist(Seq(id := "affiliations"))(
            (defaultList ++ dbAffiliationsList).map(strToHtmlOption):_*
          ).render
          affUpdated() = true
        case Left(_) =>
      }
    }
  }

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
    val emailCheck = if (email.isEmpty) Some("The email field cannot be empty") else None
    val nameCheck = if (lastName.isEmpty) Some("The last name field cannot be empty") else None
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
    Rx {
      affUpdated() match {
        case true =>
          bs.vForm(width := "100%")(
            firstNameInput.withLabel("First name"),
            lastNameInput.withLabel("* Last name"),
            emailInput.withLabel("* Email"),
            affiliationInput.withLabel("Affiliation"),
            affAutocompleteList()
          )
        case false =>
          bs.vForm(width := "100%")(
            firstNameInput.withLabel("First name"),
            lastNameInput.withLabel("* Last name"),
            emailInput.withLabel("* Email"),
            affiliationInput.withLabel("Affiliation"),
            affAutocompleteList()
          )
      }
    }

  lazy val panelWithError = div(
    panel,
    errorPanel
  )

  // async API call to update the affiliations content
  updateAffiliationAutocompleteList()
}

