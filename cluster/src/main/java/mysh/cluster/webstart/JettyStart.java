package mysh.cluster.webstart;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * @author Mysh
 * @since 2014/11/9 11:55
 */
public class JettyStart {
	public static void main(String[] args) throws Exception {
		Server server = new Server(8080);

		WebAppContext webapp = new WebAppContext();
		webapp.setContextPath("/");
		webapp.setDescriptor("D:\\project\\MyshLib\\cluster\\src\\main\\webapp\\WEB-INF\\web.xml");
		webapp.setResourceBase("D:\\project\\MyshLib\\cluster\\src\\main\\webapp");

//		Configuration.ClassList classlist = Configuration.ClassList.setServerDefault(server);
//		classlist.addBefore(
//						"org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
//						"org.eclipse.jetty.annotations.AnnotationConfiguration");

		server.setHandler(webapp);

		server.start();
		server.join();
	}
}
