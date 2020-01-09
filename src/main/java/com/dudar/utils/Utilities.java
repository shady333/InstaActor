package com.dudar.utils;

import com.google.common.base.Strings;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class Utilities {

    private static boolean gridReady(){
        return gridReady(null);
    }

    private static boolean gridReady(String urlGrid){
        String gridUrl;
        if(Strings.isNullOrEmpty(urlGrid)){
            gridUrl = "http://localhost:4444/";
        }
        else{
            gridUrl = urlGrid;
        }

        try {
            URL url = new URL(gridUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            int code = connection.getResponseCode();
            if(code == 200){
                return true;
            }
            else
            {
                return false;
            }
        }
        catch (Exception ex){
            System.out.print(".");
            return false;
        }
    }

    private static void checkGridStatus() {
        checkGridStatus(null);
    }

    public static void checkGridStatus(String hubUrl) {
        long waitGridReadyDuration = TimeUnit.SECONDS.toMillis(30);
        long currentTime = System.currentTimeMillis();
        System.out.print("Waiting for Grid to be ready - " + hubUrl);
        while(System.currentTimeMillis() < currentTime + waitGridReadyDuration){
            if(gridReady(hubUrl)){
               break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                ;
            }
        }
        System.out.println();
        if(!gridReady(hubUrl)){
            System.out.println("Selenium Grid is not ready");
            System.out.println("Terminating execution!!!");
            System.exit(0);
        }
    }
}
