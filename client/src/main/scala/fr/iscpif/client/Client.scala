package client

import fr.iscpif.client.Connection
import org.scalajs.dom
import scala.concurrent.Future
import scalatags.JsDom.{tags ⇒ tags}
import fr.iscpif.scaladget.tools.JsRxTags._
import scalatags.JsDom.all._
import rx._
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import shared._
import upickle._
import autowire._

@JSExport("Client")
object Client {

  val helloValue = Var(0)
  val caseClassValue = Var("empty")

  @JSExport
  def run(): Unit = {
    val body = dom.document.body
    val connexion = new Connection


    dom.document.body.appendChild(
      connexion.render
    )

    val maindiv = dom.document.body.appendChild(tags.div.render)
    body.appendChild(maindiv)
  }
}

object Post extends autowire.Client[String, upickle.default.Reader, upickle.default.Writer] {

  override def doCall(req: Request): Future[String] = {
    val url = req.path.mkString("/")
    dom.ext.Ajax.post(
      url = "http://localhost:8080/" + url,
      data = upickle.default.write(req.args)
    ).map {
      _.responseText
    }
  }

  def read[Result: upickle.default.Reader](p: String) = upickle.default.read[Result](p)

  def write[Result: upickle.default.Writer](r: Result) = upickle.default.write(r)
}
