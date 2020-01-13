package com.dudar.runner;

import com.codeborne.selenide.WebDriverRunner;
import com.dudar.InstaActor;
import com.dudar.utils.Utilities;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import static com.codeborne.selenide.Selenide.*;

public class Runner {

    private static RemoteWebDriver driver;

    public static void main(String[] args){

        boolean debug;
        List<String> hashTags;

        //Read configuration file
        String confFilePath = args[0];
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(confFilePath)) {
            // load a properties file
            prop.load(input);
        } catch (IOException ex) {
            System.out.println("!!!ERROR on properties initialization");
            ex.printStackTrace();
            System.exit(1);
        }

        debug = Boolean.parseBoolean(prop.getProperty("debug.mode"));

        //read all tags
        hashTags = Utilities.getAllTags(args[1]);
        hashTags.forEach(System.out::println);

        System.out.println("Starting");
        List<String> currentTags = hashTags;
        InstaActor actor = new InstaActor();
        actor.setLogin(prop.getProperty("acc.user"))
                .setPassword(prop.getProperty("acc.password"))
                .enableLikes(prop.getProperty("likes.enabled"));
        while(actor.getCompletedTags().size() != hashTags.size())
        {
            try {
                initDriver(debug);
                actor
                        .loadTags(currentTags)
                        .start();
            }
            catch (AssertionError ex) {
                System.out.println("!!!SELENIDE ASSERT ERROR");
                System.out.println("!!!Error on InstActor: " + ex.getLocalizedMessage());
                currentTags = exceptionClose(hashTags, actor);
            }
            catch (Exception ex){
                System.out.println("!!!OTHER EXCEPTION");
                currentTags = exceptionClose(hashTags, actor);
            }
        }

        clearBrowserLocalStorage();
        clearBrowserCookies();
        close();
        System.out.println("Total likes = " + actor.getTotalLikes());
        System.out.println("Shutting down!");
    }

    @NotNull
    private static List<String> exceptionClose(List<String> hashTags, InstaActor actor) {
        List<String> currentTags;
        System.out.println("Completed tags:");
        actor.getCompletedTags().forEach(System.out::println);
        currentTags = (List<String>) CollectionUtils.disjunction(hashTags, actor.getCompletedTags());
        try{
            clearBrowserLocalStorage();
            clearBrowserCookies();
            close();
        }
        catch (Exception ex){
            System.out.println("!!!Can't terminate driver");
            System.out.println(ex.getLocalizedMessage());
        }
        sleep(5000);
        return currentTags;
    }

    private static void initDriver(boolean debug) {
        if(!debug) {
            String seleniumHub = System.getenv("HUB_HOST");
            String seleniumHubPort = System.getenv("HUB_PORT");
            String gridHubUrl = "http://" + seleniumHub + ":" + seleniumHubPort;

            //Check grid status
            Utilities.checkGridStatus(gridHubUrl);
            try {
                DesiredCapabilities dc = DesiredCapabilities.chrome();
                dc.setCapability("headless", true);
                driver = new RemoteWebDriver(new URL(gridHubUrl+"/wd/hub"), dc);
            } catch (MalformedURLException e) {
                System.out.println("!!!Can't init DRIVER");
                System.out.println("Error message: " + e.getLocalizedMessage());
                driver = null;
            }
        }
        else {
            driver = new ChromeDriver();
        }
        WebDriverRunner.setWebDriver(driver);
    }
}
