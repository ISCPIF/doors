package fr.iscpif.doors.server.db

import java.util.UUID

import fr.iscpif.doors.ext.Data
import fr.iscpif.doors.ext.Data.{ApiRep, LockID, UserID}
import fr.iscpif.doors.server.{HashingAlgorithm, Settings, Utils, db}
import squants.time.TimeConversions._
import fr.iscpif.doors.server.DSL._

import scala.concurrent.duration.Deadline

object query {

  import slick.driver.H2Driver.api._
  import scala.concurrent.ExecutionContext.Implicits.global
  import fr.iscpif.doors.ext.Data

  object lock {

    def exists(email: Data.EmailAddress, lockId: Data.LockID) = DB { scheme =>
      (scheme.emails.filter(e => e.address === email.value && e.lockID === lockId.id).size > 0)
    }

    // FIXME side effect current time
    def create(userId: Data.UserID, lockId: Data.LockID, statusId: Data.StateID = Data.LockState.locked) = DB { scheme =>
      for {
       _ <- scheme.locks += db.Lock(lockId, statusId, System.currentTimeMillis() milliseconds, None)
       _ <- scheme.userLocks += db.UserLock(userId, lockId)
      } yield lockId
    }

    def getFromSecretStr(secret: String) = DB { scheme =>
      (for {
        s <- scheme.secrets.filter(s => s.secret === secret)
        l <- scheme.locks.filter(l => l.id === s.lockID)
      } yield l.id).result.head
    }

    def progress(lockId: Data.LockID, statusId: Data.StateID) = DB { scheme =>
      scheme.locks += db.Lock(lockId, statusId, System.currentTimeMillis() milliseconds, None)
    }

    //
    //      Utils.emails(database)(userId) match {
    //              case Seq() => EmailStatus.Contact
    //              case _ => EmailStatus.Other
    //            }
    //
    //          db.query(database)(db.emails += Data.Email(chronic, email, status))

  }

  object email {
    def get(userId: Data.UserID) = DB { scheme =>
      def request =
        for {
          uc <- scheme.userLocks.filter(_.userID === userId.id)
          e <- scheme.emails.filter(_.lockID === uc.lockID)
        } yield e

      request.result
    }


    def add(userId: Data.UserID, email: Data.EmailAddress, lockId: Data.LockID) =
      for {
        es <- query.email.get(userId)
        emailStatus = if (true) db.EmailStatus.Contact else db.EmailStatus.Other
        lockId <- lock.create(userId, lockId)
        _ <- DB { scheme =>
          scheme.emails += db.Email(lockId, email, if (true) db.EmailStatus.Contact else db.EmailStatus.Other)
        }
      } yield lockId

    def exists(emailAddress: String) =
      DB { sheme =>
        def request = for {
          e <- sheme.emails.filter(_.address.toLowerCase === emailAddress.toLowerCase)
        } yield e

        request.result.map {
          _.size > 0
        }
      }
  }

  object secret {

    def add(lockID: LockID, secret: String, deadline: Long): DB[String] = DB { scheme =>
      for {
        _ <- scheme.secrets += db.Secret(lockID, secret, deadline)
      } yield secret
    }

    def add(lockId: Data.LockID, deadLine: Long): DB[String] = add(lockId, Utils.uuid, deadLine)

    def email(secret: String) = DB { scheme =>
      def request =
        for {
          lockId <- scheme.secrets.filter(s => s.secret === secret)
          e <- scheme.emails.filter(e => e.lockID === lockId.lockID)
        } yield e

      request.result
    }

    def deadline(secret: String, lockId: Data.LockID) = DB { scheme =>
      scheme.secrets.filter(s => s.secret === secret && s.lockID === lockId.id).map(_.deadline).result.headOption.map(_.map(_ milliseconds))
    }


