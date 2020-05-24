package com.dudar.utils;

import com.google.common.base.Strings;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.log4j.Logger;

import javax.mail.internet.InternetAddress;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Utilities {

    final static Logger logger = Logger.getLogger(Utilities.class);
    private static Properties imaggaApiProperties = null;
    private static Properties emailProperties = null;

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

    public static boolean checkGridStatus(String hubUrl) {
        long waitGridReadyDuration = TimeUnit.SECONDS.toMillis(120);
        long currentTime = System.currentTimeMillis();
        logger.info("Waiting for Grid to be ready - " + hubUrl);
        while(System.currentTimeMillis() < currentTime + waitGridReadyDuration){
            if(gridReady(hubUrl)){
               return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
            logger.info(Utilities.getCurrentTimestamp() + "Terminating execution!!!");
        return false;
    }

    //TODO Combine into one List all values from CSV file, if it has more than 1 row
    public static List<String> getAllTags(String filePath){
        if(new File(filePath).isFile()){
            List<List<String>> records = new ArrayList<>();
            try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
                String[] values;
                while ((values = csvReader.readNext()) != null) {
                    records.add(Arrays.asList(values));
                }
                if(records.size()>0)
                    return records.get(0);
            } catch (IOException | CsvValidationException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>();
    }

    public static String getCurrentTimestamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Instant instant = timestamp.toInstant();
        return String.valueOf(instant).concat(" : ");
    }

    public static String getImaggaApiKey(){
        if(imaggaApiProperties == null){
            initImaggaProperties();
        }
        return imaggaApiProperties.getProperty("api.key");
    }

    public static String getImaggaApiSecret(){
        if(imaggaApiProperties == null){
            initImaggaProperties();
        }
        return imaggaApiProperties.getProperty("api.secret");
    }

    private static void initImaggaProperties(){
        logger.debug("Init imagga properties");
        try (InputStream input = new FileInputStream("data/access.properties")){
            imaggaApiProperties = new Properties();
            imaggaApiProperties.load(input);
        }
        catch (IOException ex){
            logger.error("Can't create Imagga properties", ex);
        }
    }

    private static void initEmailProperties(){
        logger.debug("Init Email properties");
        try (InputStream input = new FileInputStream("data/email.properties")){
            emailProperties = new Properties();
            emailProperties.load(input);
        }
        catch (IOException ex){
            logger.error("Can't create Eamil properties", ex);
        }
    }

    public static String getEmailTo(){
        getEmailPropertiesInstance();
        return emailProperties.getProperty("actions.email");
    }

    public static String getEmailSubject(){
        getEmailPropertiesInstance();
        return emailProperties.getProperty("subject.email");
    }

    public static String getEmailUserName(){
        getEmailPropertiesInstance();
        return emailProperties.getProperty("username.email");
    }

    public static String getEmailUserPassword(){
        getEmailPropertiesInstance();
        return emailProperties.getProperty("password.email");
    }

    public static String getActionsUserEmail(){
        getEmailPropertiesInstance();
        return emailProperties.getProperty("actions.email");
    }

    private static void getEmailPropertiesInstance() {
        if (emailProperties == null) {
            initEmailProperties();
        }
    }

    public static void writeListToFile(List listName, String fileName){

        try {
            FileWriter writer = new FileWriter(fileName);
            for(Object str : listName){
                writer.write((String)str + ",");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isNowTimeBetweenLimits(String startTime, String endTime) throws ParseException {
        String string1 = startTime;
        Date time1 = new SimpleDateFormat("HH:mm:ss").parse(string1);
        Calendar calendar1 = Calendar.getInstance();
        calendar1.setTime(time1);
        calendar1.add(Calendar.DATE, 1);
        String string2 = endTime;
        Date time2 = new SimpleDateFormat("HH:mm:ss").parse(string2);
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(time2);
        calendar2.add(Calendar.DATE, 1);
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date current = new Date();
        Date d = new SimpleDateFormat("HH:mm:ss").parse(formatter.format(current));
        Calendar calendar3 = Calendar.getInstance();
        calendar3.setTime(d);
        calendar3.add(Calendar.DATE, 1);
        Date x = calendar3.getTime();
        if (x.after(calendar1.getTime()) && x.before(calendar2.getTime())) {
            return true;
        }
        return false;
    }

    public static boolean isInternetConnection(){
        try {
            URL url = new URL("https://www.google.com/");
            URLConnection connection = url.openConnection();
            connection.connect();

            System.out.println("Connection Successful");
            return true;
        }
        catch (Exception e) {
            System.out.println("Internet Not Connected");
            return false;
        }
    }

}
