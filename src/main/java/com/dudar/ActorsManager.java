package com.dudar;

import java.util.HashMap;
import java.util.Map;

public class ActorsManager {

    Map<String, Actor> actorsMap = new HashMap<>();

    private static ActorsManager instance;

    private ActorsManager() {
        ;
    }

    public static ActorsManager getInstance(){
        if(instance != null){
            instance = new ActorsManager();
        }
        return instance;
    }

}
