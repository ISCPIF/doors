package client

import org.scalajs.dom
import scala.concurrent.Future
import scalatags.JsDom.{tags ⇒ tags}
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


    dom.document.body.appendChild(
      tags.div("iscpifWUI !!").render
    )

    val maindiv = dom.document.body.appendChild(tags.div.render)
    body.appendChild(maindiv)
  }
}

object Post extends autowire.Client[String, upickle.Reader, upickle.Writer] {

  override def doCall(req: Request): Future[String] = {
    val url = req.path.mkString("/")
    dom.ext.Ajax.post(
      url = "http://localhost:8080/" + url,
      data = upickle.write(req.args)
    ).map {
      _.responseText
    }
  }

  def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)

  def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}
