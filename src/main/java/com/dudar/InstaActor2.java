package com.dudar;

import com.dudar.utils.services.EmailService;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import java.util.Date;

public class InstaActor2 implements Runnable, Actor {

    private boolean isStopped;
    private boolean isCompleted = false;
    private String name;
    private Thread t = null;
    private Date creationDate;

    final static Logger logger = Logger.getLogger(InstaActor2.class);

    public InstaActor2(String name){
        this.name = name;
        creationDate = new Date();
    }

    public Date getCreationDate(){
        return creationDate;
    }

    public boolean isCompleted(){
        return isCompleted;
    }

    @Override
    public void run() {
        isStopped = false;

        int tickersCount = 0;

        while(!isStopped && !isCompleted)
        {
            if(isStopped){
                logger.info(name + " Stop received");
                isStopped = false;
                break;
            }


            workMethod();

            logger.info("Ticker - " + tickersCount);
            tickersCount++;
            if(tickersCount > 1000){
                isCompleted = true;
            }
        }
        logger.info(name + " Execution stopped");
        try {
            EmailService.generateAndSendEmail("Execution stopped");
        }
        catch (MessagingException ex){
            logger.error(ex.getMessage());
        }
    }

    private void workMethod() {
        logger.info(name + " Doing some work ...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isActive(){
        t.getState();
        return true;
    }

    public Actor start () {
        logger.info("Starting " +  name );
        try {
            EmailService.generateAndSendEmail("Started");
        }
        catch (MessagingException ex){
            logger.error(ex.getMessage());
        }
        if (t == null) {
            t = new Thread (this, name);
            t.start ();
        }
        return this;
    }

    @Override
    public Actor stop() {
        return null;
    }

    @Override
    public String getStatus() {
        String currentStatus = "/**** Insta Actor "+name+" ****/\n";
        currentStatus += "|\n";
//        currentStatus += "|   Tag: " + currentTag + " from " + ALLTAGS_COUNT + ".\n";
//        currentStatus += "|   Current post number " + i + " from " + maxPostsCount + ".\n";
//        currentStatus += "|   Url: " + currentPostUrl + ".\n";
//        currentStatus += "|   Type: " + currentPostType.toString() + ".\n";
//        currentStatus += "|   Like: " + currentPostLikeAdded + ".\n";
//        if(!Strings.isNullOrEmpty(addedComment)){
//            currentStatus += "|   Added comment: " + addedComment + ".\n";
//        }
//        currentStatus += "|   Likes added Total: " + totalLikes + ".\n";
        currentStatus += "|********************************\n";

        logger.info("Current post info:\n" + currentStatus);

        return currentStatus;
    }

    public void sendStatusViaEmail(){
        try {
            EmailService.generateAndSendEmail(getStatus().replaceAll("\n", "</br>"));
            creationDate = new Date();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void stopExecution(){
        isStopped = true;
        t = null;
    }

    public String getName() {
        return name;
    }
}
