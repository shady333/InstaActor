package com.dudar;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.dudar.utils.ImageAnalyzer;
import com.dudar.utils.Utilities;
import com.dudar.utils.services.EmailService;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codeborne.selenide.Selenide.*;

public class InstaActor2 implements Runnable, Actor {

    private boolean detectMediaContant = false;
    private int crashCounter = 0;

    private AtomicBoolean running = new AtomicBoolean(false);

    enum PostType{
        PHOTO,
        VIDEO,
        GALLERY,
        UNDEFINED
    }

    private Map<String, ArrayList> processedPosts = new HashMap<>();

    private List<String> allTags = null;
    private static RemoteWebDriver driver;
    private boolean isStopped = true;
    private boolean isCompleted = false;
    private boolean isActive = false;
    private String name;
    private Thread t = null;
    private Date creationDate;

    final static Logger logger = Logger.getLogger(InstaActor2.class);
    private int viewMinDalay;
    private int viewMaxDelay;
    private int viewMinDelayVideo;
    private int viewMaxDelayVideo;
    private int likesPercentage;
    private int commentsPercentage;
    private int maxPostsCount;
    private String userName;
    private String userPass;
    private boolean likesEnabled = false;
    private boolean commentsEnabled = false;
    private boolean debugMode = false;
    private List<String> completedTags = new ArrayList<>();
    private String currentTag = "";
    private List<String> defectedTags = new ArrayList<>();
    private String currentPostUrl = "";
    private PostType currentPostType = PostType.UNDEFINED;
    private boolean currentPostLikeAdded = false;
    private int totalLiked = 0;
    private int warningsCounter = 0;
    private String addedComment = "";
    private String currentStatus = "";
    private boolean interrupted = false;

    private int totalComments = 0;

    public InstaActor2(String name){
        this.name = name;
        creationDate = new Date();
    }

    public InstaActor2(String name, Properties actorProperties, List<String> tags){
        this.name = name;
        creationDate = new Date();
        allTags = tags;
        setupProperties(actorProperties);
    }

    private void setupProperties(Properties actorProperties) {
        if(!StringUtils.isEmpty(actorProperties.getProperty("view.min.delay")))
            viewMinDalay = Integer.parseInt(actorProperties.getProperty("view.min.delay"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("view.max.delay")))
            viewMaxDelay = Integer.parseInt(actorProperties.getProperty("view.max.delay"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("video.min.delay")))
            viewMinDelayVideo = Integer.parseInt(actorProperties.getProperty("video.min.delay"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("video.max.delay")))
            viewMaxDelayVideo = Integer.parseInt(actorProperties.getProperty("video.max.delay"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("likes.percentage")))
            likesPercentage = Integer.parseInt(actorProperties.getProperty("likes.percentage"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("comments.percentage")))
            commentsPercentage = Integer.parseInt(actorProperties.getProperty("comments.percentage"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("posts.count")))
            maxPostsCount = Integer.parseInt(actorProperties.getProperty("posts.count"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("acc.user")))
            userName = actorProperties.getProperty("acc.user");
        if(!StringUtils.isEmpty(actorProperties.getProperty("acc.password")))
            userPass = actorProperties.getProperty("acc.password");
        if(!StringUtils.isEmpty(actorProperties.getProperty("likes.enabled")))
            likesEnabled = Boolean.parseBoolean(actorProperties.getProperty("likes.enabled"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("comments.enabled")))
            commentsEnabled = Boolean.parseBoolean(actorProperties.getProperty("comments.enabled"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("debug.mode")))
            debugMode = Boolean.parseBoolean(actorProperties.getProperty("debug.mode"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("detect.media.content")))
            detectMediaContant = Boolean.parseBoolean(actorProperties.getProperty("detect.media.content"));
    }

    @Override
    public boolean isAlive(){
        return running.get();
    }

    public String getThreadStatus(){
        if(t != null)
            return t.getState().toString();
        else
            return "UNDEFINED";
    }

    @Override
    public boolean isCompleted() {
        return isCompleted;
    }

    @Override
    public boolean isStopped() {
        return isStopped;
    }

    @Override
    public boolean isInterrupted(){
        return t.isInterrupted();
    }

    @Override
    public Thread.State getState(){
        return t.getState();
    }

