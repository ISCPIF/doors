import fr.iscpif.iscpifwui.server.Servlet
import org.scalatra.LifeCycle
import javax.servlet.ServletContext


object ScalatraBootstrap {
  def arguments = "arguments"
  case class Arguments()
}

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