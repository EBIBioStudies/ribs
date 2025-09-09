package uk.ac.ebi.biostudies.config;


import java.util.HashMap;
import java.util.Map;
import net.jawr.web.servlet.JawrSpringController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

@Configuration
@PropertySource("classpath:jawr.properties")
public class JawrConfig {

    @Autowired
    private JawrSpringController jawrJsController;
    @Autowired
    private JawrSpringController jawrCssController;

    @Bean
    public HandlerMapping jawrHandlerMapping() {
        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(-2147483648);
        Map<String, Object> urlMap = new HashMap();
        urlMap.put("**/*.css", this.jawrCssController);
        urlMap.put("**/*.js", this.jawrJsController);
        handlerMapping.setUrlMap(urlMap);
        return handlerMapping;
    }
}
