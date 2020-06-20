package phaseII;

import com.dudar.utils.Utilities;
import com.dudar.utils.services.ActorActions;
import com.dudar.utils.services.EmailService;
import com.dudar.utils.services.Emailer;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
                try{
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                resetGrid();
                try{
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.HOURS);

        controllersCollection.forEach(controller -> {
            try{
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Thread controllerThread = new Thread(controller);
            logger.info("Start for " + controller.toString());
            controllerThread.start();
            try{
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        while(true){
            if(!Utilities.isInternetConnection() && !wasStopped){

                try {
                    p = Runtime.getRuntime().exec("bash stopGrid.sh");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                p.waitFor();

                logger.warn("STOPPING ALL ACTORS. Out of Internet connection.");
                proceedAction(new AbstractMap.SimpleEntry<>("ALL", ActorActions.STOP));
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
                proceedAction(new AbstractMap.SimpleEntry<>("ALL", ActorActions.START));
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

                lastActionDate = new Date();
            }
            currentAction = null;

            TimeUnit.SECONDS.sleep(30);
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
                break;
            case DOWNLOAD:
                String propFilePath = "data/" + currentAction.getKey() + "_user.properties";
                EmailService.generateAndSendEmail(currentAction.getKey() + " PROPERTIES FILE", propFilePath);
                break;
            case UPLOAD:
                try {
                    Path from = Paths.get("tmp/" + currentAction.getKey() + "_user.properties"); //convert from File to Path
                    Path to = Paths.get("data/" + currentAction.getKey() + "_user.properties"); //convert from String to Path
                    Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
                    EmailService.generateAndSendEmail(currentAction.getKey() + "Properties file updated");
                }
                catch (IOException ex){
                    logger.error("Can't replace properties file\n" + ex.getMessage());
                }
                break;
            default:
                break;
        }
    }

    private static void resetGrid() {
        Process p = null;
        logger.info("RESET GRID");
        try {
            p = Runtime.getRuntime().exec("bash stopGrid.sh");
            logger.info("bash stopGrid.sh");
            p.waitFor();
            logger.info("p.waitFor()");

            try{
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            p = Runtime.getRuntime().exec("bash startGrid.sh");
            logger.info("bash startGrid.sh");
            p.waitFor();
            logger.info("p.waitFor()");

            try{
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("RESET GRID Completed");
    }
}
