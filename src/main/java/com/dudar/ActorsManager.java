package com.dudar;

import com.dudar.runner.Runner2;
import com.dudar.utils.Utilities;
import com.dudar.utils.services.ActorActions;
import org.apache.log4j.Logger;

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
        if(!actorsMap.containsKey(action.getKey())){
            if(action.getValue() == ActorActions.START){
                ////!!!!! ONLY FOR LOCAL TEST USAGE
                if(action.getKey().equalsIgnoreCase("inline")){
                    Properties prop = new Properties();
                    try {
                        prop.load(new FileInputStream("data/inline_user.properties"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    actorsMap.put(action.getKey(), new InstaActor2(action.getKey(), prop, Utilities.getAllTags("data/inline_tags.csv")));
                }
                if(action.getKey().equalsIgnoreCase("bricks")){
                    Properties prop = new Properties();
                    try {
                        prop.load(new FileInputStream("data/bricks_user.properties"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    actorsMap.put(action.getKey(), new InstaActor2(action.getKey(), prop, Utilities.getAllTags("data/bricks_tags.csv")));
                }
                if(action.getKey().equalsIgnoreCase("neverold")){
                    Properties prop = new Properties();
                    try {
                        prop.load(new FileInputStream("data/neverold_user.properties"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    actorsMap.put(action.getKey(), new InstaActor2(action.getKey(), prop, Utilities.getAllTags("data/neverold_tags.csv")));
                }


//                actorsMap.put(action.getKey(), new InstaActor2(action.getKey()));
                actorsMap.get(action.getKey()).start();
            }
        }
        else{
            switch (action.getValue()){
                case START:
                    if(!actorsMap.get(action.getKey()).isAlive()){
                        actorsMap.get(action.getKey()).start();
                    }
                    logger.info(action.getKey() + " is running");
                    break;
                case STOP:
                    actorsMap.get(action.getKey()).stop();
                    break;
                case STATUS:
                    actorsMap.get(action.getKey()).getStatus();
                    break;
                default:
                    ;
            }
        }
    }

}
