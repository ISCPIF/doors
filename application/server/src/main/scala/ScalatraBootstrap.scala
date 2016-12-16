//
//import org.scalatra.LifeCycle
//import javax.servlet.ServletContext
//
//import fr.iscpif.doors.server._
//
//
//class ScalatraBootstrap extends LifeCycle {
//  override def init(context: ServletContext) {
//    val args = context.get(Launcher.arguments).get.asInstanceOf[Servlet.Arguments]
//    context mount(new Servlet(args), "/*")
//  }
//}