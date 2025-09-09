package uk.ac.ebi.biostudies.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** Created by ehsan on 16/03/2017. */
@Configuration
@PropertySource("classpath:announcement.properties")
public class AnnouncementConfig {

  @Value("${announcement.enabled}")
  private boolean enabled;

  @Value("${announcement.headline}")
  private String headline;

  @Value("${announcement.message}")
  private String message;

  @Value("${announcement.priority}")
  private String priority;

  public boolean isEnabled() {
    return enabled;
  }

  public String getHeadline() {
    return headline;
  }

  public String getMessage() {
    return message;
  }

  public String getPriority() {
    return priority;
  }
}
