package com.dudar.utils;

import com.google.common.base.Strings;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Utilities {

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
            return code == 200;
        }
        catch (Exception ex){
            System.out.print(".");
            return false;
        }
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
            }
        }
        System.out.println();
        if(!gridReady(hubUrl)){
            System.out.println("Selenium Grid is not ready");
            System.out.println("Terminating execution!!!");
            System.exit(0);
        }
    }

    //TODO Combine into one List all values from CSV file, if it has more than 1 row
    public static List<String> getAllTags(String filePath){
        List<List<String>> records = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                records.add(Arrays.asList(values));
            }
            return records.get(0);
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getCurrentTimestamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Instant instant = timestamp.toInstant();
        return String.valueOf(instant).concat(" : ");
    }
}
