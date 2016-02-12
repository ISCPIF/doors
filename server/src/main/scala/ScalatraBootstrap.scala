import fr.iscpif.doors.server.Servlet
import org.scalatra.LifeCycle
import javax.servlet.ServletContext

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

object ScalatraBootstrap {
  def arguments = "arguments"

  case class Arguments()

}

import ScalatraBootstrap._

class ScalatraBootstrap extends LifeCycle {


  override def init(context: ServletContext) {
    val args = context.getAttribute(ScalatraBootstrap.arguments).asInstanceOf[ScalatraBootstrap.Arguments]
    context.mount(
      new Servlet {
        def arguments = args
      },
      "/*"
    )
  }
}


object JettyLauncher {
  // this is my entry object as specified in sbt project definition
  def main(args: Array[String]) {
    val port = scala.util.Try(args(0).toInt).getOrElse(8080)

    val server = new Server(port)
    val context = new WebAppContext()
    context setContextPath "/"

    context.setResourceBase("resource_managed/webapp")

    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")

    server.setHandler(context)

    server.start
    server.join
  }
}