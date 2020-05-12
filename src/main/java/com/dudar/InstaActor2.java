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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Condition.disappears;
import static com.codeborne.selenide.Selenide.*;

public class InstaActor2 implements Runnable, Actor {

    public static final String START_TIME = "10:00:00";
    public static final String END_TIME = "23:00:00";
    //    private boolean detectMediaContant = false;
    private int crashCounter = 0;

    private AtomicBoolean running = new AtomicBoolean(false);

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
    private List<String> completedTags = new ArrayList<>();
    private String currentTag = "";
    private List<String> defectedTags = new ArrayList<>();
    private List<String> likedPosts = new ArrayList<>();
    private List<String> commentedPosts = new ArrayList<>();
    private String currentPostUrl = "";
    private PostType currentPostType = PostType.UNDEFINED;
    private boolean currentPostLikeAdded = false;
    private int totalLiked = 0;
    private int followedCount = 0;
    private String addedComment = "";
    private String currentStatus = "";
    private boolean interrupted = false;
    private int totalComments = 0;
    private InstaActorProperties actorProperties;

    private long stopPoint = 0;

    public InstaActor2(String name, Properties properties, List<String> tags){
        this.name = name;
        creationDate = new Date();
        allTags = tags;
    }

    private void initValuesFromProperties() {
        actorProperties = new InstaActorProperties(name);
    }

    private void sendEmailMessage(String message){
        if(actorProperties.isEmailServiceEnabled()){
            EmailService.generateAndSendEmail(message);
        }
    }

