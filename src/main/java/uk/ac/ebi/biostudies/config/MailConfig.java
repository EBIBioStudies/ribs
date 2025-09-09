package uk.ac.ebi.biostudies.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** Created by ehsan on 06/04/2017. */
@Configuration
@PropertySource("classpath:email.properties")
public class MailConfig {
  @Value("${app.reports.originator}")
  private String reportsOriginator;

  @Value("${app.reports.recipients}")
  private String reportsRecipients;

  @Value("${app.reports.hidden-recipients}")
  private String hiddenRecipients;

  @Value("${app.reports.subject}")
  private String reportsSubject;

  @Value("${app.reports.smtp.host}")
  private String smtpHost;

  @Value("${app.reports.smtp.port}")
  private String smtpPort;

  @Value("${bs.feedback.recipients}")
  private String feedbackRecipients;

  @Value("${bs.feedback.subject}")
  private String feedbackSubject;

  @Value("${bs.password-remind.originator}")
  private String passwordRemindOriginator;

  @Value("${bs.password-remind.recipients}")
  private String passwordRemindRecipients;

  @Value("${bs.password-remind.subject}")
  private String passwordRemindSubject;

  public String getReportsOriginator() {
    return reportsOriginator;
  }

  public String[] getReportsRecipients() {
    return reportsRecipients.split(",");
  }

  public String[] getHiddenRecipients() {
    return hiddenRecipients.split(",");
  }

  public String getReportsSubject() {
    return reportsSubject;
  }

  public String getSmtpHost() {
    return smtpHost;
  }

  public String getSmtpPort() {
    return smtpPort;
  }

  public String[] getFeedbackRecipients() {
    return feedbackRecipients.split(",");
  }

  public String getFeedbackSubject() {
    return feedbackSubject;
  }

  public String getPasswordRemindOriginator() {
    return passwordRemindOriginator;
  }

  public String[] getPasswordRemindRecipients() {
    return passwordRemindRecipients.split(",");
  }

  public String getPasswordRemindSubject() {
    return passwordRemindSubject;
  }
}
