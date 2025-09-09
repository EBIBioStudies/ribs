package uk.ac.ebi.biostudies.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Configuration
@Component
public class MyTomcatConnectorCustomizer
    implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
  @Autowired private Environment env;

  @Override
  public void customize(TomcatServletWebServerFactory factory) {
    Integer maxParameterCount =
        env.getProperty("server.tomcat.max-parameter-count", Integer.class, -1);
    factory.addConnectorCustomizers(connector -> connector.setMaxParameterCount(maxParameterCount));
  }
}
