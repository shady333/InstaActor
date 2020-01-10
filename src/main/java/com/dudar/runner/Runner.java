package com.dudar.runner;

import com.codeborne.selenide.WebDriverRunner;
import com.dudar.InstaActor;
import com.dudar.utils.Utilities;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.codeborne.selenide.Selenide.*;

public class Runner {

    public static void main(String[] args){

        boolean debug = true;

        RemoteWebDriver driver;

        List<String> hashTags = new ArrayList<>();



        //Read configuration file
        String confFilePath = args[0];
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(confFilePath)) {



            // load a properties file
            prop.load(input);

            // get the property value and print it out
            System.out.println(prop.getProperty("hub.host"));
            System.out.println(prop.getProperty("hub.port"));
            System.out.println(prop.getProperty("acc.user"));
            System.out.println(prop.getProperty("acc.password"));

        } catch (IOException ex) {
            ex.printStackTrace();
        }


        //read all tags
        hashTags = Utilities.getAllTags(args[1]);

        hashTags.forEach(el -> System.out.println(el));

        if(!debug) {

            String seleniumHub = System.getenv("HUB_HOST");
            String seleniumHubPort = System.getenv("HUB_PORT");

            String gridHubUrl = "http://" + seleniumHub + ":" + seleniumHubPort;

            //Check grid status
            Utilities.checkGridStatus(gridHubUrl);

            try {
                DesiredCapabilities dc = DesiredCapabilities.chrome();
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

        System.out.println("Starting");

        WebDriverRunner.setWebDriver(driver);

        //open("https://google.com");
        //sleep(3000);

        System.out.println(WebDriverRunner.getWebDriver().getTitle());

        InstaActor actor = new InstaActor();
        actor.setLogin(prop.getProperty("acc.user"))
                .setPassword(prop.getProperty("acc.password"))
                .loadTags(hashTags)
                .build()
        .start();

        clearBrowserLocalStorage();
        close();

        System.out.println("Shutting down!");
    }

}
