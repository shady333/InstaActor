package com.dudar.runner;

import com.codeborne.selenide.WebDriverRunner;
import com.google.common.base.Strings;
import com.rationaleemotions.GridApiAssistant;
import com.rationaleemotions.pojos.Host;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Selenide.*;

public class Runner {

    private static boolean gridReady(){
        return gridReady(null);
    }

    private static boolean gridReady(String urlGrid){
        String gridUrl = "";
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

    public static void main(String[] args){

//        String gridHubUrl = "http://localhost:4444/wd/hub";
        String gridHubUrl = "http://selenium-hub:4444/wd/hub";

        //Check grid status
        checkGridStatus(gridHubUrl);

        boolean debug = false;

        System.out.println("I'm working");

        RemoteWebDriver driver;

        if(!debug) {

            try {
                DesiredCapabilities dc = DesiredCapabilities.chrome();
//                driver = new RemoteWebDriver(new URL("http://selenium-hub:4444/wd/hub"), dc);
                driver = new RemoteWebDriver(new URL(gridHubUrl), dc);
            } catch (MalformedURLException e) {
                System.out.println("!!!Can't init DRIVER");
                System.out.println("Error message: " + e.getLocalizedMessage());
                driver = null;
            }

        }
        else
            {
            driver = new ChromeDriver();
        }
        WebDriverRunner.setWebDriver(driver);

        open("https://google.com");
        sleep(3000);

        clearBrowserLocalStorage();
        close();

        System.out.println("Shutting down!");
    }

    private static void checkGridStatus() {
        checkGridStatus(null);
    }

    private static void checkGridStatus(String hubUrl) {
        long waitGridReadyDuration = TimeUnit.SECONDS.toMillis(30);
        long currentTime = System.currentTimeMillis();
        System.out.print("Waiting for Grid to be ready ");
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
        System.out.println("");
        if(!gridReady(hubUrl)){
            System.out.println("Selenium Grid is not ready");
            System.out.println("Terminating execution!!!");
            System.exit(0);
        }
    }
}
