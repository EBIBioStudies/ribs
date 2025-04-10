package uk.ac.ebi.biostudies.config;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class CustomErrorReportValve extends ErrorReportValve {

    private Logger logger = LogManager.getLogger(CustomErrorReportValve.class.getName());

    @Override
    protected void report(Request request, Response response, Throwable throwable) {
        int status = response.getStatus();
        if ( status < 400) return;

        if (throwable!=null) {
            logger.error(throwable);
        }
        try {
            String errorPage = FileCopyUtils.copyToString(new InputStreamReader(
                            this.getClass().getClassLoader().getResourceAsStream ("static/error.html"),
                            Charset.forName("utf-8")));

            response.setContentType(String.valueOf(ContentType.TEXT_HTML));
            response.setContentLength(errorPage.length());
            response.getOutputStream().print(errorPage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
