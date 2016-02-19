import fr.iscpif.doors.server.Servlet
import org.scalatra.LifeCycle
import javax.servlet.ServletContext


class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context mount(new Servlet, "/*")
  }
}