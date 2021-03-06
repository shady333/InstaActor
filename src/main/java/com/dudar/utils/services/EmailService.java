package com.dudar.utils.services;

import com.dudar.utils.Utilities;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import javax.activation.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Date;
import java.util.Properties;

public class EmailService {

    final static Logger logger = Logger.getLogger(EmailService.class);

    static Properties mailServerProperties;
    static Session getMailSession;
    static MimeMessage generateMailMessage;

    /***
     * Expected email message subject in format: "ACTION_START ACTOR_MYACTOR any other characters"
     * @param sender
     * @param date
     * @return
     * @throws MessagingException
     */
    public static AbstractMap.SimpleEntry<String, ActorActions> getActionFromEmail(String sender, Date date) throws MessagingException {
        //create properties field
        Properties properties = new Properties();

        ActorActions resultAction = ActorActions.UNDEFINED;
        String resultActorName = "";

        try {

            properties.put("mail.pop3.host", "pop.gmail.com");
            properties.put("mail.pop3.port", "995");
            properties.put("mail.pop3.starttls.enable", "true");
            properties.put("mail.store.protocol", "imaps");

            Session emailSession = Session.getDefaultInstance(properties);


            //create the POP3 store object and connect with the pop server
            Store store = emailSession.getStore("imaps");

            store.connect("pop.gmail.com", Utilities.getEmailUserName(), Utilities.getEmailUserPassword());

            //create the folder object and open it
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            // retrieve the messages from the folder in an array and print it
            Message[] messages = emailFolder.search(new FlagTerm(new Flags(
                    Flags.Flag.SEEN), false));
            logger.info("messages.length---" + messages.length);

            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];

                if (message.getSentDate().after(date)) {
                    String subject = message.getSubject();
                    if (subject.contains("ACTION_") & subject.contains("ACTOR_") & message.getFrom()[0].toString().contains(sender)) {

                        String actionName = subject.split(" ")[0].split("_")[1];
                        resultActorName = subject.split(" ")[1].split("_")[1];

                        if (actionName.equals("START")) {
                            resultAction = ActorActions.START;
                        } else if (actionName.equals("STOP")) {
                            resultAction = ActorActions.STOP;
                        } else if (actionName.equals("STATUS")) {
                            resultAction = ActorActions.STATUS;
                        } else if (actionName.equals("ABORT")) {
                            resultAction = ActorActions.ABORT;
                        } else if (actionName.equals("REBOOT")) {
                            resultAction = ActorActions.REBOOT;
                        } else if (actionName.equals("REGISTER")) {
                            resultAction = ActorActions.REGISTER;
                        } else if (actionName.equals(ActorActions.ENABLELIKE.toString())) {
                            resultAction = ActorActions.ENABLELIKE;
                        } else if (actionName.equals(ActorActions.DISABLELIKE.toString())) {
                            resultAction = ActorActions.DISABLELIKE;
                        } else if (actionName.equals(ActorActions.ENABLECOMMENT.toString())) {
                            resultAction = ActorActions.ENABLECOMMENT;
                        } else if (actionName.equals(ActorActions.DISABLECOMMENT.toString())) {
                            resultAction = ActorActions.DISABLECOMMENT;
                        }
                        else if (actionName.equals("DOWNLOAD")) {
                            resultAction = ActorActions.DOWNLOAD;
                        }
                        else if (actionName.equals("UPLOAD")) {
                            Multipart multipart = (Multipart) message.getContent();

                            for (int j = 0; j < multipart.getCount(); j++) {
                                BodyPart bodyPart = multipart.getBodyPart(j);
                                if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
                                        StringUtils.isBlank(bodyPart.getFileName())) {
                                    continue; // dealing with attachments only
                                }
                                InputStream is = bodyPart.getInputStream();
                                // -- EDIT -- SECURITY ISSUE --
                                // do not do this in production code -- a malicious email can easily contain this filename: "../etc/passwd", or any other path: They can overwrite _ANY_ file on the system that this code has write access to!
                                if(bodyPart.getFileName().contains(".properties")){
                                    File f = new File("tmp/" + bodyPart.getFileName());
                                    FileOutputStream fos = new FileOutputStream(f);
                                    byte[] buf = new byte[4096];
                                    int bytesRead;
                                    while ((bytesRead = is.read(buf)) != -1) {
                                        fos.write(buf, 0, bytesRead);
                                    }
                                    fos.close();
                                    resultAction = ActorActions.UPLOAD;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            //close the store and folder objects
            emailFolder.close(false);
            store.close();

        }
        catch (Exception ex){
            logger.error(ex.getMessage());
        }

        return new AbstractMap.SimpleEntry<>(resultActorName, resultAction);
    }

    public static void generateAndSendEmail(String emailBody){
        try{

            logger.debug("setup Mail Server Properties..");
            mailServerProperties = System.getProperties();
            mailServerProperties.put("mail.smtp.port", "587");
            mailServerProperties.put("mail.smtp.auth", "true");
            mailServerProperties.put("mail.smtp.starttls.enable", "true");
            mailServerProperties.put("mail.smtp.starttls.required", "true");
            mailServerProperties.put("mail.socketFactory.port", "587");
            mailServerProperties.put("mail.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            mailServerProperties.put("mail.socketFactory.fallback", "false");
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
        catch (MessagingException ex){
            logger.error("Can't send Email");
            logger.error(ex.getMessage());
        }

    }

    public static void generateAndSendEmail(String emailBody, String filePathAttachment){
        try{

            logger.debug("setup Mail Server Properties..");
            mailServerProperties = System.getProperties();
            mailServerProperties.put("mail.smtp.port", "587");
            mailServerProperties.put("mail.smtp.auth", "true");
            mailServerProperties.put("mail.smtp.starttls.enable", "true");
            mailServerProperties.put("mail.smtp.starttls.required", "true");
            mailServerProperties.put("mail.socketFactory.port", "587");
            mailServerProperties.put("mail.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            mailServerProperties.put("mail.socketFactory.fallback", "false");
            logger.debug("Mail Server Properties have been setup successfully...");

            logger.debug("get Mail Session..");
            getMailSession = Session.getDefaultInstance(mailServerProperties, null);
            generateMailMessage = new MimeMessage(getMailSession);
            generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(Utilities.getEmailTo()));
            generateMailMessage.setSubject(Utilities.getEmailSubject());



            // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();

            // Now set the actual message
            messageBodyPart.setText(emailBody + "<br><br> Regards, <br>InstaActor");

            // Create a multipar message
            Multipart multipart = new MimeMultipart();

            // Set text message part
            multipart.addBodyPart(messageBodyPart);

            // Part two is attachment
            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(filePathAttachment);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(filePathAttachment);
            multipart.addBodyPart(messageBodyPart);

            // Send the complete message parts
            generateMailMessage.setContent(multipart,"text/html");

            logger.debug("Mail Session has been created successfully...");

            logger.debug("Get Session and Send mail");
            Transport transport = getMailSession.getTransport("smtp");
            transport.connect("smtp.gmail.com", Utilities.getEmailUserName(), Utilities.getEmailUserPassword());
            transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
            transport.close();
            logger.debug("Mail was sent successfully...");
        }
        catch (MessagingException ex){
            logger.error("Can't send Email");
            logger.error(ex.getMessage());
        }

    }

}
