package com.dudar.runner;

import com.codeborne.selenide.WebDriverRunner;
import com.dudar.utils.Utilities;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

import static com.codeborne.selenide.Selenide.*;

public class Runner {

    public static void main(String[] args){

        RemoteWebDriver driver;

        boolean debug = false;





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

        System.out.println("I'm working");

        WebDriverRunner.setWebDriver(driver);

        open("https://google.com");
        sleep(3000);

        System.out.println(WebDriverRunner.getWebDriver().getTitle());

        clearBrowserLocalStorage();
        close();

        System.out.println("Shutting down!");
    }

}
