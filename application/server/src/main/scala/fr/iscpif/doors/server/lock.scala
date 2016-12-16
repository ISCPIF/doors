package fr.iscpif.doors.server

/*
 * Copyright (C) 17/03/16 // mathieu.leclaire@openmole.org
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

import fr.iscpif.doors.ext.Data
import fr.iscpif.doors.server.db._

import scala.util._
import cats._
import cats.data._
import cats.implicits._
import fr.iscpif.doors.ext.Data.LockID
import db.dbIOActionIsMonad
import squants.time._
import squants.time.TimeConversions._
import DSL._

object lock {

  lazy val registration = LockID("registration")

  object EmailValidation {

    case class Info(secret: String, secretLink: String)

    case class Email(subject: String, content: String)

    def defaultEmail(info: Info) =
      EmailValidation.Email(
        subject = "[DOORS] Email confirmation",
        content = s"Hi,<br>Please click on the following link to confirm this email address !<br> ${info.secretLink} <br><br>The DOORS team"
      )

    sealed trait UnlockError
    case object DeadLineNotFound extends UnlockError
    case object SecretExpired extends UnlockError
    case object EmailNotFound extends UnlockError
  }

//
// val startQuest = EmailValidation() -- ManualValidation("")

//  val form512 = FormValidation(iden"Form512")
//
//  val gargantext =
//    startQuest --
  //   (form512 && FormValidation("Gargantext5")) --
  //     (ManualValidation(validator = query(firstName = "Alexandre", famillyName = "Delanoe"), id = "GargantextMailValditaion"))
//

  case class EmailValidation(
                              lockId: Data.EmailAddress => Data.LockID = e => Data.LockID(e.value),
                              confirmationDelay: Time = Days(2),
                              generateEmail: EmailValidation.Info => EmailValidation.Email = EmailValidation.defaultEmail)(publicURL: String) {

    private def sendMail[M[_]](publicURL: String, emailAddress: Data.EmailAddress)(secret: String)(implicit mailM: DSL.Email[M]) = {
      val lockIdVal = lockId(emailAddress)
      val emailContent = generateEmail(EmailValidation.Info(secret, Utils.secretLink(publicURL, secret, lockIdVal)))
      mailM.send(emailAddress.value, emailContent.subject, emailContent.content)
    }

    def start[M[_] : Monad](uid: Data.UserID, emailAddress: Data.EmailAddress)(implicit mailM: DSL.Email[M]) = {
      val lockIdVal = lockId(emailAddress)
      def insertEmail =
        query.lock.exists(emailAddress, lockIdVal) map { r =>
          if(r) DB.pure(())
          else db.query.email.add(uid, emailAddress, lockIdVal)
        }

      for {
        _ <- insertEmail
        secret <- query.secret.add(lockIdVal, Utils.now + confirmationDelay.toMillis)
      } yield secret
    } effect { secret => sendMail[M](publicURL, emailAddress)(secret) }

    def unlock[M[_]: Monad](secret: String)(implicit errorM: DSL.SignalError[M]) = {
      def processEmail(email: Option[db.Email]) = {
        email.map(e => lockId(e.address)) match {
          case None => DB.pure[Either[EmailValidation.UnlockError, Unit]](Left(EmailValidation.EmailNotFound))
          case Some(lid) =>
            for {
              deadline <- query.secret.deadline(secret, lid)
              r <- processDeadline(deadline, lid)
            } yield r
        }
      }
      def processDeadline(deadline: Option[Time], lockID: Data.LockID): DB[Either[EmailValidation.UnlockError, Unit]] =
        deadline match {
          case None => DB.pure[Either[EmailValidation.UnlockError, Unit]](Left(EmailValidation.DeadLineNotFound))
          case Some(deadline) =>
            if (deadline.toMillis > System.currentTimeMillis()) DB.pure[Either[EmailValidation.UnlockError, Unit]] (Left(EmailValidation.SecretExpired))
            else query.lock.progress(lockID, Data.LockState.locked).map(e => Right(e))
        }

        for {
          email <- query.secret.email(secret)
          res <- processEmail(email.headOption)
        } yield res
    } effect { e =>
      e match {
        case Left(e) => errorM.error(e)
        case Right(_) => ().pure[M]
      }
    }

//
//    def revalidate[M[_] : Monad](emailAddress: String)(implicit mailM: DSL.Email[M]) =
//      DSL.Action.db { scheme =>
//        query.lock.forEmail(scheme)(emailAddress).result.flatMap {
//          _.headOption match {
//            case None => db.error(EmailValidation.EmailNotFound)
//            case Some(e) =>
//              query.secret.add(scheme)(e.id, chronicleID, Utils.now + confirmationDelay.toMillis).map(Ior.right)
//          }
//        }
//      } map (_.map(sendMail[M](publicURL, emailAddress)))


    //    def chronicId =
    //      mailTableM.chronicForEmail(email).flatMap {
    //        case Some(chronicId) => chronicId.pure[M]
    //        case None =>
    //          for {
    //            id <- chronicM.generateId
    //            _ <- mailTableM.add(uid, email, id)
    //          } yield id
    //      }
    //
    //    for {
    //      id <- chronicId
    //      now <- dateM.now
    //      secret <- secretM.generateSecret
    //      _ <- secretM.setSecret(email, id, now + (2 days).toMillis, secret)
    //      _ <- Utils.sendEmailConfirmation(publicURL, email, id, secret)
    //      _ <- statusM.set(db.States.LOCKED, id, lockId)
    //    } yield ()
    //  }

    //  // FIXME manage error and separate return types for each action
    //  def next[M[_]: Monad](action: Action)(implicit mailTableM: DSL.EmailTable[M],
    //                                        mailM: DSL.Email[M],
    //                                        chronicM: DSL.ChronicTable[M],
    //                                        secretM: DSL.SecretTable[M],
    //                                        dateM: DSL.Date[M],
    //                                        statusM: DSL.Status[M]) = action match {
    //    case Start(uid, email, publicURL) =>
    //
    //    case Unlock(email, secret, chronicID) =>
    //      secretM.checkSecret(secret, chronicID) flatMap {
    //        case true => statusM.set(db.States.OPEN, chronicID, lockId)
    //        case false => ().pure[M]
    //      }
    //    case Revalidate(chronicId, publicURL) =>
    //      mailTableM.emailForChronic(chronicId) flatMap {
    //        case Some(email) =>
    //          for {
    //            now <- dateM.now
    //            secret <- secretM.generateSecret
    //            _ <- secretM.setSecret(email.email, chronicId, now + confirmationDelay.toMillis, secret)
    //            _ <- Utils.sendEmailConfirmation(publicURL, email.email, chronicId, secret)
    //            _ <- statusM.set(db.States.LOCKED, chronicId, lockId)
    //          } yield ()
    //        case None =>
    //          //FIXME manage errors
    //          ().pure[M]
    //      }
    //
    //  }


    //      chronicForMail (email) {
    //        case Some(chronicId) => chronicId
    //        case None =>
    //          id = Generer chronic id
    //          Ajoute mail dans base(email, id)
    //          id
    //      }
    //      set sercret(mail, chronicId)
    //      envoie mail avec secret(mail, chronicId)
    //      setStatus(LOCKED)
    //    case Unlock(email, secret, chronicID) =>
    //      if(secretOK(email, secret, chronicID)) setStatus(OPEN) else Noop
    //    case Revalidate(chronicId) =>
    //      set sercret(mail, chronicId)
    //      envoie mail avec secret(mail, chronicId)
    //      setStatus(LOCKED)
  }

  object Test {

    /*   object EmailValidation {
       sealed trait Action
       case class Start(uid: UserID, email: Email) extends Action
       case class Unlock(email: Email, secret: String, chronicleId: ChronicleID) extends Action
       case class Revalidate(chronicId) extends Action
     }

       def manualValidation = {
         case NEW(uid) => setStatus(LOCKED)
         case LOCKED(validator) => if(validatorOK(validator)) setStatus(OPEN) Noop
         case OPEN =>
       }

       */

  }

  //
  //
  //trait AccessQuest {
  //  def promote(database: Database)(requester: UserID, currentState: State.Id): Try[State.Id]
  //  def status(database: Database)(currentState: State.Id): Try[String]
  //}
  //
  //case class ManualValidation(validators: DbAction[Seq[User]]) extends AccessQuest {
  //
  //  def promote(database: Database)(requester: UserID, state: State.Id): Try[State.Id] = {
  //    val vs = runQuery(database)(validators).map(_.id).toSet
  //    if(vs.isEmpty) failure("validator not set")
  //    else if(!vs.contains(requester)) failure("you are not a validator")
  //    else Success(States.OPEN)
  //  }
  //
  //  def status(database: Database)(state: State.Id): Try[String] = state match {
  //    case States.LOCKED => Try(s"Validation waiting approval of one of the users: ${runQuery(database)(validators).map(_.name).mkString(",")}.")
  //    case States.OPEN => Success("Approved")
  //    case x => failure(s"Invalid state $x")
  //  }
  //}
}