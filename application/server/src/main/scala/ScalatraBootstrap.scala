import fr.iscpif.doors.server.{Launcher, Servlet}
import org.scalatra.LifeCycle
import javax.servlet.ServletContext

import fr.iscpif.doors.api.AccessQuest


class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    val args = context.get(Launcher.arguments).get.asInstanceOf[Launcher.Parameter]
    context mount(new Servlet(args.quests), "/*")
  }
}