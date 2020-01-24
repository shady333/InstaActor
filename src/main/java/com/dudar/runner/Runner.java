package com.dudar.runner;

import com.codeborne.selenide.WebDriverRunner;
import com.dudar.InstaActor;
import com.dudar.utils.Utilities;
import com.dudar.utils.services.EmailService;
import com.google.common.base.Strings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;

import javax.mail.MessagingException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import static com.codeborne.selenide.Selenide.*;

public class Runner {

    final static Logger logger = Logger.getLogger(Runner.class);

    private static RemoteWebDriver driver;
    private static Properties prop = new Properties();

    public static void main(String[] args){
        boolean debug;
        List<String> hashTags;
        String confFilePath = args[0];
        try (InputStream input = new FileInputStream(confFilePath)) {
            prop.load(input);
        } catch (IOException ex) {
            System.out.println("!!!ERROR on properties initialization");
            ex.printStackTrace();
            System.exit(1);
        }
        debug = Boolean.parseBoolean(prop.getProperty("debug.mode"));
        //TODO better way to combine tags
        hashTags = Utilities.getAllTags(args[1]);
        hashTags.forEach(System.out::println);
        System.out.println("Starting...");
        List<String> currentTags = hashTags;
        InstaActor actor = new InstaActor();
        actor.setLogin(prop.getProperty("acc.user"))
                .setPassword(prop.getProperty("acc.password"))
                .enableLikes(prop.getProperty("likes.enabled"))
                .enableComments(prop.getProperty("comments.enabled"))
                .setMaxPostsCount(Integer.valueOf(prop.getProperty("posts.count")))
                .setMinViewDelay(Integer.valueOf(prop.getProperty("view.min.delay")))
                .setMaxViewDelay(Integer.valueOf(prop.getProperty("view.max.delay")))
                .setMinVideoDelay(Integer.valueOf(prop.getProperty("video.min.delay")))
                .setMaxVideoDelay(Integer.valueOf(prop.getProperty("video.max.delay")))
                .setLikesPercentage(Integer.valueOf(prop.getProperty("likes.percentage")))
                .setCommentsPercentage(Integer.valueOf(prop.getProperty("comments.percentage")));
        actor.viewCurrentParameters();
        while(actor.getCompletedTags().size() != hashTags.size())
        {
            try {
                initDriver(debug);
                actor
                        .loadTags(currentTags)
                        .start();
            }
            catch (AssertionError err) {
                currentTags = exceptionClose(hashTags, actor, "ASSERT ERRROR\n"+err.getLocalizedMessage());
            }
            catch (Exception ex){
                System.out.println("!!!OTHER EXCEPTION");
                currentTags = exceptionClose(hashTags, actor, "EXCEPTION\n"+ex.getLocalizedMessage());
            }
        }
        clearBrowserLocalStorage();
        clearBrowserCookies();
        WebDriverRunner.getWebDriver().quit();
        getCurrentStateForCompletedActions(actor);
        try {
            EmailService.generateAndSendEmail("Work Completed</br> + Likes added: " + actor.getTotalLikes() + "</br>");
        }
        catch (MessagingException e) {
            logger.debug(e.getLocalizedMessage());
        }
        System.out.println("Shutting down!");
    }

    @NotNull
    private static List<String> exceptionClose(List<String> hashTags, InstaActor actor, String issueMessage) {
        List<String> currentTags;
        logger.error("UNEXPECTED STOP INFO:\n");
        logger.error(issueMessage);

        getCurrentStateForCompletedActions(actor);
        currentTags = (List<String>) CollectionUtils.disjunction(hashTags, actor.getCompletedTags());
        try{
            clearBrowserLocalStorage();
            clearBrowserCookies();
            WebDriverRunner.getWebDriver().quit();
        }
        catch (Exception ex){
            logger.error("!!!Can't terminate driver");
            logger.error(ex.getLocalizedMessage());
        }
        sleep(5000);
        return currentTags;
    }

    private static void getCurrentStateForCompletedActions(InstaActor actor) {
        System.out.println("Likes added: " + actor.getTotalLikes());
        System.out.println("Comments added: " + actor.getTotalComments());
        System.out.println("Completed tags:");
        actor.getCompletedTags().forEach(System.out::println);
        System.out.println("Defected tags");
        actor.printDefectedTags();
        System.out.println("**********************InstaActor*******************");
    }

    private static void initDriver(boolean debug) {
        if(!debug) {
            String seleniumHub = System.getenv("HUB_HOST");
            String seleniumHubPort = System.getenv("HUB_PORT");
            if(Strings.isNullOrEmpty(seleniumHub) || Strings.isNullOrEmpty(seleniumHubPort)){
                seleniumHub = prop.getProperty("hub.host");
                seleniumHubPort = prop.getProperty("hub.port");
            }
            String gridHubUrl = "http://" + seleniumHub + ":" + seleniumHubPort;
            //Check grid status
            Utilities.checkGridStatus(gridHubUrl);
            try {
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR,
                        UnexpectedAlertBehaviour.IGNORE);
                chromeOptions.setHeadless(true);
                driver = new RemoteWebDriver(new URL(gridHubUrl+"/wd/hub"), chromeOptions);
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
