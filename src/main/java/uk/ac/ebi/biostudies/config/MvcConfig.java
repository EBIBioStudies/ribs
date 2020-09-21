package uk.ac.ebi.biostudies.config;

/**
 * Created by ehsan on 23/02/2017.
 */

import net.jawr.web.servlet.JawrSpringController;
import org.apache.catalina.Context;
import org.apache.catalina.core.StandardHost;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.config.annotation.*;


@Configuration
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = "uk.ac.ebi.biostudies")
@PropertySource("classpath:scheduler.properties")
public class MvcConfig implements WebMvcConfigurer {


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**");
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        AntPathMatcher matcher = new AntPathMatcher();
        matcher.setCaseSensitive(false);
        configurer.setPathMatcher(matcher);
    }


    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/{collection:.+}/studies/EMPIAR-{id:.+}", "https://www.ebi.ac.uk/pdbe/emdb/empiar/entry/{id}/");
        registry.addRedirectViewController("/studies/EMPIAR-{id:.+}", "https://www.ebi.ac.uk/pdbe/emdb/empiar/entry/{id}/");
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/about").setViewName("about");
        registry.addViewController("/about/*").setViewName("about");
        registry.addViewController("/help").setViewName("help");
        registry.addViewController("/help/").setViewName("help");
        registry.addViewController("/studies/help").setViewName("help");
        registry.addViewController("/studies/help/").setViewName("help");
        registry.addViewController("/{collection:.+}/help").setViewName("help");
        registry.addViewController("/{collection:.+}/help/").setViewName("help");
        registry.addViewController("/{collection:.+}/**/help").setViewName("help");
        registry.addViewController("/{collection:.+}/**/help/").setViewName("help");
        registry.addViewController("/submit").setViewName("submit");
        registry.addViewController("/submit/*").setViewName("submit");

        registry.addViewController("/collections").setViewName("collections");
        registry.addViewController("/collections/").setViewName("collections");
        registry.addViewController("/studies").setViewName("search");
        registry.addViewController("/studies/*").setViewName("search");
        registry.addViewController("/studies/{accession:.+}").setViewName("detail");
        registry.addViewController("/studies/{accession:.+}/").setViewName("detail");
        registry.addViewController("/studies/{accession:.+}/files").setViewName("files");
        registry.addViewController("/studies/{accession:.+}/files/").setViewName("files");

        registry.addViewController("/{collection:.+}/studies").setViewName("search");
        registry.addViewController("/{collection:.+}/studies/").setViewName("search");

        registry.addViewController("/{collection:.+}/studies/{accession:.+}").setViewName("detail");
        registry.addViewController("/{collection:.+}/studies/{accession:.+}/").setViewName("detail");

        registry.addViewController("/{collection:.+}/studies/{accession:.+}/files").setViewName("files");
        registry.addViewController("/{collection:.+}/studies/{accession:.+}/files/").setViewName("files");

        registry.addViewController("/studies/{accession:.+}/sdrf").setViewName("sdrf");
        registry.addViewController("/studies/{accession:.+}/sdrf/").setViewName("sdrf");
        registry.addViewController("/{collection:.+}/studies/{accession:.+}/sdrf").setViewName("sdrf");
        registry.addViewController("/{collection:.+}/studies/{accession:.+}/sdrf/").setViewName("sdrf");

        registry.addViewController("/studies/{accession:.+}/csv").setViewName("csv");
        registry.addViewController("/studies/{accession:.+}/csv").setViewName("csv");
        registry.addViewController("/{collection:.+}/studies/{accession:.+}/csv").setViewName("csv");
        registry.addViewController("/{collection:.+}/studies/{accession:.+}/csv/").setViewName("csv");

        registry.addViewController("{accession:.+}").setViewName("detail");
        registry.addViewController("{accession:.+}/").setViewName("detail");

        registry.addViewController("arrayexpress-in-biostudies").setViewName("arrayexpress-in-biostudies");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/help/**").addResourceLocations("/help/").setCachePeriod(6000);
        registry.addResourceHandler("/js/**").addResourceLocations("/js/").setCachePeriod(6000);
        registry.addResourceHandler("/css/**").addResourceLocations("/css/").setCachePeriod(6000);
        registry.addResourceHandler("/images/**").addResourceLocations("/images/").setCachePeriod(6000);
        registry.addResourceHandler("/misc/**").addResourceLocations("/misc/").setCachePeriod(6000);
        registry.addResourceHandler("/fonts/**").addResourceLocations("/fonts/").setCachePeriod(6000);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer properties = new PropertySourcesPlaceholderConfigurer();

        properties.setLocation(new ClassPathResource("scheduler.properties"));
        properties.setIgnoreResourceNotFound(false);

        return properties;
    }

    @Bean
    public JawrSpringController jawrJsController() {
        JawrSpringController jawrJsController = new JawrSpringController();
        jawrJsController.setConfigLocation("/jawr.properties");
        jawrJsController.setType("js");
        return jawrJsController;
    }

    @Bean
    public JawrSpringController jawrCssController() {
        JawrSpringController jawrCssController = new JawrSpringController();
        jawrCssController.setConfigLocation("/jawr.properties");
        jawrCssController.setType("css");
        return jawrCssController;
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> containerCustomizer() {
        return factory -> {
            factory.addContextCustomizers(MvcConfig::customize);
        };
    }

    private static void customize(Context context) {
        ((StandardHost) context.getParent()).setErrorReportValveClass(CustomErrorReportValve.class.getCanonicalName());
        context.getParent().getPipeline().addValve(new CustomErrorReportValve());

    }
}