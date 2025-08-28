package uk.ac.ebi.biostudies.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import uk.ac.ebi.biostudies.auth.*;


@Configuration
@EnableWebSecurity
@ComponentScan("uk.ac.ebi.biostudies.config")
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private RestBasedAuthenticationProvider authProvider;
    @Autowired
    private CustomRememberMeCookieService customRememberMeCookieService;
    @Autowired
    private BioStudiesLogoutHandler bioStudiesLogoutHandler;
    @Autowired
    CookieFilter cookieFilter;
    @Autowired
    SecurityConfig securityConfig;
    @Autowired
    private XSSFilter xssFilter;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authProvider);
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        String adminIPAllowStr = securityConfig.getAdminIPAllowList();
        String adminIPAllowList[] = adminIPAllowStr.split("\\|");
        StringBuilder ipAllowList = new StringBuilder("");
        for(String ipAdmin:adminIPAllowList){
            ipAllowList.append(String.format(" or hasIpAddress('%s')", ipAdmin));
        }
        http.addFilterAfter(xssFilter, SecurityContextPersistenceFilter.class)
                .addFilterAfter(cookieFilter, AnonymousAuthenticationFilter.class)
                .csrf().disable()
                .headers().frameOptions().sameOrigin().and()
                .anonymous().principal("guest").authorities("GUEST").and()
                .authorizeRequests().antMatchers("/api/v1/index/**").access("hasAuthority('SUPER_USER')"+ipAllowList.toString())
                .antMatchers("/**").permitAll()
                .and().formLogin().successHandler(new RefererAuthenticationSuccessHandler())
                .loginPage("/").usernameParameter("u").passwordParameter("p").permitAll()
                .and().logout().logoutUrl("/logout").addLogoutHandler(bioStudiesLogoutHandler).deleteCookies("JSESSIONID")
                .and().rememberMe().rememberMeServices(customRememberMeCookieService)
                ;
    }


}
