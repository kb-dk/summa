package dk.statsbiblioteket.summa.storage.http;

import dk.statsbiblioteket.util.qa.QAInfo;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * FIXME: Missing class docs for dk.statsbiblioteket.summa.storage.http.ServletExperiment
 *
 * @author mke
 * @since Sep 10, 2009
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class ServletExperiment extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse res)
                                   throws ServletException, IOException {

        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        
        out.println("<HTML>");
        out.println("<HEAD><TITLE>Hello World</TITLE></HEAD>");
        out.println("<BODY>");
        out.println("<BIG>Hello World</BIG>");
        out.println("</BODY></HTML>");
      }

}
