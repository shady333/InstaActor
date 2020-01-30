package com.dudar.runner;

import com.dudar.ActorsManager;
import com.dudar.InstaActor2;
import com.dudar.utils.Utilities;
import com.dudar.utils.services.ActorActions;
import com.dudar.utils.services.EmailService;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import java.util.AbstractMap;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Runner {

    final static Logger logger = Logger.getLogger(Runner.class);

    public static void main(String[] args) throws InterruptedException, MessagingException {

//        Map<String, InstaActor2> actors = new HashMap<>();
//        String mainActorName = "InstaActor";
//        actors.put(mainActorName, new InstaActor2(mainActorName));
//
//        actors.get(mainActorName).start();
//
        logger.info("Starting...");

//        ActorsManager actors = ActorsManager.getInstance();

        Date lastActionDate = new Date();

        AbstractMap.SimpleEntry<String, ActorActions> currentAction;

        EmailService.generateAndSendEmail("InstaActor service is UP and running");

        while(true)
        {
//            currentAction = new AbstractMap.SimpleEntry<>("inline", ActorActions.START);
            currentAction = EmailService.getActionFromEmail(Utilities.getActionsUserEmail(), lastActionDate);
            lastActionDate = new Date();

            if(currentAction.getValue() == ActorActions.STATUS
            & currentAction.getKey().equals("ALL")){
                String message = "<p>Registered actors:";
                message += "<p>" + ActorsManager.getInstance().getAllStatus();

                EmailService.generateAndSendEmail(message);
            }

            if(currentAction.getValue() == ActorActions.ABORT){
                EmailService.generateAndSendEmail("<h1>!!!STOP FOR EXECUTION!!!");
                logger.info("END");
                System.exit(0);
            }

            if((currentAction.getValue() != ActorActions.UNDEFINED)
                    & !StringUtils.isEmpty(currentAction.getKey())){

                //DO SOME STUFF
                logger.info(currentAction.getKey() + " --- " + currentAction.getValue());

                ActorsManager.getInstance().proceedAction(currentAction);


            }

            //
            TimeUnit.SECONDS.sleep(60);
        }

    }

    private static boolean statusRequestedEmailService(InstaActor2 actor){
        try {
            return EmailService.isStatusRequestMessage(Utilities.getActionsUserEmail(), actor.getName(), actor.getCreationDate());
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean shouldStopEmailService(InstaActor2 actor){
        try {
            return EmailService.isStopMessage(Utilities.getActionsUserEmail(), actor.getName(), actor.getCreationDate());
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return false;
    }

}