    //    object SecretTable {
    //      def interpreter(database: Database) = new Interpreter[Id] {
    //        def interpret[_] = {
    //          case setSecret(email, chronic, deadLine, secret) =>
    //            db.runQuery(database)(db.secrets += Data.Secret(chronic, secret, deadLine))
    //            ()
    //          case checkSecret(secret, chronic) =>
    //            Utils.isSecretConfirmed(database)(secret, chronic.id)
    //        }
    //      }
    //    }
    //
    //
    //    @dsl trait SecretTable[M[_]] {
    //      def generateSecret: M[String]
    //      def setSecret(email: String, chronic: Data.ChronicleID, deadLine: Long, secret: String): M[Unit]
    //      def checkSecret(secret: String, chronic: Data.ChronicleID): M[Boolean]
    //    }


  }

  object user {

    def add(firstName: String, lastName: String, affiliation: String, password: Data.Password, hashAlgorithm: HashingAlgorithm) = {
      // NB here it is assumed that password is already hashed
      val user = User(UserID(Utils.uuid), firstName: String, lastName: String, affiliation: String, password, hashAlgorithm)
      for {
        _ <- DB {
          _.users += user
        }
        lockId <- lock.create(user.id, fr.iscpif.doors.server.lock.registration(user), Data.LockState.locked)
        _ <- lock.progress(lockId, Data.LockState.unlocked)
      } yield user.id
    }

    def get(userID: UserID)(settings: Settings, database: db.Database) = DB { scheme =>
      (for {
        u <- scheme.users if u.id === userID.id
      } yield u).result
    }.execute(settings, database)

    def isAdmin(uid: UserID)(settings: Settings, database: db.Database): Boolean = {
      def adminUsers: Seq[String] = DB { scheme =>
        (for {
          l <- scheme.locks if l.id === "admin"
          uc <- scheme.userLocks if (uc.userID === uid.id && uc.lockID === l.id)
        } yield uc.userID).result
      }.execute(settings, database)

      adminUsers.contains(uid.id)
    }


    // returns user connected to the lock connected to the secret
    def fromSecret(scrt: String)(settings: Settings, database: db.Database): Either[freedsl.dsl.Error,User] = {
        db.DB { scheme =>
          (for {
            s <- scheme.secrets.filter(s => s.secret === scrt)
            secl <- scheme.locks.filter(l => l.id === s.lockID) // the lock for the secret
            ul <- scheme.userLocks.filter(ul => ul.lockID === secl.id)
            u <- scheme.users.filter(u => u.id === ul.userID)
          } yield u).result.head
      }.execute(settings, database)
    }


    // from email to owner
    def fromEmail(email: String)(settings: Settings, database: db.Database): ApiRep[User] = {
      db.DB { scheme =>
        (for {
          e <- scheme.emails if (e.address === email)

          // most recent locks:(id, increment) ... because we want max(l.increment) to filter only the last lock having this l.id
          lastLocks <- scheme.locks.groupBy(_.id).map{ case (id, aLock) => (id , aLock.map(_.increment).max) }
          l <- scheme.locks if (l.state === Data.LockState.unlocked.id
                                && l.id === lastLocks._1
                                && l.increment === lastLocks._2
                                && l.id === e.lockID)

          ul <- scheme.userLocks if (ul.lockID === l.id)
          u <- scheme.users if (u.id === ul.userID)
        } yield u).result.headOption
      }.execute(settings, database)
    }

    def changePass(usr: User, newValue: String, newHName: String, newHJson: String)(settings: Settings, database: db.Database) = {
      db.DB { scheme =>
        scheme.users.filter(_.id === usr.id.id).map(
          u => (u.password, u.hashAlgorithm, u.hashParameters)
        ).update(newValue, newHName, newHJson)
      }.execute(settings, database)
    }

  }


  //          db.query(database)(q.result.headOption).map(ChronicleID(_))
  //        case add(userId, email, chronic) =>
  //          def status =
  //            Utils.emails(database)(userId) match {
  //              case Seq() => EmailStatus.Contact
  //              case _ => EmailStatus.Other
  //            }
  //
  //          db.query(database)(db.emails += Data.Email(chronic, email, status))
  //          ()
  //        case emailForChronic(chronic) =>
  //          db.query(database)(db.emails.filter(_.chronicleID === chronic.id).result.headOption)
  //        case exists(email) =>
  //          !db.query(database)(db.emails.filter { e => e.email === email }.result).isEmpty
  //      }
  //    }
  //  }


}
