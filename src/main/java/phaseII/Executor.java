package phaseII;

import com.dudar.utils.Utilities;
import com.dudar.utils.services.ActorActions;
import com.dudar.utils.services.EmailService;
import com.dudar.utils.services.Emailer;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.openqa.selenium.remote.DriverCommand.STATUS;

public class Executor {

    final static Logger logger = Logger.getLogger(Executor.class);
    static List<Controller> controllersCollection = new ArrayList<>();
    static Emailer emailerService = new Emailer();

    public static void main(String[] args) throws InterruptedException {
        AbstractMap.SimpleEntry<String, ActorActions> currentAction = null;
        Process p = null;

        Date lastActionDate = new Date();
        boolean wasStopped = false;

        Runnable controller1 = new Controller();
        Runnable controller2 = new Controller();
        ((Controller) controller1).registerActor("3dprint");
        ((Controller) controller1).registerActor("bricks");
        ((Controller) controller1).registerActor("legomini");
        ((Controller) controller1).registerActor("snail");
        ((Controller) controller2).registerActor("neverold");
        ((Controller) controller2).registerActor("inline");

        controllersCollection.add((Controller) controller1);
        controllersCollection.add((Controller) controller2);



//        Thread controllerThread = new Thread(controller1);
//        controllerThread.start();
//
//        controllersCollection.add(new Thread(controller1));
//        controllersCollection.add(new Thread(controller2));

//        ((Controller) controller).proceedAction(new AbstractMap.SimpleEntry<>("ALL", ActorActions.START));

        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        ses.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                resetGrid();
            }
        }, 0, 4, TimeUnit.HOURS);




        controllersCollection.forEach(controller -> {
            Thread controllerThread = new Thread(controller);
            controllerThread.start();
            try{
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        while(true){

//            controller.start();




            if(!Utilities.isInternetConnection() && !wasStopped){

                try {
                    p = Runtime.getRuntime().exec("bash stopGrid.sh");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                p.waitFor();

                logger.warn("STOPPING ALL ACTORS. Out of Internet connection.");
//                EmailService.generateAndSendEmail("STOPPING ALL ACTORS. Out of Internet connection.");

                controllersCollection.forEach(item -> {
                    item.proceedAction(new AbstractMap.SimpleEntry<>("ALL", ActorActions.STOP));
                });
//                ((Controller) controller1).proceedAction(new AbstractMap.SimpleEntry<>("ALL", ActorActions.STOP));
                wasStopped = true;
            }

            if(wasStopped && Utilities.isInternetConnection()){


                try {
                    p = Runtime.getRuntime().exec("bash startGrid.sh");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                p.waitFor();

                logger.warn("START ALL ACTORS. Internet connection resumed.");
                controllersCollection.forEach(item -> {
                    item.proceedAction(new AbstractMap.SimpleEntry<>("ALL", ActorActions.START));
                });
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

                proceedAction(currentAction);

//                AbstractMap.SimpleEntry<String, ActorActions> finalCurrentAction = currentAction;
//                controllersCollection.forEach(item -> {
//                    item.proceedAction(finalCurrentAction);
//                });

                lastActionDate = new Date();
            }
            currentAction = null;
//
//            controller.getStatusOfActors();

//            controller.stopAllActors();

            TimeUnit.SECONDS.sleep(10);
        }
    }

    private static void proceedAction(AbstractMap.SimpleEntry<String, ActorActions> currentAction) {
        switch (currentAction.getValue()){
            case STATUS:
                final String[] statusMessage = {"STATUS for ALL\n"};
                controllersCollection.forEach(controller -> {
                    statusMessage[0] += controller.getStatusForAll();
                });
                EmailService.generateAndSendEmail(statusMessage[0]);
                break;
            case STOP:
                controllersCollection.forEach(controller -> {
                    if(currentAction.getKey().equals("ALL")){
                        controller.stopAllActors();
                    }
                    else if(controller.containsActor(currentAction.getKey())){
                        controller.stopActor(currentAction.getKey());
                    }
                });
                break;
            case START:
                controllersCollection.forEach(controller -> {
                    if(currentAction.getKey().equals("ALL")){
                        controller.startAllActors();
                    }
                    else if(controller.containsActor(currentAction.getKey())){
                        controller.startActor(currentAction.getKey());
                    }
                });
        }
    }

    private static void resetGrid() {
        Process p = null;
        logger.info("RESET GRID");
        try {
            p = Runtime.getRuntime().exec("bash stopGrid.sh");
            p.waitFor();

            p = Runtime.getRuntime().exec("bash startGrid.sh");
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
