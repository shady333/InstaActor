package phaseII;

import com.dudar.utils.Utilities;
import com.dudar.utils.services.ActorActions;
import com.dudar.utils.services.EmailService;
import com.dudar.utils.services.Emailer;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import java.util.AbstractMap;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Executor {

    final static Logger logger = Logger.getLogger(Executor.class);


    public static void main(String[] args) throws InterruptedException {
        AbstractMap.SimpleEntry<String, ActorActions> currentAction = null;

        Date lastActionDate = new Date();
        boolean wasStopped = false;

        Controller controller = new Controller();
//        controller.registerActor("3dprint");
//        TimeUnit.SECONDS.sleep(30);
//        controller.registerActor("bricks");
//        TimeUnit.SECONDS.sleep(30);
//        controller.regis030);
        controller.registerActor("neverold");
        TimeUnit.SECONDS.sleep(30);

        Emailer emailerService = new Emailer();

        while(true){
            if(wasStopped && Utilities.isInternetConnection()){
                controller.startAllActors();
                wasStopped = false;
            }
            if(!Utilities.isInternetConnection()){
                logger.warn("STOPPING ALL ACTORS. Out of Internet connection.");
//                EmailService.generateAndSendEmail("STOPPING ALL ACTORS. Out of Internet connection.");
                controller.stopAllActors();
                wasStopped = true;
            }


            logger.info("Tick from executor");

            try {
                currentAction = emailerService.getActionFromEmail(Utilities.getActionsUserEmail(), lastActionDate);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
            if(currentAction != null && currentAction.getValue() != ActorActions.UNDEFINED){
                logger.info("Received action:" + currentAction.getKey() + "; " + currentAction.getValue());
                controller.proceedAction(currentAction);

                lastActionDate = new Date();
            }
            currentAction = null;

            controller.getStatusOfActors();

            TimeUnit.SECONDS.sleep(30);
        }
    }
}
