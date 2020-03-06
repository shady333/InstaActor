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
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    private String name;
    private Thread t = null;
    private Date creationDate;
    private boolean sleepMode = false;

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
    private boolean debugMode = false;
    private List<String> completedTags = new ArrayList<>();
    private String currentTag = "";
    private List<String> defectedTags = new ArrayList<>();
    private List<String> likedPosts = new ArrayList<>();
    private List<String> commentedPosts = new ArrayList<>();
    private String currentPostUrl = "";
    private PostType currentPostType = PostType.UNDEFINED;
    private boolean currentPostLikeAdded = false;
    private int totalLiked = 0;
    private String proxyValue = "";

    private String addedComment = "";
    private String currentStatus = "";
    private boolean interrupted = false;
    private boolean emailServiceEnabled = false;
    private Properties actorProperties;

    private int sleepDurationBetweenRunsInHours = 2;

    private int totalComments = 0;

    private boolean repeatActionsAfterComplete = false;

    public InstaActor2(String name, Properties properties, List<String> tags){
        this.name = name;
        creationDate = new Date();
        allTags = tags;
        actorProperties = properties;
        initValuesFromProperties();
    }

    private void initValuesFromProperties() {


        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("data/" + name + "_user.properties"));
            actorProperties = prop;
        } catch (IOException e) {
            logger.error(getNameForLog() + "Can't reinit properties from file");
            e.printStackTrace();
        }

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
        if(!StringUtils.isEmpty(actorProperties.getProperty("debug.mode")))
            debugMode = Boolean.parseBoolean(actorProperties.getProperty("debug.mode"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("detect.media.content")))
            detectMediaContant = Boolean.parseBoolean(actorProperties.getProperty("detect.media.content"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("email.service")))
            emailServiceEnabled = Boolean.parseBoolean(actorProperties.getProperty("email.service"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("service.repeat")))
            repeatActionsAfterComplete = Boolean.parseBoolean(actorProperties.getProperty("service.repeat"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("sleep.duration")))
            sleepDurationBetweenRunsInHours = Integer.parseInt(actorProperties.getProperty("sleep.duration"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("proxy")))
            proxyValue = actorProperties.getProperty("proxy");
    }

    private void sendEmailMessage(String message){
        if(emailServiceEnabled){
            EmailService.generateAndSendEmail(message);
        }
    }

    private void sendEmailMessage(String message, String filePath){
        if(emailServiceEnabled){
            EmailService.generateAndSendEmail(message, filePath);
        }
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
    public boolean isActive(){
        return !sleepMode;
    }

    @Override
    public Thread.State getState(){
        return t.getState();
    }

    private void initDriver(boolean debug) {

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR,
                UnexpectedAlertBehaviour.IGNORE);
        chromeOptions.setHeadless(true);
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--enable-automation");
        if(!StringUtils.isEmpty(proxyValue))
            chromeOptions.addArguments("--proxy-server=" +  proxyValue);

        if(!debug) {
            String seleniumHub = System.getenv("HUB_HOST");
            String seleniumHubPort = System.getenv("HUB_PORT");
            if(Strings.isNullOrEmpty(seleniumHub) || Strings.isNullOrEmpty(seleniumHubPort)){
                seleniumHub = "localhost";
                seleniumHubPort = "4444";
            }
            String gridHubUrl = "http://" + seleniumHub + ":" + seleniumHubPort;
            if(!Utilities.checkGridStatus(gridHubUrl))
            {
                logger.error(getNameForLog() + "GRID not ready for execution. Stop service.");
                stop();
            }
            try {
                driver = new RemoteWebDriver(new URL(gridHubUrl+"/wd/hub"), chromeOptions);
            } catch (MalformedURLException e) {
                logger.error(getNameForLog() + "!!!Can't init DRIVER");
                logger.error(getNameForLog() + "Error message: " + e.getLocalizedMessage());
                driver = null;
            }
        }
        else {
            chromeOptions.setHeadless(false);
            driver = new ChromeDriver(chromeOptions);
        }
        WebDriverRunner.setWebDriver(driver);
    }

    private void authenticate() throws InstaActorStopExecutionException {
        open("https://www.instagram.com/accounts/login/?source=auth_switcher");
        sleep(getRandomViewTimeout());
        InstaActorElements.getUserLoginInput().val(userName).pressTab();
        InstaActorElements.getUserPasswordInput().val(userPass).pressEnter();
        sleep(10000);



        if(suspectedActionsDetectorAfterLogin())
            logger.warn("Can't login!!!");

        if($$(By.xpath("//button[contains(.,'Send Security Code')]")).size() > 0){
            logger.error("Can't login to account");
            throw new InstaActorStopExecutionException(getNameForLog() + "LOGIN SECURITY CODE");
        } else if ($$(By.xpath("//button[contains(.,'Log In')]")).size() > 0) {
            logger.error("Can't login to account");
            throw new InstaActorStopExecutionException(getNameForLog() + "LOGIN ERROR");
        }
    }

    private void waitTillPageLoadedAndSearchAvailable(){
        int retriesCounter = 0;
        while(true){
            ElementsCollection items = $$(By.cssSelector("input[placeholder=\"Search\"]"));
            if(items.size() > 0){
                items.get(0).shouldBe(Condition.visible);
                return;
            }
            if(retriesCounter > 10){
                logger.error("Search control is not in expected state");
                return;
            }
            retriesCounter++;
            waitSomeTime(1000);
        }
    }

    private void waitSomeTime(int duration){
        long currentPoint = System.currentTimeMillis();

        while(System.currentTimeMillis() < (currentPoint + duration)){
            logger.debug(getNameForLog() + "Sleep duration - " + duration/1000 + " seconds.\n"
                + ((currentPoint + duration) - System.currentTimeMillis())/1000 + " seconds left.");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkIfPopupShown() throws InstaActorStopExecutionException {
        checkCompromizedInfo();
        waitTillPageLoadedAndSearchAvailable();
        ElementsCollection popupWindow = $$(By.xpath("//div[attribute::role='dialog']"));
        if(popupWindow.size() > 0){
            String popupText = popupWindow.get(0).find("h2").getText();
            System.out.println("!!!Popup detected - " + popupText);
            if(popupText.equalsIgnoreCase("Turn on Notifications")){
                mouseMoveToElementAndClick($(By.xpath("//button[contains(text(), 'Not Now')]")));
            }
        }
    }

    private void checkCompromizedInfo() throws InstaActorStopExecutionException {
        if(InstaActorElements.getCompromisedAccountInfo()!=null){
            throw new InstaActorStopExecutionException(getNameForLog() + "COMPROMIZED");
        }
    }

    private int getRandomViewTimeout(){
        return ThreadLocalRandom.current().nextInt(viewMinDalay, viewMaxDelay + 1);
    }

    private int getVideoRandonTimeout(){
        return ThreadLocalRandom.current().nextInt(viewMinDelayVideo, viewMaxDelayVideo + 1);
    }

    private void mouseMoveToElementAndClick(WebElement element){
        //sleep(getRandomViewTimeout());
        Actions action = new Actions(WebDriverRunner.getWebDriver());
        action.moveToElement(element).perform();
        element.click();
        sleep(getRandomViewTimeout());
    }

    private boolean searchByTag(String searchTag) {
        SelenideElement searchBox = $(By.cssSelector("input[placeholder=\"Search\"]")).shouldBe(Condition.visible);
        mouseMoveToElementAndClick($(By.xpath("//span[contains(text(),'Search')]")));
        searchBox.val("#"+searchTag);
        $(By.xpath("//div[contains(@class,'SearchClear')]")).waitUntil(Condition.visible, 10000);

        if($$(By.xpath("//a[attribute::href=\"/explore/tags/"+searchTag+"/\"]//span[contains(.,\""+searchTag+"\")]")).size()>0){
            $$(By.xpath("//a[attribute::href=\"/explore/tags/"+searchTag+"/\"]//span[contains(.,\""+searchTag+"\")]")).get(0).click();
        }
        sleep(5000);
        $(By.xpath("//div[contains(text(),'Top posts')]")).waitUntil(Condition.visible, 10000);
        SelenideElement tagLocator = null;
        if($$(By.cssSelector("main h1")).size() > 0){
            tagLocator = $$(By.cssSelector("main h1")).get(0);
        }
        if(tagLocator != null && tagLocator.getText().equalsIgnoreCase("#"+searchTag)){
            logger.info(getNameForLog()  + "Current page Tag - "+tagLocator.getText());
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
                if(detectMediaContant){
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
                        logger.error(getNameForLog() + e.getLocalizedMessage());
                    } catch (IOException e) {
                        logger.error("Can't detect post type. Can't load image.");
                    }
                }
                sleep(getRandomViewTimeout());
                currentPostType = PostType.PHOTO;
                return;
            }
            else
            {
                for(int i = 1; i < imagePost.size(); i++){
                    if($$(By.cssSelector(".coreSpriteRightChevron")).size()>0) {
                        logger.info(getNameForLog() + "Navigate to next image > " + i);
                        mouseMoveToElementAndClick($(By.cssSelector(".coreSpriteRightChevron")).waitUntil(Condition.visible, 5000));
                    }
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

    private boolean shouldLikePost(){
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
        if(InstaActorElements.getPostLikeButton() != null){
            logger.info(getNameForLog() + "Like post");
            mouseMoveToElementAndClick(InstaActorElements.getPostLikeButton());
            if(suspectedActionsDetectorOnAction()){
                logger.info(getNameForLog() + "DISABLE LIKE AND COMMENTS ACTION!!!");
                likesPercentage = 0;
                commentsPercentage = 0;
                resetCurrentPostStatus();
            }
            else{
                currentPostLikeAdded = true;
                logger.info(getNameForLog() + "Like added");
                totalLiked++;
                likedPosts.add(currentPostUrl);
            }
        }
    }

    private void resetCurrentPostStatus() {
        currentPostLikeAdded = false;
        addedComment = "";
        currentPostType = PostType.UNDEFINED;
        currentStatus = "";
    }

    public void enableLikeAction(){
        logger.info(getNameForLog() + "ENABLE LIKE ACTION");
        likesPercentage = 90;
    }

    public void disableLikeAction(){
        logger.info(getNameForLog() + "DISABLE LIKE ACTION");
        likesPercentage = 0;
    }

    public void enableCommentsAction(){
        logger.info(getNameForLog() + "ENABLE COMMENTS ACTION");
        commentsPercentage = 90;
    }

    public void disableCommentsAction(){
        logger.info(getNameForLog() + "DISABLE COMMENTS ACTION");
        commentsPercentage = 0;
    }

    private boolean suspectedActionsDetectorOnAction() {
        ElementsCollection buttonReport = $$(By.xpath("//button[contains(text(),'Report a Problem')]"));
        if(buttonReport.size() > 0){
                logger.warn(getNameForLog() + "!!!WARNING!!!");
                sendEmailMessage(getNameForLog() + "Like or Comment action was blocked by Instagram service<br/>"
                        + "<b>LIKE and COMMENT option will be disabled for current instance: " + name + "<br/>"
                        +"<b>Tag name:</b> " + currentTag + "<br/>"
                        +"<b>Post Url:</b> " + currentPostUrl + "<br/>", screenshot("tmp/crash/chash_info.png"));
                buttonReport.get(0).click();
                sleep(60000);
                return true;
        }
        return false;
    }

    private boolean suspectedActionsDetectorAfterLogin() {
        ElementsCollection caption = $$(By.xpath("//h3[contains(.,'Your Account Was Compromised')]"));
        ElementsCollection buttonReport = $$(By.xpath("//button[contains(text(),'Report a Problem')]"));
        if((caption.size() > 0) && (buttonReport.size() > 0)){
            logger.warn(getNameForLog() + "Can't login to account!!!");
            sendEmailMessage(getNameForLog() + "Can't login to account<br/>"
                    + "<b>You have to change pasword and re-init current Actor: " + name + "<br/>"
                    , screenshot("tmp/crash/chash_info.png"));
            buttonReport.get(0).click();
            logger.info(getNameForLog() + "Stop Actor.");
            stop();
            return true;
        }
        return false;
    }

    private boolean suspectedActionsDetectorOnComment() {
        ElementsCollection buttonReport = $$(By.xpath("//button[contains(text(),'Report a Problem')]"));
        if(buttonReport.size() > 0){
                logger.warn(getNameForLog() + "!!!WARNING!!!");
                logger.warn(getNameForLog() + "SKIP CURRENT TAG Liking\nBREAK!!!!");
                System.out.println("!!!STOP EXECUTION");
                sendEmailMessage(getNameForLog() + "Comment action was blocked by Instagram service<br/>"
                        + "<b>COMMENT option will be disabled for current instance: " + name + "<br/>"
                        +"<b>Tag name:</b> " + currentTag + "<br/>"
                        +"<b>Post Url:</b> " + currentPostUrl + "<br/>", screenshot("tmp/crash/chash_info.png"));
                buttonReport.get(0).click();
                return true;
        }
        return false;
    }

    private boolean shouldCommentPost(){
        int min = 1;
        int max = 100;
        boolean result = false;
        if(ThreadLocalRandom.current().nextInt(min, max) <= commentsPercentage) {
            logger.info(getNameForLog() + "Add Comment");
            result = true;
        }
        return result;
    }

    private void addCommentToPost(){
        //TODO Post comment according to image type
        try {
            String commentText = InstaActorComments.generateComment(currentPostType);
            logger.debug(getNameForLog() + "Trying to add comment: " + commentText);
            $(By.cssSelector("article textarea")).val(commentText);
            mouseMoveToElementAndClick($(By.xpath("//button[attribute::type='submit']")));
            if(suspectedActionsDetectorOnAction()) {
                logger.info(getNameForLog() + "DISABLE LIKE AND COMMENTS ACTION!!!");
                likesPercentage = 0;
                commentsPercentage = 0;
                resetCurrentPostStatus();
            }
            else{
                logger.info(getNameForLog() + "Comment added: " + commentText);
                totalComments++;
                addedComment = commentText;
            }
            commentedPosts.add(currentPostUrl);
        } catch (Error err) {
            logger.error(getNameForLog() + "ERROR on commenting" + err.getLocalizedMessage());
        }
    }

    private void  interactWithPosts(int maxPostsCount) throws InstaActorStopExecutionException {
        String rootElement = "//div[contains(text(), 'Top posts')]/../..";
        $(By.xpath(rootElement)).shouldBe(Condition.enabled).scrollIntoView(true);
        sleep(getRandomViewTimeout());
        WebElement firstPostToLike = $(By.xpath(rootElement+"//a")).shouldBe(Condition.enabled);
        mouseMoveToElementAndClick(firstPostToLike);
        for(int i = 1; i <= maxPostsCount; i++){
            if(!running.get()){
                throw new InstaActorStopExecutionException();
            }
            currentPostUrl = WebDriverRunner.url();
            InstaActorElements.getPostCloseButton().shouldBe(Condition.visible).shouldBe(Condition.enabled);
            if(InstaActorElements.getPostLikeButton()!=null){

                if(processedPosts.get(currentTag).contains(currentPostUrl)){
                    logger.info(getNameForLog() + "Post was already processed");
                    logger.info(getNameForLog() + "SKIP - " + currentPostUrl);
                    continue;
                }
                sleep(getRandomViewTimeout());
                detectPostTypeAndAct();
                if(shouldLikePost()) {
                    if (!likedPosts.contains(currentPostUrl)) {
                        addLikeToPost();
                    }
                }
                if(shouldCommentPost()){
                    if(!commentedPosts.contains(currentPostUrl)) {
                        addCommentToPost();
                    }
                }
            }
            logger.info(getNameForLog() + "Current post info:\n" + getCurrentStatusString(i));
            ArrayList<String> items = processedPosts.get(currentTag);
            items.add(currentPostUrl);
            processedPosts.put(currentTag, items);
            resetCurrentPostStatus();
            if (!moveToNextPostIfAvailable())
                break;
        }
    }

    private boolean moveToNextPostIfAvailable() {
        ElementsCollection nextButtonElements = $$(By.xpath("//a[contains(text(), 'Next')]"));
        if(nextButtonElements.size() > 0){
            nextButtonElements.get(0).shouldBe(Condition.visible);
            mouseMoveToElementAndClick(nextButtonElements.get(0));
        }
        else{
            logger.info("No more next elements");
            return false;
        }
        return true;
    }

    private String getCurrentStatusString(int currentPostPosition) {
        currentStatus += "\n/***************InstaActor " + name + " POST INFO*****************/\n";
        currentStatus += "|\n";
        currentStatus += "|   Tag: " + currentTag + ".\n";
        currentStatus += "|   Tags count: " + allTags.size() + ".\n";
        currentStatus += "|   Completed Tags: " + completedTags.size() + ".\n";
        currentStatus += "|   Defected Tags: " + defectedTags.size() + ".\n";
        currentStatus += "|   Number is " + currentPostPosition + " from " + maxPostsCount + ".\n";
        currentStatus += "|   Url: " + currentPostUrl + ".\n";
        currentStatus += "|   Type: " + currentPostType.toString() + ".\n";
        currentStatus += "|   Like: " + currentPostLikeAdded + ".\n";
        if(!Strings.isNullOrEmpty(addedComment)){
            currentStatus += "|   Added comment: " + addedComment + ".";
        }
        currentStatus += "|\n";
        return currentStatus;
    }

    public String viewCurrentParameters(){
        String currentStatus = "*****InstaActor Parameters*****\n"
                + "Name - " + name + "\n"
                + "Active - " + isActive() + "\n"
                + "Like percentage - " + likesPercentage + "\n"
                + "Comment percentage - " + commentsPercentage +"\n"
                + "View parameters: " + viewMinDalay + " " + viewMaxDelay + "\n"
                + "Video parameters: " + viewMinDelayVideo + " " + viewMaxDelayVideo + "\n"
                + "*****InstaActor Parameters*****";
        logger.info(currentStatus);
        return currentStatus;
    }

    private int executionCounter = 0;

    private int randomPostsCountValue(){
        int values = (maxPostsCount > 10) ? (maxPostsCount-10) : 10;
        return ThreadLocalRandom.current().nextInt(values, maxPostsCount + 10);
    }

    @Override
    public void run() {
        running.set(true);
        while (running.get()) {
            sendEmailMessage(viewCurrentParameters().replaceAll("\n", "<br/>"));
            interrupted = false;
            commentedPosts.addAll(Utilities.getAllTags(getCommentedPostsFilePath()));
            commentedPosts = commentedPosts.stream()
                    .distinct()
                    .collect(Collectors.toList());
            likedPosts.addAll(Utilities.getAllTags(getLikedPostsFilePath()));
            likedPosts = likedPosts.stream()
                    .distinct()
                    .collect(Collectors.toList());
            initValuesFromProperties();
            initTagsFromFile();
            if (isCompleted) {
                logger.info(getNameForLog() + "All tags were processed");
                String message = getNameForLog() + " execution completed.</br>";
                sendEmailMessage(message + generateStatusForEmail());
                if(repeatActionsAfterComplete){
                    executionCounter++;
                    interrupted = false;
                    isCompleted = false;
                    crashCounter = 0;
                    completedTags = new ArrayList<>();
                    sleepMode = true;
                    waitSomeTime(sleepDurationBetweenRunsInHours*3600000);
                    sleepMode = false;
                    startTime = LocalDateTime.now();
                    endTime = null;
                }
                else {
                    stopExecution();
                }
            }
            while (!isCompleted & !isStopped) {
                if (crashCounter > 10) {
                    stopExecution();
                    //clearSession();
                    sendEmailMessage("CrashCounter exceed max value for - <b>" + name
                            + "</b><p>Stop execution.<p>"+generateStatusForEmail());
                    crashCounter = 0;
                    break;
                }
                try {
                    initDriver(debugMode);
                    authenticate();
                    checkIfPopupShown();
                    Collections.shuffle(allTags);
                    logger.debug(getNameForLog() + "Shuffled Tags: " + allTags);
                    int tagsCollectionSize = allTags.size();
                    AtomicInteger tagCounter = new AtomicInteger(1);
                    int reactionsCounter = 0;

                    followSuggestedAccounts();

                    for (String searchTag : allTags) {
                        processedPosts.put(searchTag, new ArrayList());
                        if(reactionsCounter > ThreadLocalRandom.current().nextInt(3, 10)) {
                            followAccountFromYourFeed();
                            reactionsCounter = 0;
                        }
                        reactionsCounter++;
                        if (!completedTags.contains(searchTag)) {
                            completedTags.add(searchTag);
                            logger.info(getNameForLog() + "Current tag is " + tagCounter + " from " + tagsCollectionSize + " all of Tags");
                            tagCounter.getAndIncrement();
                            if (searchByTag(searchTag)) {
                                interactWithPosts(randomPostsCountValue());
                                if (!interrupted) {
                                    WebElement closeButton = InstaActorElements.getPostCloseButton().shouldBe(Condition.visible);
                                    mouseMoveToElementAndClick(closeButton);
                                } else {
                                    break;
                                }
                            }
                            else{
                                logger.info("Can't find postst for tag - " + searchTag);
                                defectedTags.add(searchTag);
                            }
                        }
                    }
                    isCompleted = true;
                    logger.info(getStatus());
                }
                catch (AssertionError err){
                    logger.info("Selenide error: " + err.getMessage());
                    sendEmailMessage(getNameForLog() + "SELENIDE Assert Error: " + err.getMessage(), screenshot("tmp/crash/assert_error_info.png"));
                }
                catch (InstaActorStopExecutionException ex) {
                    String message = getNameForLog() + "Execution stopped!!!";
                    logger.info(message);
                    message += "<p>Error details: <p>"+ex.getMessage();
                    sendEmailMessage(message + "<p>" + generateStatusForEmail(),screenshot("tmp/crash/stop_execution.png"));
                    stopExecution();
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                    if(ex.getMessage().contains("DevToolsActivePort file doesn't exist"))
                    {
                        logger.error("Chrome driver error\n" + ex.getMessage());
                    } else if(ex.getMessage().contains("Timed out waiting for driver server to start.")){
                        logger.error("Chrome driver error\n" + ex.getMessage());
                    }
                    else {
                        sendEmailMessage("<p> Service <b>" + name + "</b> crashed with exception:<p>"
                                + ex.getMessage(), screenshot("tmp/crash/crash_exception_info.png"));
                        crashCounter++;
                    }
                } finally {
                    clearSession();
                }
            }

            endTime = LocalDateTime.now();

        }
    }

    private void initTagsFromFile() {
        logger.info(getNameForLog() + "Init tags");
        allTags = Utilities.getAllTags("data/" + name + "_tags.csv");
    }

    @NotNull
    private String getDefectedTagsFilePath() {
        return "data/results/"+name+"_defectedTags.csv";
    }

    @NotNull
    private String getLikedPostsFilePath() {
        return "data/results/"+name+"_likedPosts.csv";
    }

    @NotNull
    private String getCommentedPostsFilePath() {
        return "data/results/"+name+"_commentedPosts.csv";
    }

    private void writeListToFile(List listName, String fileName){

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

    private void followAccountFromYourFeed(){
        open("https://www.instagram.com/accounts/activity/");
        followAccounts();
    }

    private void followSuggestedAccounts(){
        open("https://www.instagram.com/explore/people/suggested/");
        $(By.xpath("//h4[text()='Suggested']")).waitUntil(Condition.visible, 10000);
        followAccounts();
    }

    private void followAccounts() {
        logger.info(getNameForLog() + "Review and follow accounts");
        waitSomeTime(getRandomViewTimeout());
        ElementsCollection followButtons = $$(By.xpath("//button[text()='Follow']"));
        int maxItems = (followButtons.size()>5)?5:followButtons.size();
        for(int i=0; i<maxItems; i++){
            waitSomeTime(getRandomViewTimeout());
            if(followButtons.get(0).is(Condition.visible)){
                logger.info(getNameForLog() + "follow account");
                //try{
                //    followButtons.get(0).click();
                //}
                //catch(AssertionError err){
                    executeJavaScript("arguments[0].click()", followButtons.get(0));
                //}
                waitSomeTime(getRandomViewTimeout());
                if(InstaActorElements.getActionBlockedDialog()!=null){
                    logger.info(getNameForLog() + "Action Blocked dialog");
                    $(By.xpath("//div[attribute::role='dialog']//button[contains(text(),\"Report a Problem\")]")).click();
                    waitSomeTime(getRandomViewTimeout());
                    return;
                }
            }
            waitSomeTime(getRandomViewTimeout());
        }
    }

    private void clearSession(){
        try{
            //isCompleted = false;
            writeListToFile(defectedTags, getDefectedTagsFilePath());
            writeListToFile(likedPosts, getLikedPostsFilePath());
            writeListToFile(commentedPosts, getCommentedPostsFilePath());

            sleep(30000);

            logger.error(getNameForLog() + "Clear WebDriver session");
            closeWebDriver();
        }
        catch (IllegalStateException ex){
            logger.error(getNameForLog() + ex.getMessage());
        }
        catch (Exception ex){
            logger.error(getNameForLog() + ex.getMessage());
        }
        finally {
            driver = null;
        }
    }

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Actor start () {
        logger.info(getNameForLog() + "Starting...");
        isStopped = false;
        isCompleted = false;
        crashCounter = 0;
        startTime = LocalDateTime.now();
        endTime = null;
        if (t == null) {
            t = new Thread (this, name);
            startTime = LocalDateTime.now();
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
        logger.info(getNameForLog() + "STOP");
        endTime = LocalDateTime.now();
        clearSession();
        stopExecution();
        endTime = LocalDateTime.now();
        return this;
    }

    private long getExecutionDuration(){
        return ChronoUnit.MINUTES.between(startTime, (endTime == null) ? LocalDateTime.now() : endTime);
    }

    private String generateStatusForEmail(){
        String status = "<h1>InstaActor STATUS</h1>"
                +"<p>Service name: " + name
                +"<p>Service is running: " + isActive()
                +"<p>Started: " + creationDate
                +"<p>Current run start time: " + startTime
                +"<p>Current(latest) duration (minutes): " + getExecutionDuration()
                +"<p>Completed executions: " + executionCounter
                +"<p>Was interrupted: " + interrupted
                +"<p>Like percentage: " + likesPercentage + "%"
                +"<p>Comments percentage: " + commentsPercentage + "%"
                +"<p>Completed Tags: " + completedTags.size()
                +"<p>Defected Tags: " + defectedTags.size()
                +"<p>Tag: " + currentTag + " from " + allTags.size()
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
        logger.info(getNameForLog() + "Current status:\n" + currentStatus);
        //sendEmailMessage(generateStatusForEmail());
        return currentStatus;
    }

    public void  stopExecution(){
        running.set(false);
        isStopped = true;
        isCompleted = false;
        logger.info(getNameForLog() + "Stopping the execution");
        clearSession();
    }

    public String getNameForLog() {
        return name + " >>> ";
    }
}