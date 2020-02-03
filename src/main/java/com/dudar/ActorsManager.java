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
                case REGISTER:
                    try {
                        Properties prop = new Properties();
                        prop.load(new FileInputStream("data/" + action.getKey() + "_user.properties"));
                        actorsMap.put(action.getKey(), new InstaActor2(action.getKey(), prop, Utilities.getAllTags("data/" + action.getKey() + "_tags.csv")));
                    } catch (IOException e) {
                        logger.error("Can't add item.");
                        logger.error(e.getMessage());
                        e.printStackTrace();
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
                    }
                    logger.info(action.getKey() + " is running");
                    break;
                case STOP:
                    actorsMap.get(action.getKey()).stop();
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
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public String getAllStatus() {
        String services = "";
        for(String name : actorsMap.keySet()){
            services += "* " + name + " is active - " + actorsMap.get(name).isAlive() + "<br/>";
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
