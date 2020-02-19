package com.dudar.runner;

import com.dudar.ActorsManager;
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

    public static void main(String[] args) throws InterruptedException {

        logger.info("Starting...");
        Date lastActionDate = new Date();
        AbstractMap.SimpleEntry<String, ActorActions> currentAction;
        if(args.length > 0){
            if(args[0].equals("-Doption=START")){
                ActorsManager.getInstance().initActorsFromDataFolder();
                ActorsManager.getInstance().startAllRegistered();
            }
        }
        EmailService.generateAndSendEmail("InstaActor service is UP and running");
        while(true)
        {
            try {
                ActorsManager.getInstance().trackActiveServices();

                currentAction = EmailService.getActionFromEmail(Utilities.getActionsUserEmail(), lastActionDate);
                if (currentAction.getValue() == ActorActions.ABORT) {
                    EmailService.generateAndSendEmail("<h1>!!!STOP FOR EXECUTION!!!");
                    logger.info("END");
                    System.exit(0);
                }
                if ((currentAction.getValue() != ActorActions.UNDEFINED)
                        & !StringUtils.isEmpty(currentAction.getKey())) {
                    logger.info(currentAction.getKey() + " --- " + currentAction.getValue());
                    ActorsManager.getInstance().proceedAction(currentAction);
                    lastActionDate = new Date();
                }
            }
            catch (Exception ex){
                logger.error("Exception in Runner - " + ex.getMessage());
            }
            finally {
                TimeUnit.SECONDS.sleep(30);
            }
        }
    }
}
