package uk.ac.ebi.biostudies.config;

/**
 * Created by ehsan on 23/02/2017.
 */

import org.apache.catalina.Context;
import org.apache.catalina.core.StandardHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
import uk.ac.ebi.biostudies.auth.BotFilter;


@Configuration
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = "uk.ac.ebi.biostudies")
@PropertySource("classpath:scheduler.properties")
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    ExternalServicesConfig externalServicesConfig;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer properties = new PropertySourcesPlaceholderConfigurer();

        properties.setLocation(new ClassPathResource("scheduler.properties"));
        properties.setIgnoreResourceNotFound(false);

        return properties;
    }

    private static void customize(Context context) {
        ((StandardHost) context.getParent()).setErrorReportValveClass(CustomErrorReportValve.class.getCanonicalName());
        context.getParent().getPipeline().addValve(new CustomErrorReportValve());

    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**");
        registry.addMapping("/files/**").allowedOrigins(externalServicesConfig.getAccessControlAllowOrigin().split(","))
                .allowedMethods("GET", "POST");
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        AntPathMatcher matcher = new AntPathMatcher();
        matcher.setCaseSensitive(false);
        configurer.setPathMatcher(matcher);
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
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> containerCustomizer() {
        return factory -> {
            factory.addContextCustomizers(MvcConfig::customize);
        };
    }

    @Bean
    public FilterRegistrationBean<BotFilter> botFilter() {
        FilterRegistrationBean<BotFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new BotFilter());
        registrationBean.addUrlPatterns("/*"); // Apply filter to all paths
        return registrationBean;
    }
}