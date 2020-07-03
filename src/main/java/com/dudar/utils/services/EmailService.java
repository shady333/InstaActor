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
        generateAndSendEmail(emailBody, "");
    }

    public static void generateAndSendEmail(String emailBody, String filePathAttachment){
        // Recipient's email ID needs to be mentioned.
        String to = Utilities.getEmailTo();

        // Sender's email ID needs to be mentioned
        String from = Utilities.getEmailUserName();

        // Assuming you are sending email from through gmails smtp
        String host = "smtp.gmail.com";

        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        // Get the Session object.// and pass
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {

                return new PasswordAuthentication(Utilities.getEmailUserName(), Utilities.getEmailUserPassword());

            }

        });
        //session.setDebug(true);
        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            // Set Subject: header field
            message.setSubject(Utilities.getEmailSubject());

            Multipart multipart = new MimeMultipart();

            MimeBodyPart attachmentPart = new MimeBodyPart();

            MimeBodyPart textPart = new MimeBodyPart();

            try {

                textPart.setText(emailBody);
                multipart.addBodyPart(textPart);
                try{
                    if(!filePathAttachment.isEmpty()) {
                        File f =new File(filePathAttachment);
                        attachmentPart.attachFile(f);
                        multipart.addBodyPart(attachmentPart);
                    }
                }
                catch (NullPointerException ex){
                    logger.debug(ex.getMessage());
                }



            } catch (Exception e) {

                e.printStackTrace();

            }

            message.setContent(multipart);

            System.out.println("sending...");
            // Send message
            Transport.send(message);
            System.out.println("Sent message successfully....");
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}
