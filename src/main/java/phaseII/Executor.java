package phaseII;

import com.dudar.utils.Utilities;
import com.dudar.utils.services.ActorActions;
import com.dudar.utils.services.EmailService;
import com.dudar.utils.services.Emailer;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Executor {

    final static Logger logger = Logger.getLogger(Executor.class);


    public static void main(String[] args) throws InterruptedException {
        AbstractMap.SimpleEntry<String, ActorActions> currentAction = null;

        Date lastActionDate = new Date();
        boolean wasStopped = false;

        Runnable controller = new Controller();
        ((Controller) controller).registerActor("3dprint");
        ((Controller) controller).registerActor("bricks");
        ((Controller) controller).registerActor("legomini");
        ((Controller) controller).registerActor("neverold");
        ((Controller) controller).registerActor("inline");

        Emailer emailerService = new Emailer();

        Thread controllerThread = new Thread(controller);
        controllerThread.start();

//        ((Controller) controller).proceedAction(new AbstractMap.SimpleEntry<>("ALL", ActorActions.START));

        Process p = null;

        while(true){

//            controller.start();




            if(!Utilities.isInternetConnection() && !wasStopped){

                try {
                    p = Runtime.getRuntime().exec("sh stopGrid.sh");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                p.waitFor();

                logger.warn("STOPPING ALL ACTORS. Out of Internet connection.");
//                EmailService.generateAndSendEmail("STOPPING ALL ACTORS. Out of Internet connection.");
                ((Controller) controller).proceedAction(new AbstractMap.SimpleEntry<>("ALL", ActorActions.STOP));
                wasStopped = true;
            }

            if(wasStopped && Utilities.isInternetConnection()){


                try {
                    p = Runtime.getRuntime().exec("sh startGrid.sh");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                p.waitFor();

                logger.warn("START ALL ACTORS. Internet connection resumed.");
                ((Controller) controller).proceedAction(new AbstractMap.SimpleEntry<>("ALL", ActorActions.START));
                wasStopped = false;
            }

//
            logger.info("Tick from executor");
//
            try {
                currentAction = emailerService.getActionFromEmail(Utilities.getActionsUserEmail(), lastActionDate);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
            if(currentAction != null && currentAction.getValue() != ActorActions.UNDEFINED){
                logger.info("Received action:" + currentAction.getKey() + "; " + currentAction.getValue());
                ((Controller) controller).proceedAction(currentAction);

                lastActionDate = new Date();
            }
            currentAction = null;
//
//            controller.getStatusOfActors();

//            controller.stopAllActors();

            TimeUnit.SECONDS.sleep(30);
        }
    }
}
