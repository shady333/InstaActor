package com.dudar;

import com.dudar.utils.Utilities;
import com.dudar.utils.services.ActorActions;
import com.dudar.utils.services.EmailService;
import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.testcontainers.shaded.org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class ActorsManager {

    final static Logger logger = Logger.getLogger(ActorsManager.class);

    Map<String, Actor> actorsMap = new HashMap<>();

    private static ActorsManager instance;

    private ActorsManager() {
        ;
    }

    public static ActorsManager getInstance(){
        if(instance == null){
            instance = new ActorsManager();
        }
        return instance;
    }

    public void trackActiveServices(){
        for(Actor value: actorsMap.values()){
            try {
                logger.debug(value.getNameForLog() + "isAlive - " + value.isAlive());
                logger.debug(value.getNameForLog() + "isActive - " + value.isActive());
                logger.debug(value.getNameForLog() + "isInterrupted - " + value.isInterrupted());
                logger.debug(value.getNameForLog() + "State - " + value.getState());

//            if(value.getState() == Thread.State.TERMINATED){
//                if(!value.isCompleted() & !value.isStopped()){
//                    value.start();
//                }
//            }
            }
            catch (Exception ex){
                logger.error("Can't get status for " + value.getNameForLog());
                logger.error(ex.getMessage());
            }
        }
    }

    public void startTerminatedInstances(){
        for(Actor currentActor: actorsMap.values()){
            try{
                if((currentActor.getState() == Thread.State.TERMINATED) && (!currentActor.isStopped())){
                    currentActor.start();
                }
            }
            catch (Exception ex){
                logger.error("Can't start terminated Actor - " + currentActor.getNameForLog());
            }
        }
    }

    public void proceedAction(AbstractMap.SimpleEntry<String, ActorActions> action){
            String message = "";
            switch (action.getValue()){
                case REBOOT:
                    message += "<p>Current actors status before reboot:";
                    message += "<p>" + ActorsManager.getInstance().getAllStatus();
                    EmailService.generateAndSendEmail(message);
                    ActorsManager.getInstance().resetAll();
                    initActorsFromDataFolder();
                    ActorsManager.getInstance().startAllRegistered();
                    break;
                case REMOVE:
                    if(!actorsMap.get(action.getKey()).isAlive()){
                        actorsMap.get(action.getKey()).stop();
                        actorsMap.keySet().removeIf(key -> key == action.getKey());
                        try {
                            TimeUnit.SECONDS.wait(30);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        logger.info("Removed Actor with key - " + action.getKey());
                    }
                case REGISTER:
                    if(!actorsMap.containsKey(action.getKey())){
                        try {
                            Properties prop = new Properties();
                            prop.load(new FileInputStream("data/" + action.getKey() + "_user.properties"));
                            actorsMap.put(action.getKey(), new InstaActor2(action.getKey(), prop, Utilities.getAllTags("data/" + action.getKey() + "_tags.csv")));
                            logger.info("Registered new Actor - " + action.getKey());
                        } catch (IOException e) {
                            logger.error("Can't add item.");
                            logger.error(e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    else{
                        logger.warn("Can't register Actor with name " + action.getKey() +
                                ".\nActor with same name already registered.");
                    }
                    break;
                case START:
                    if(!actorsMap.get(action.getKey()).isAlive()){
                        actorsMap.get(action.getKey()).start();
                        try{
                            Thread.sleep(10000);
                        }
                        catch (Exception ex){
                            ;
                        }
                        logger.info("Satrted Actor - " + action.getKey());
                    }
                    else
                        logger.info(action.getKey() + " is running");
                    break;
                case STOP:
                    actorsMap.get(action.getKey()).stop();
                    break;
                case ENABLELIKE:
                    actorsMap.get(action.getKey()).enableLikeAction();
                    break;
                case DISABLELIKE:
                    actorsMap.get(action.getKey()).disableLikeAction();
                    break;
                case ENABLECOMMENT:
                    actorsMap.get(action.getKey()).enableCommentsAction();
                    break;
                case DISABLECOMMENT:
                    actorsMap.get(action.getKey()).disableCommentsAction();
                    break;
                case STATUS:
                    if(action.getKey().equals("ALL"))
                    {
                        message += "<p>Registered actors:";
                        message += "<p>" + ActorsManager.getInstance().getAllStatus();
                        EmailService.generateAndSendEmail(message);
                    }
                    else{
                        actorsMap.get(action.getKey()).getStatus();
                    }
                    break;
                default:
                    ;
            }
    }

    public void initActorsFromDataFolder() {
        final File folder = new File("data");
        for (final File fileEntry : folder.listFiles()) {
            if (!fileEntry.isDirectory()) {
                String itemName = FilenameUtils.getBaseName(fileEntry.getAbsolutePath()).split("_")[0];
                ActorsManager.getInstance().proceedAction(new AbstractMap.SimpleEntry(itemName, ActorActions.REGISTER));
            }
        }
    }

    public void startAllRegistered(){
        actorsMap.entrySet().forEach(entry->{
            proceedAction(new AbstractMap.SimpleEntry<>(entry.getKey(), ActorActions.START));
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public String getAllStatus() {
        String services = "";
        for(String name : actorsMap.keySet()){
            services += "* " + name + " Alive - " + actorsMap.get(name).isAlive() + "<br/>";
            services += "* " + name + " Active - " + actorsMap.get(name).isActive() + "<br/>";
            services += "* " + name + " Stopped - " + actorsMap.get(name).isStopped() + "<br/>";
            services += "* " + name + " State - " + actorsMap.get(name).getThreadStatus() + "<br/>";
        }
        return services;
    }

    public void resetAll() {
        for(String name : actorsMap.keySet()){
            actorsMap.get(name).stop();
        }
        actorsMap = new HashedMap();
    }
}
