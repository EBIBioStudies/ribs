/*
 * Copyright 2009-2016 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.biostudies.api.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.config.MailConfig;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Service
public class EmailSender {

  @Autowired MailConfig mailConfig;

  public void send(
      String[] recipients, String[] hiddenRecipients, String subject, String message, String from)
      throws MessagingException {
    boolean debug = false;

    // Set the host smtp address and port
    Properties props = new Properties();
    props.put("mail.smtp.host", mailConfig.getSmtpHost());
    props.put("mail.smtp.port", mailConfig.getSmtpPort());

    // create some properties and get the default Session
    Session session = Session.getDefaultInstance(props, null);
    session.setDebug(debug);

    // create a message
    MimeMessage msg = new MimeMessage(session);

    // set originator (FROM) address
    InternetAddress addressFrom = new InternetAddress(from);
    msg.setFrom(addressFrom);

    // set recipients (TO) address
    InternetAddress[] addressTo = new InternetAddress[recipients.length];
    for (int i = 0; i < recipients.length; i++) {
      addressTo[i] = new InternetAddress(recipients[i]);
    }
    msg.setRecipients(Message.RecipientType.TO, addressTo);

    // set hidden recipients (BCC) address
    InternetAddress[] addressBcc = new InternetAddress[hiddenRecipients.length];
    for (int i = 0; i < hiddenRecipients.length; i++) {
      addressBcc[i] = new InternetAddress(hiddenRecipients[i]);
    }
    msg.setRecipients(Message.RecipientType.BCC, addressBcc);

    // Setting the Subject and Content Type
    msg.setSubject(subject);
    msg.setText(message, "UTF-8");
    Transport.send(msg);
  }
}
