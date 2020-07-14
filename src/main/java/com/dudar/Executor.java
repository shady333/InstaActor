package com.dudar;

import com.dudar.utils.Utilities;
import com.dudar.utils.services.ActorActions;
import com.dudar.utils.services.EmailService;
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

public class Executor {

    final static Logger logger = Logger.getLogger(Executor.class);
    static List<Controller> controllersCollection = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        AbstractMap.SimpleEntry<String, ActorActions> currentAction = null;
        Process p = null;

        Date lastActionDate = new Date();
        boolean wasStopped = false;

        Runnable controller1 = new Controller("controller1");
        ((Controller) controller1).registerActor("3dprint");
        ((Controller) controller1).registerActor("bricks");
        ((Controller) controller1).registerActor("legomini");
        ((Controller) controller1).registerActor("snail");
        ((Controller) controller1).registerActor("neverold");
        ((Controller) controller1).registerActor("inline");

        controllersCollection.add((Controller) controller1);

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
        }, 0, 6, TimeUnit.HOURS);

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

        while(true) {
            try{
                if (!Utilities.isInternetConnection() && !wasStopped) {

                    try {
                        p = Runtime.getRuntime().exec("bash stopGrid.sh");
                        p.waitFor();

                        logger.warn("STOPPING ALL ACTORS. Out of Internet connection.");
                        controllersCollection.stream().forEach(controller -> {
                            controller.proceedAction(new AbstractMap.SimpleEntry<>("ALL", ActorActions.STOP));
                        });

                        wasStopped = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                if (wasStopped && Utilities.isInternetConnection()) {


                    try {
                        p = Runtime.getRuntime().exec("bash startGrid.sh");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    p.waitFor();

                    logger.warn("START ALL ACTORS. Internet connection resumed.");
                    controllersCollection.stream().forEach(controller -> {
                        controller.proceedAction(new AbstractMap.SimpleEntry<>("ALL", ActorActions.START));
                    });
                    wasStopped = false;
                }

                logger.info("Tick from executor");
                try {
                    currentAction = EmailService.getActionFromEmail(Utilities.getActionsUserEmail(), lastActionDate);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
                if (currentAction != null && currentAction.getValue() != ActorActions.UNDEFINED) {
                    logger.info("Received action:" + currentAction.getKey() + "; " + currentAction.getValue());

                    AbstractMap.SimpleEntry<String, ActorActions> finalCurrentAction = currentAction;
                    controllersCollection.stream().forEach(controller -> {
                        controller.proceedAction(finalCurrentAction);
                    });

                    lastActionDate = new Date();
                }
            }
            catch (Exception ex){
                logger.error("Executor: " + ex.getMessage());
                EmailService.generateAndSendEmail("EXECUTOR EXCEPTION:\n" + ex.getMessage());
            }
            currentAction = null;

            TimeUnit.SECONDS.sleep(30);
        }
    }

    private static void resetGrid() {
        Process p = null;

        logger.info("Deleting build folder");
        clearBuildFolder();

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
        EmailService.generateAndSendEmail("RESET GRID Completed\n");
    }

    static void clearBuildFolder(){
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("rm -rf build");
            p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