    private static void initDriver(boolean debug) {
        if(!debug) {
            String seleniumHub = System.getenv("HUB_HOST");
            String seleniumHubPort = System.getenv("HUB_PORT");
            if(Strings.isNullOrEmpty(seleniumHub) || Strings.isNullOrEmpty(seleniumHubPort)){
                seleniumHub = "localhost";
                seleniumHubPort = "4444";
            }
            String gridHubUrl = "http://" + seleniumHub + ":" + seleniumHubPort;
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

    private void authentificate() {
        open("https://www.instagram.com/accounts/login/?source=auth_switcher");
        sleep(getRandonTimeout());
        $(By.name("username")).val(userName).pressTab();
        $(By.name("password")).val(userPass).pressEnter();
        sleep(3000);
    }

    private void checkIfPopupShown() {

        $(By.cssSelector("input[placeholder=\"Search\"]")).shouldBe(Condition.visible);
        $(By.cssSelector("svg[aria-label=\"Instagram\"]")).shouldBe(Condition.visible);
        ElementsCollection popupWindow = $$(By.xpath("//div[attribute::role='dialog']"));
        if(popupWindow.size() > 0){
            String popupText = popupWindow.get(0).find("h2").getText();
            System.out.println("!!!Popup detected - " + popupText);
            if(popupText.equalsIgnoreCase("Turn on Notifications")){
                mouseMoveToElementAndClick($(By.xpath("//button[contains(text(), 'Not Now')]")));
            }
        }
    }

    private int getRandonTimeout(){
        return ThreadLocalRandom.current().nextInt(viewMinDalay, viewMaxDelay + 1);
    }

    private int getVideoRandonTimeout(){
        return ThreadLocalRandom.current().nextInt(viewMinDelayVideo, viewMaxDelayVideo + 1);
    }

    private void mouseMoveToElementAndClick(WebElement element){
        sleep(getRandonTimeout());
        Actions action = new Actions(WebDriverRunner.getWebDriver());
        action.moveToElement(element).perform();
        element.click();
        sleep(getRandonTimeout());
    }

    private boolean searchByTag(String searchTag) {
        SelenideElement searchBox = $(By.cssSelector("input[placeholder=\"Search\"]")).shouldBe(Condition.visible);
        mouseMoveToElementAndClick($(By.xpath("//span[contains(text(),'Search')]")));
        searchBox.val("#"+searchTag);
        sleep(3000);
        $(By.xpath("//div[contains(@class,'SearchClear')]")).shouldBe(Condition.visible);
        searchBox.sendKeys(Keys.DOWN, Keys.ENTER);
        sleep(getRandonTimeout());
        $(By.cssSelector("svg[aria-label=\"Instagram\"]")).shouldBe(Condition.visible);
        SelenideElement tagLocator = $(By.cssSelector("main h1"));
        sleep(5000);
        logger.info(name + " >>> "  + "Current page Tag - "+tagLocator.getText());
        if(tagLocator.getText().equalsIgnoreCase("#"+searchTag)){
            currentTag = searchTag;
            return true;
        }
        else{
            System.out.println("!!! Can't find  search tag page. Search Tag - "+searchTag);
            defectedTags.add(searchTag);
            return false;
        }
    }

    private void detectPostTypeAndAct() {
        ElementsCollection imagePost = $$(By.xpath("//div[attribute::role='dialog']//article//img[attribute::style='object-fit: cover;']"));
        if(imagePost.size()>0){
            if(imagePost.size()==1){
                try {
                    String imageUrl = imagePost.get(0).getAttribute("srcset").split(" ")[0];
                    URL imageURL = new URL(imageUrl);
                    BufferedImage saveImage = ImageIO.read(imageURL);
                    String savedImagePath = "tmp/current_post_image.jpg";
                    ImageIO.write(saveImage, "jpg", new File(savedImagePath));

                    //TODO Image Recognition
                    if(detectMediaContant)
                        ImageAnalyzer.imageType(savedImagePath);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                currentPostType = PostType.PHOTO;
                return;
            }
            else
            {
                for(int i =1; i < imagePost.size(); i++){
                    System.out.println("Navigate to next image > " + i);
                    mouseMoveToElementAndClick($(By.cssSelector(".coreSpriteRightChevron")));
                }
                currentPostType = PostType.GALLERY;
                return;
            }
        }
        imagePost = $$(By.xpath("//div[attribute::role='dialog']//article//video[attribute::type='video/mp4']"));
        if(imagePost.size() > 0)
        {
            WebElement videoButton = $(By.xpath(
                    "//div[attribute::role='dialog']//article//video[attribute::type='video/mp4']/../../../../..")).shouldBe(Condition.enabled);
            videoButton.click();
            sleep(getVideoRandonTimeout());
            videoButton.click();
            currentPostType = PostType.VIDEO;
            return;
        }
        return;
    }

    private boolean likePost(){
        int min = 1;
        int max = 100;
        boolean result;
        if(ThreadLocalRandom.current().nextInt(min, max) <= likesPercentage) {
            result = true;
        }
        else {
            result = false;
        }
        return result;
    }

    private void addLikeToPost() {
        logger.info(name + " >>> "  + "Like post");
        mouseMoveToElementAndClick(InstaActorElements.getPostLikeButton());
        currentPostLikeAdded = true;
        totalLiked++;
    }

    private void resetCurrentPostStatus() {
        currentPostLikeAdded = false;
        addedComment = "";
        currentPostType = PostType.UNDEFINED;
        currentStatus = "";
    }

    private boolean suspectedActionsDetector() {
        ElementsCollection buttonReport = $$(By.xpath("//button[contains(text(),'Report a Problem')]"));
        if(buttonReport.size() > 0){
            warningsCounter++;
            if(warningsCounter>2){
                logger.warn(name + " >>> "  + "!!!WARNING!!!");
                logger.warn(name + " >>> "  + "SKIP CURRENT TAG Liking\nBREAK!!!!");
                System.out.println("Completed tags:");
                completedTags.forEach(System.out::println);
                System.out.println("!!!STOP EXECUTION");
                EmailService.generateAndSendEmail("Unexpected behavior - Stop Action!!! for:<br/>"
                            +"<b>Tag name:</b> " + currentTag + "<br/>"
                            +"<b>Post Url:</b> " + currentPostUrl + "<br/>", screenshot("tmp/crash/chash_info.png"));
                buttonReport.get(0).click();
                stopExecution();
                resetCurrentPostStatus();
                interrupted = true;
                return true;
            }
            logger.warn(name + " >>> "  + "!!!WARNING!!!");
            logger.warn(name + " >>> "  + "Detected suspicious action detected by service");
            buttonReport.get(0).click();
            sleep(getRandonTimeout());
            if(InstaActorElements.getPostLikeButton()!=null) {
                System.out.println("Re Like current post");
                if(likesEnabled)
                    mouseMoveToElementAndClick(InstaActorElements.getPostLikeButton());
                sleep(getRandonTimeout());
            }
            System.out.println("Switching to next tag for likes");
            resetCurrentPostStatus();
            return true;
        }
        return false;
    }

    private boolean commentPost(){
        int min = 1;
        int max = 100;
        boolean result = false;
        if(ThreadLocalRandom.current().nextInt(min, max) <= commentsPercentage) {
            System.out.println(Utilities.getCurrentTimestamp() + "Add Comment");
            result = true;
        }
        return result;
    }

    private String getComment(){
        if(ThreadLocalRandom.current().nextInt(0, 100) > 50){
            int maxVal = InstaActorComments.comments.size();
            int commentIndex = ThreadLocalRandom.current().nextInt(0, maxVal);
            return InstaActorComments.comments.get(commentIndex);
        }
        else{
            if(this.currentPostType == PostType.VIDEO){
                logger.info(name + " >>> "  + "Return Video comment");
                return InstaActorComments.commentsVideo.get(ThreadLocalRandom.current().nextInt(0, InstaActorComments.commentsVideo.size()));
            }
            logger.info(name + " >>> "  + "Return Other Content comment");
            return
                    InstaActorComments.comment1.get(ThreadLocalRandom.current().nextInt(0, InstaActorComments.comment1.size()))
                            .concat(
                                    InstaActorComments.comment2.get(ThreadLocalRandom.current().nextInt(0, InstaActorComments.comment2.size()))
                            ).concat(
                            InstaActorComments.comment3.get(ThreadLocalRandom.current().nextInt(0, InstaActorComments.comment3.size()))
                    );
        }
    }

    private void addCommentToPost(){
        if(commentPost())
        {
            try {
                String commentText = getComment();
                logger.debug(name + " >>> "  + "Trying to add comment: " + commentText);
                $(By.cssSelector("article textarea")).val(commentText);

                //TODO add emojji support
                //Commented part for posting emojji, not working yet
//                String JS_ADD_TEXT_TO_INPUT = "var elm = arguments[0], txt = arguments[1]; elm.value += txt; elm.dispatchEvent(new Event('change'));";
//                WebElement textBox = $(By.cssSelector("article textarea"));
//                executeJavaScript(JS_ADD_TEXT_TO_INPUT, textBox, commentText);

                mouseMoveToElementAndClick($(By.xpath("//button[attribute::type='submit']")));
                logger.info(name + " >>> "  + "Comment added: " + commentText);
                totalComments++;
                addedComment = commentText;
            } catch (Error err) {
                logger.error(name + " >>> "  + "ERROR on commenting" + err.getLocalizedMessage());
            }
        }
        else{
            logger.info(name + " >>> "  + "Skip comment");
        }
    }

    private void interactWithPosts(int maxPostsCount) throws InstaActorStopExecutionException {
        String rootElement = "//div[contains(text(), 'Top posts')]/../..";
        $(By.xpath(rootElement)).shouldBe(Condition.enabled).scrollIntoView(true);
        sleep(getRandonTimeout());
        WebElement firstPostToLike = $(By.xpath(rootElement+"//a")).shouldBe(Condition.enabled);
        mouseMoveToElementAndClick(firstPostToLike);

        //TODO detect count of available posts. Should not exceed maxPostsCount
        for(int i = 1; i <= maxPostsCount; i++){

            if(!running.get()){
                throw new InstaActorStopExecutionException();
            }

            currentPostUrl = WebDriverRunner.url();
            InstaActorElements.getPostCloseButton().shouldBe(Condition.visible).shouldBe(Condition.enabled);
            if(InstaActorElements.getPostLikeButton()!=null){
                sleep(getRandonTimeout());
                if(processedPosts.get(currentTag).contains(currentPostUrl)){
                    logger.info(name + " >>> "  + "Post was already processed");
                    logger.info(name + " >>> "  + "SKIP - " + currentPostUrl);
                    continue;
                }
                detectPostTypeAndAct();
                if(likePost()){
                    if(likesEnabled) {
                        addLikeToPost();
                        if (suspectedActionsDetector())
                            return;
                    }
                    else{
                        logger.info(name + " >>> "  + "!!!Likes option is disabled");
                    }
                    if(commentsEnabled) {
                        //TODO Post comment according to image type
                        addCommentToPost();
                        if (suspectedActionsDetector())
                            return;
                    }
                }
            }
            currentStatus += "\n/***************InstaActor " + name + " POST INFO*****************/\n";
            currentStatus += "|\n";
            currentStatus += "|   Tag: " + currentTag + ".\n";
            currentStatus += "|   Number is " + i + " from " + maxPostsCount + ".\n";
            currentStatus += "|   Url: " + currentPostUrl + ".\n";
            currentStatus += "|   Type: " + currentPostType.toString() + ".\n";
            currentStatus += "|   Like: " + currentPostLikeAdded + ".\n";
            if(!Strings.isNullOrEmpty(addedComment)){
                currentStatus += "|   Added comment: " + addedComment + ".";
            }
            currentStatus += "|\n";
            logger.info(name + " >>> "  + "Current post info:\n" + currentStatus);
            ArrayList<String> items = processedPosts.get(currentTag);
            items.add(currentPostUrl);
            processedPosts.put(currentTag, items);
            resetCurrentPostStatus();
            WebElement nextPostButton = $(By.xpath("//a[contains(text(), 'Next')]")).shouldBe(Condition.visible);
            mouseMoveToElementAndClick(nextPostButton);
            sleep(getRandonTimeout());
        }
    }

    public String viewCurrentParameters(){
        String currentStatus = "*****InstaActor Parameters*****\n"
                + "Name - " + name + "\n"
                + "Like enabled - " + likesEnabled + "\n"
                + "Like percentage - " + likesPercentage + "\n"
                + "Comment enabled - " + commentsEnabled + "\n"
                + "Comment percentage - " + commentsPercentage +"\n"
                + "View parameters: " + viewMinDalay + " " + viewMaxDelay + "\n"
                + "Video parameters: " + viewMinDelayVideo + " " + viewMaxDelayVideo + "\n"
                + "*****InstaActor Parameters*****";
        logger.info(currentStatus);
        return currentStatus;
    }

    @Override
    public void run() {
        running.set(true);
        while (running.get()) {
            EmailService.generateAndSendEmail(viewCurrentParameters().replaceAll("\n", "<br/>"));
            interrupted = false;
            isStopped = false;
            if (isCompleted) {
                logger.info("All tags were processed");
                EmailService.generateAndSendEmail(generateStatusForEmail());
                stopExecution();
            }
            while (!isCompleted & !isStopped) {
                if (crashCounter > 10) {
                    stopExecution();
                    EmailService.generateAndSendEmail("CrashCounter exceed max value for - <b>" + name
                            + "</b><p>Stop execution.<p>"+generateStatusForEmail());
                    break;
                }
                try {
                    initDriver(debugMode);
                    isActive = true;
                    authentificate();
                    checkIfPopupShown();
                    Collections.shuffle(allTags);
                    int tagsCollectionSize = allTags.size();
                    AtomicInteger tagCounter = new AtomicInteger(1);
                    for (String searchTag : allTags) {
                        processedPosts.put(searchTag, new ArrayList());
                        if (!completedTags.contains(searchTag)) {
                            completedTags.add(searchTag);
                            logger.info(name + " >>> " + "Current tag is " + tagCounter + " from " + tagsCollectionSize + " all of Tags");
                            tagCounter.getAndIncrement();
                            if (searchByTag(searchTag)) {
                                interactWithPosts(maxPostsCount);
                                if (!interrupted) {
                                    WebElement closeButton = InstaActorElements.getPostCloseButton().shouldBe(Condition.visible);
                                    mouseMoveToElementAndClick(closeButton);
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                    isCompleted = true;
                    //stopExecution();
                    logger.info(getStatus());
                }
                catch (AssertionError err){
                    logger.info("Selenide error: " + err.getMessage());
                    EmailService.generateAndSendEmail("SELENIDE Assert Error: " + err.getMessage());
                    crashCounter++;
                }
                catch (InstaActorStopExecutionException ex) {
                    running.set(false);
                    isStopped = true;
                    String message = name + ">>> Execution stopped!!!";
                    logger.info(message);
                    EmailService.generateAndSendEmail(message + "<p>" + generateStatusForEmail());
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                    EmailService.generateAndSendEmail("<p> Service <b>" + name + "</b> crashed with exception:<p>"
                            + ex.getMessage());
                    isActive = false;
                    crashCounter++;
                } finally {
                    clearSession();
                }
            }
        }
    }

    private void clearSession(){
        try{
            logger.error(name + " >>> "  + "Clear WebDriver session");
            driver = null;
        }
        catch (IllegalStateException ex){
            logger.error(name + " >>> "  + ex.getMessage());
        }
        catch (Exception ex){
            logger.error(name + " >>> "  + ex.getMessage());
        }
    }

    private void workMethod() {
        logger.info(name + " >>> "  + "Doing some work ...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Actor start () {
        logger.info(name + " >>> "  + "Starting...");
        isStopped = false;
        crashCounter = 0;
        if (t == null) {
            t = new Thread (this, name);
            t.start ();
        } else if (t.getState() == Thread.State.TERMINATED) {
            logger.info("Starting not active thread");
            t = new Thread (this, name);
            t.start ();
        }
        return this;
    }

    @Override
    public Actor stop() {
        logger.info(name + " >>> "  + "STOP");
        clearSession();
        stopExecution();
        return this;
    }

    private String generateStatusForEmail(){
        String status = "<h1>InstaActor STATUS</h1>"
                +"<p>Sevice name: " + name
                +"<p>Sevice is running: " + isAlive()
                +"<p>Was interruped: " + interrupted
                +"<p>Completed Tags: " + completedTags.size()
                +"<p>Tag: " + currentTag + " from " + allTags.size()
//                +"Current post number " + i + " from " + maxPostsCount + ".\n";
                + "<p>Url: " + currentPostUrl
                + "<p>Type: " + currentPostType.toString()
                + "<p>Like: " + currentPostLikeAdded;
        if(!Strings.isNullOrEmpty(addedComment)){
            status += "<p>Added comment: " + addedComment;
        }
        status += "<p>Likes added Total: " + totalLiked;
        status += "<p>Comments added Total: " + totalComments;
        return status;
    }

    @Override
    public String getStatus() {
        String currentStatus = "/**** Insta Actor "+name+" ****/\n";
        currentStatus += "|\n";
        currentStatus += "|********************************\n";
        logger.info(name + " >>> "  + "Current status:\n" + currentStatus);
        EmailService.generateAndSendEmail(generateStatusForEmail());
        return currentStatus;
    }

    public void stopExecution(){
        running.set(false);
        isStopped = true;
        logger.info(name + " >>> "  + "Stopping the execution");
    }

    public String getName() {
        return name;
    }
}
