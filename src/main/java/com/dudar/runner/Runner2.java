package com.dudar.runner;

import com.dudar.ActorsManager;
import com.dudar.InstaActor2;
import com.dudar.utils.Utilities;
import com.dudar.utils.services.ActorActions;
import com.dudar.utils.services.EmailService;
import org.apache.commons.lang3.StringUtils;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Runner2 {

    public static void main(String[] args) throws InterruptedException, IOException, MessagingException {

//        Map<String, InstaActor2> actors = new HashMap<>();
//        String mainActorName = "InstaActor";
//        actors.put(mainActorName, new InstaActor2(mainActorName));
//
//        actors.get(mainActorName).start();
//
        ActorsManager actors = ActorsManager.getInstance();

        Date lastActionDate = new Date();

        AbstractMap.SimpleEntry<String, ActorActions> currentAction;

        while(true)
        {
            currentAction = EmailService.getActionFromEmail(Utilities.getActionsUserEmail(), lastActionDate);

            if(!(currentAction.getValue() != ActorActions.UNDEFINED)
                    & !StringUtils.isEmpty(currentAction.getKey())){

                //DO SOME STUFF

            }

            lastActionDate = new Date();
            TimeUnit.SECONDS.sleep(20);
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
