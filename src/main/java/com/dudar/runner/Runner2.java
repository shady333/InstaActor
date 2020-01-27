package com.dudar.runner;

import com.dudar.InstaActor;
import com.dudar.InstaActor2;
import com.dudar.utils.Utilities;
import com.dudar.utils.services.EmailService;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Runner2 {

    public static void main(String[] args) throws InterruptedException, IOException, MessagingException {

        Map<String, InstaActor2> actors = new HashMap<>();
        String mainActorName = "InstaActor";
        actors.put(mainActorName, new InstaActor2(mainActorName));

        actors.get(mainActorName).start();

        while(true)
        {
            if(actors.get(mainActorName).isCompleted())
                break;

            if(statusRequestedEmailService(actors.get(mainActorName)))
            {
                actors.get(mainActorName).sendStatusViaEmail();
            }

            if(shouldStopEmailService(actors.get(mainActorName))) {
                actors.get(mainActorName).stopExecution();
                break;
            }


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
