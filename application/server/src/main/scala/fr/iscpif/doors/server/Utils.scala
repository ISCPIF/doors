package fr.iscpif.doors.server
//
//import javax.mail.internet.InternetAddress
//import javax.naming.OperationNotSupportedException
//
import fr.iscpif.doors.ext.Data
//import fr.iscpif.doors.server.db.States
import org.json4s.jackson.JsonMethods._
import org.json4s._
//import slick.driver.H2Driver.api._
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.{Await, Future}
//import scala.concurrent.duration.Duration
//import scala.util._
//import db._
//
///*
// * Copyright (C) 18/03/16 // mathieu.leclaire@openmole.org
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
object Utils {
  protected implicit val jsonFormats: Formats = DefaultFormats.withBigDecimal

  implicit class ToJsonDecorator(x: Any) {
    def toJson = pretty(Extraction.decompose(x))
  }

  def fromJSON[T: Manifest](s: String) = parse(s).extract[T]

//
//  implicit def throwableToEmailDeliveringError(t: Throwable): EmailDeliveringError =
//    EmailDeliveringError(t.getMessage, t.getStackTrace.map {
//      _.toString
//    })
//
//  def toUser(pUser: PartialUser, pass: Password, salt: String): User =
//    User(pUser.id, Password(Hashing(pass.password, salt)), pUser.name, Hashing.currentMethod, Hashing.currentParametersJson)
//
//  def connect(database: db.Database)(email: String, password: String, salt: String): Option[User] =
//    runQuery(database)(
//      (for {
//        e <- emails if (e.email === email)
//        uc <- userChronicles if (e.chronicleID === uc.chronicleID)
//        u <- users if (u.id === uc.userID)
//      } yield (u)).result.headOption).filter {
//      _.password == Hashing(password, salt: String)
//    }
//
//
////  def isSecretConfirmed(database: db.Database)(secret: String, chronicleID: String): Boolean =
////    !runQuery(database)(secrets.filter { ec =>
////      ec.chronicleID === chronicleID && ec.secret === secret && ec.deadline >= System.currentTimeMillis
////    }.result).isEmpty
//
//  /* def isMatching(user: User, email: String) = (for {
//     (e, uc) <- emails join userChronicles on (_.chronicleID === _.chronicleID) if (e.email === email && uc.userID === user.id)
//   } yield ()).length > 0*/
//
//
//  // Add user
//
//
//  // Add email
////  def emailAddQueries(database: db.Database)(userID: UserID, email: String, chronicleID: Option[ChronicleID] = None, secret: Option[String] = None) = {
////    val doesEmailExist = query(database)(emails.filter {
////      _.email === email
////    }.result)
////    if (doesEmailExist.isEmpty) {
////      val cID = chronicleID.getOrElse(ChronicleID(uuid))
////      DBIO.seq(
////        emails += Email(cID, email),
////        // Two days to confirm the new email
////        secrets += Secret(cID, secret.getOrElse(uuid), System.currentTimeMillis + 172800000),
////        chronicleAddQueries(cID, userID, locks.EMAIL_VALIDATION, States.LOCKED)
////      )
////    } else DBIO.seq()
////  }
//
//  //def addEmail(database: db.Database)(userID: UserID, email: String) = query(database)(emailAddQueries(database)(userID, email))
//
//
//
//  def resetPasswordQueries(userID: UserID, chronicleID: Option[LockID] = None, secret: Option[String] = None) = {
//    val cID = chronicleID.getOrElse(LockID(uuid))
//    //One day to reset the pass
//    DBIO.seq(
//      secrets += Secret(cID, secret.getOrElse(uuid), 86400000),
//      chronicleAddQueries(cID, userID, locks.RESET_PASSWORD, States.LOCKED)
//    )
//  }
//
//  def resetPassword(database: db.Database)(userID: UserID, chronicleID: Option[LockID] = None, secret: Option[String] = None) = {
//    runQuery(database)(resetPasswordQueries(userID, chronicleID, secret))
//  }
//
//  // Add chronicle
//  def chronicleAddQueries(chronicleID: LockID, userID: UserID, lockID: Lock.Id, stateID: State.Id) = DBIO.seq(
//    chronicles += Lock(chronicleID, lockID, stateID, System.currentTimeMillis, None),
//    userChronicles += UserLock(userID, chronicleID)
//  )
//
//  def addChronicle(database: db.Database)(chronicleID: LockID, userID: UserID, lockID: Lock.Id, stateID: State.Id) =
//    runQuery(database)(chronicleAddQueries(chronicleID, userID, lockID, stateID))
//
//
//  def hasAdmin(database: db.Database): Boolean = {
//    runQuery(database)(chronicles.filter { s =>
//      s.lock === locks.ADMIN
//    }.result).length > 0
//  }
//
//

//
////  def sendEmailConfirmation[M[_]](
////    publicURL: String,
////    to: String,
////    chronicleID: ChronicleID,
////    secret: String)(implicit emailM: DSL.Email[M]): M[Unit] = {
////
////    val secretURL = s"${publicURL}/emailvalidation?chronicle=${chronicleID.id}&secret=$secret"
////    val secretLink = s"<a href=${secretURL}>${secretURL}</a>"
////
////    emailM.send(
////      to,
////      "[DOORS] Email confirmation",
////      s"Hi,<br>Please click on the following link to confirm this email address !<br> $secretLink <br><br>The DOORS team")
////  }
//
////  def sendResetPasswordEmail(
////    smtp: SMTPSettings,
////    publicURL: String,
////    email: String,
////    chronicleID: ChronicleID,
////    secret: String) = {
////
////    val secretURL = s"${publicURL}/resetPassword?chronicle=${chronicleID.id}&secret=$secret"
////    val secretLink = s"<a href=${secretURL}>${secretURL}</a>"
////
////
////    DoorsMailer.send(
////      smtp,
////      "DOORS | Reset password",
////      s"Hi,<br>Please click on the following link to reset your password !<br> $secretLink <br><br>The DOORS team",
////      email
////    )
////  }
//
//

  def secretLink(publicURL: String, secret: String, lock: Data.LockID) = {
    val secretURL = s"${publicURL}/emailvalidation?lock=${lock.id}&$secret=$secret"
     s"<a href=${secretURL}>${secretURL}</a>"
  }

  def uuid = java.util.UUID.randomUUID.toString
  def now = System.currentTimeMillis()

}