    private void sendEmailMessage(String message, String filePath){
        if(actorProperties.isEmailServiceEnabled()){
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
        if(!StringUtils.isEmpty(actorProperties.getProxyValue()))
            chromeOptions.addArguments("--proxy-server=" +  actorProperties.getProxyValue());

        if(!debug) {
            String seleniumHub = actorProperties.getHub();
            String seleniumHubPort = String.valueOf(actorProperties.getHubPort());
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
        enterloginAndPassword();
        if(suspectedActionsDetectorAfterLogin())
            logger.warn("Can't login!!!");
        if($$(By.xpath("//button[contains(.,'Send Security Code')]")).size() > 0){
            logger.error("Can't login to account");
            throw new InstaActorStopExecutionException(getNameForLog() + "LOGIN SECURITY CODE");
        }
    }

    private void enterloginAndPassword() {
        InstaActorElements.getUserLoginInput().val(actorProperties.getUserName()).pressTab();
        InstaActorElements.getUserPasswordInput().val(actorProperties.getUserPass()).pressEnter();
        $(By.xpath("//button[contains(.,'Log In')]")).waitUntil(disappears, 10000);
        sleep(3000);
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
        return ThreadLocalRandom.current().nextInt(actorProperties.getViewMinDelay(), actorProperties.getViewMaxDelay() + 1);
    }

    private int getVideoRandonTimeout(){
        return ThreadLocalRandom.current().nextInt(actorProperties.getViewMinDelayVideo(), actorProperties.getViewMaxDelayVideo() + 1);
    }

    private void mouseMoveToElementAndClick(WebElement element){
        Actions action = new Actions(WebDriverRunner.getWebDriver());
        action.moveToElement(element).perform();
        element.click();
        sleep(getRandomViewTimeout());
    }

    private boolean searchByTag(String searchTag) {
        try {
            SelenideElement searchBox = $(By.cssSelector("input[placeholder=\"Search\"]")).shouldBe(Condition.visible);
            mouseMoveToElementAndClick($(By.xpath("//span[contains(text(),'Search')]")));
            searchBox.val("#" + searchTag);
            $(By.xpath("//div[contains(@class,'SearchClear')]")).waitUntil(Condition.visible, 10000);

            if ($$(By.xpath("//a[attribute::href=\"/explore/tags/" + searchTag + "/\"]//span[contains(.,\"" + searchTag + "\")]")).size() > 0) {
                $$(By.xpath("//a[attribute::href=\"/explore/tags/" + searchTag + "/\"]//span[contains(.,\"" + searchTag + "\")]")).get(0).click();
            }
            sleep(5000);
            $(By.xpath("//div[contains(text(),'Top posts')]")).waitUntil(Condition.visible, 10000);
            SelenideElement tagLocator = null;
            if ($$(By.cssSelector("main h1")).size() > 0) {
                tagLocator = $$(By.cssSelector("main h1")).get(0);
            }
            if (tagLocator != null && tagLocator.getText().equalsIgnoreCase("#" + searchTag)) {
                logger.info(getNameForLog() + "Current page Tag - " + tagLocator.getText());
                currentTag = searchTag;
                return true;
            } else {
                System.out.println("!!! Can't find  search tag page. Search Tag - " + searchTag);
                defectedTags.add(searchTag);
                return false;
            }
        }
        catch (com.codeborne.selenide.ex.ElementNotFound err){
            logger.error(getNameForLog() + "Failed on search.");
            return false;
        }
    }

    private void detectPostTypeAndAct() {
        ElementsCollection imagePost = $$(By.xpath("//div[attribute::role='dialog']//article//img[attribute::style='object-fit: cover;']"));
        if(imagePost.size()>0){
            if(imagePost.size()==1){
                if(actorProperties.isDetectMediaContent()){
                    try {
                        String imageUrl = imagePost.get(0).getAttribute("srcset").split(" ")[0];
                        URL imageURL = new URL(imageUrl);
                        BufferedImage saveImage = ImageIO.read(imageURL);
                        String savedImagePath = "tmp/current_post_image.jpg";
                        ImageIO.write(saveImage, "jpg", new File(savedImagePath));

                        //TODO Image Recognition
                        if(actorProperties.isDetectMediaContent())
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
        boolean result = false;
        if(ThreadLocalRandom.current().nextInt(min, max) <= actorProperties.getLikesPercentage()) {
            if(System.currentTimeMillis() > stopPoint + actorProperties.getActionPauseDurationHours()*3600000)
                result = true;
        }
        return result;
    }

    private void addLikeToPost() {
        if(InstaActorElements.getPostLikeButton() != null){
            logger.info(getNameForLog() + "Like post");
            mouseMoveToElementAndClick(InstaActorElements.getPostLikeButton());
            if(suspectedActionsDetectorOnAction()){
                logger.info(getNameForLog() + "DISABLE LIKE AND COMMENTS ACTION!!!");
                disableLikeAction();
                disableCommentsAction();
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
        actorProperties.setLikesPercentage(90);
    }

    public void disableLikeAction(){
        logger.info(getNameForLog() + "DISABLE LIKE ACTION");
        actorProperties.setLikesPercentage(0);
    }

    public void enableCommentsAction(){
        logger.info(getNameForLog() + "ENABLE COMMENTS ACTION");
        actorProperties.setCommentsPercentage(90);
    }

    public void disableCommentsAction(){
        logger.info(getNameForLog() + "DISABLE COMMENTS ACTION");
        actorProperties.setCommentsPercentage(0);
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
                sleep(20000);
                stopPoint = System.currentTimeMillis();
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
        if(ThreadLocalRandom.current().nextInt(min, max) <= actorProperties.getCommentsPercentage()) {
            logger.info(getNameForLog() + "Add Comment");
            result = true;
        }
        return result;
    }

    private void addCommentToPost(){
        //TODO Post comment according to image type
        if(System.currentTimeMillis() > stopPoint + actorProperties.getActionPauseDurationHours()*3600000) {
            try {
                String commentText = InstaActorComments.generateComment(currentPostType);
                logger.debug(getNameForLog() + "Trying to add comment: " + commentText);
                $(By.cssSelector("article textarea")).val(commentText);
                mouseMoveToElementAndClick($(By.xpath("//button[attribute::type='submit']")));
                if (suspectedActionsDetectorOnAction()) {
                    logger.info(getNameForLog() + "DISABLE LIKE AND COMMENTS ACTION!!!");
                    disableCommentsAction();
                    resetCurrentPostStatus();
                } else {
                    logger.info(getNameForLog() + "Comment added: " + commentText);
                    totalComments++;
                    addedComment = commentText;
                    commentedPosts.add(currentPostUrl);
                }
            } catch (Error err) {
                logger.error(getNameForLog() + "ERROR on commenting" + err.getLocalizedMessage());
            }
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
        currentStatus += "|   Number is " + currentPostPosition + " from " + actorProperties.getMaxPostsCount() + ".\n";
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
                + "Like percentage - " + actorProperties.getLikesPercentage() + "\n"
                + "Comment percentage - " + actorProperties.getCommentsPercentage() +"\n"
                + "View parameters: " + actorProperties.getViewMinDelay()
                    + " " + actorProperties.getViewMaxDelay() + "\n"
                + "Video parameters: " + actorProperties.getViewMinDelayVideo()
                    + " " + actorProperties.getViewMaxDelayVideo() + "\n"
                + "*****InstaActor Parameters*****";
        logger.info(currentStatus);
        return currentStatus;
    }

    private int executionCounter = 0;

    private int randomPostsCountValue(){
        int values = (actorProperties.getMaxPostsCount() > 10) ? (actorProperties.getMaxPostsCount()-10) : 10;
        return ThreadLocalRandom.current().nextInt(values, actorProperties.getMaxPostsCount() + 10);
    }

    @Override
    public void run() {
        running.set(true);
        while (running.get()) {
            initValuesFromProperties();
            if(actorProperties.isNightMode()){
                try{
                    while(isNowTimeBetweenLimits(START_TIME, END_TIME))
                    {
                        logger.info(getNameForLog() + "Sleep time");
                        waitSomeTime(1*3600000);
                    }
                }
                catch (ParseException ex){
                    ;
                }
            }

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
            initTagsFromFile();
            if (isCompleted) {
                sleepAfterCompletion();
            }
            while (!isCompleted & !isStopped) {
                if (crashCounter > 10) {
                    stopExecution();
                    sendEmailMessage("CrashCounter exceed max value for - <b>" + name
                            + "</b><p>Stop execution.<p>"+generateStatusForEmail());
                    crashCounter = 0;
                    break;
                }
                try {
                    magicWorker();
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

    private void sleepAfterCompletion() {
        logger.info(getNameForLog() + "All tags were processed");
        String message = getNameForLog() + " execution completed.</br>";
        sendEmailMessage(message + generateStatusForEmail());
        if(actorProperties.isRepeatActionsAfterComplete()){
            executionCounter++;
            interrupted = false;
            isCompleted = false;
            crashCounter = 0;
            completedTags = new ArrayList<>();
            sleepMode = true;
            waitSomeTime(actorProperties.getSleepDurationBetweenRunsInHours()*3600000);
            sleepMode = false;
            startTime = LocalDateTime.now();
            endTime = null;
        }
        else {
            stopExecution();
        }
    }

    private boolean isNowTimeBetweenLimits(String startTime, String endTime) throws ParseException {
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

    private void magicWorker() throws InstaActorStopExecutionException {
        initDriver(actorProperties.isDebugMode());
        authenticate();
        checkIfPopupShown();
        Collections.shuffle(allTags);
        logger.debug(getNameForLog() + "Shuffled Tags: " + allTags);
        int tagsCollectionSize = allTags.size();
        AtomicInteger tagCounter = new AtomicInteger(1);
        int reactionsCounter = 0;
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
        followSuggestedAccounts();
        isCompleted = true;
        logger.info(getStatus());
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
            if(followButtons.get(0).is(Condition.visible)){
                logger.info(getNameForLog() + "follow account");
                try {
                    mouseMoveToElementAndClick(followButtons.get(0));
                    waitSomeTime(getRandomViewTimeout());
                    if(InstaActorElements.getActionBlockedDialog()!=null){
                        logger.info(getNameForLog() + "Action Blocked dialog");
                        $(By.xpath("//div[attribute::role='dialog']//button[contains(text(),\"Report a Problem\")]")).click();
                        waitSomeTime(getRandomViewTimeout());
                        return;
                    }
                    followedCount++;
                }
                catch (ElementClickInterceptedException ex){
                    logger.debug(getNameForLog() + "Can't click at follow button");
                }
            }
            waitSomeTime(getRandomViewTimeout());
        }
    }

    private void clearSession(){
        try{
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
                +"<p>Like percentage: " + actorProperties.getLikesPercentage() + "%"
                +"<p>Comments percentage: " + actorProperties.getCommentsPercentage() + "%"
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
        status += "<p>Follow accounts Total: " + followedCount;
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