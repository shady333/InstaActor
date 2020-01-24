package com.dudar.utils.services;

import com.dudar.utils.Utilities;
import org.apache.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    final static Logger logger = Logger.getLogger(EmailService.class);

    static Properties mailServerProperties;
    static Session getMailSession;
    static MimeMessage generateMailMessage;

    public static void generateAndSendEmail(String emailBody) throws MessagingException {

        logger.debug("setup Mail Server Properties..");
        mailServerProperties = System.getProperties();
        mailServerProperties.put("mail.smtp.port", "587");
        mailServerProperties.put("mail.smtp.auth", "true");
        mailServerProperties.put("mail.smtp.starttls.enable", "true");
        logger.debug("Mail Server Properties have been setup successfully...");

        logger.debug("get Mail Session..");
        getMailSession = Session.getDefaultInstance(mailServerProperties, null);
        generateMailMessage = new MimeMessage(getMailSession);
        generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(Utilities.getEmailTo()));
        generateMailMessage.setSubject(Utilities.getEmailSubject());
        generateMailMessage.setContent(emailBody + "<br><br> Regards, <br>InstaActor", "text/html");
        logger.debug("Mail Session has been created successfully...");

        logger.debug("Get Session and Send mail");
        Transport transport = getMailSession.getTransport("smtp");
        transport.connect("smtp.gmail.com", Utilities.getEmailUserName(), Utilities.getEmailUserPassword());
        transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
        transport.close();
        logger.debug("Mail was sent successfully...");
    }

}
