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
    private boolean emailServiceEnabled = false;

    private int totalComments = 0;

    private boolean repeatActionsAfterComplete = false;

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
        if(!StringUtils.isEmpty(actorProperties.getProperty("email.service")))
            emailServiceEnabled = Boolean.parseBoolean(actorProperties.getProperty("email.service"));
        if(!StringUtils.isEmpty(actorProperties.getProperty("service.repeat")))
            repeatActionsAfterComplete = Boolean.parseBoolean(actorProperties.getProperty("service.repeat"));
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
                chromeOptions.addArguments("--no-sandbox");
                chromeOptions.addArguments("--enable-automation");
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
        sleep(getRandonViewTimeout());
        InstaActorElements.getUserLoginInput().val(userName).pressTab();
        InstaActorElements.getUserPasswordInput().val(userPass).pressEnter();
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
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void interactWithTurnOnNotificationsDialog() throws InstaActorStopExecutionException {
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
            throw new InstaActorStopExecutionException("Compromised");
        }
    }

    private int getRandonViewTimeout(){
        return ThreadLocalRandom.current().nextInt(viewMinDalay, viewMaxDelay + 1);
    }

    private int getVideoRandonTimeout(){
        return ThreadLocalRandom.current().nextInt(viewMinDelayVideo, viewMaxDelayVideo + 1);
    }

    private void mouseMoveToElementAndClick(WebElement element){
        sleep(getRandonViewTimeout());
        Actions action = new Actions(WebDriverRunner.getWebDriver());
        action.moveToElement(element).perform();
        element.click();
        sleep(getRandonViewTimeout());
    }

    private boolean searchByTag(String searchTag) {
        sleep(1000);
        SelenideElement searchBox = $(By.cssSelector("input[placeholder=\"Search\"]")).shouldBe(Condition.visible);
        mouseMoveToElementAndClick($(By.xpath("//span[contains(text(),'Search')]")));
        searchBox.val("#"+searchTag);
        sleep(3000);
        $(By.xpath("//div[contains(@class,'SearchClear')]")).waitUntil(Condition.visible, 10000);
        searchBox.sendKeys(Keys.DOWN, Keys.ENTER);
        sleep(5000);
        $(By.cssSelector("svg[aria-label=\"Instagram\"]")).shouldBe(Condition.visible);
        SelenideElement tagLocator = $(By.cssSelector("main h1")).shouldBe(Condition.exist);

        logger.info(getName()  + "Current page Tag - "+tagLocator.getText());
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
                    logger.info(getName() + "Navigate to next image > " + i);
                    mouseMoveToElementAndClick($(By.cssSelector(".coreSpriteRightChevron")).shouldBe(Condition.visible));
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
        if(InstaActorElements.getPostLikeButton() != null){
            logger.info(getName() + "Like post");
            mouseMoveToElementAndClick(InstaActorElements.getPostLikeButton());
            currentPostLikeAdded = true;
            totalLiked++;
        }
        else{
            logger.info(getName() + "Post already Liked.");
        }
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
                logger.warn(getName() + "!!!WARNING!!!");
                logger.warn(getName() + "SKIP CURRENT TAG Liking\nBREAK!!!!");
                System.out.println("Completed tags:");
                completedTags.forEach(System.out::println);
                System.out.println("!!!STOP EXECUTION");
                sendEmailMessage("Unexpected behavior - Stop Action!!! for:<br/>"
                        +"<b>Tag name:</b> " + currentTag + "<br/>"
                        +"<b>Post Url:</b> " + currentPostUrl + "<br/>", screenshot("tmp/crash/chash_info.png"));

                buttonReport.get(0).click();
                stopExecution();
                resetCurrentPostStatus();
                interrupted = true;
                return true;
            }
            logger.warn(getName() + "!!!WARNING!!!");
            logger.warn(getName() + "Detected suspicious action detected by service");
            buttonReport.get(0).click();
            sleep(getRandonViewTimeout());
            if(InstaActorElements.getPostLikeButton()!=null) {
                System.out.println("Re Like current post");
                if(likesEnabled)
                    mouseMoveToElementAndClick(InstaActorElements.getPostLikeButton());
                sleep(getRandonViewTimeout());
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
            logger.info(getName() + "Add Comment");
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
                logger.info(getName() + "Return Video comment");
                return InstaActorComments.commentsVideo.get(ThreadLocalRandom.current().nextInt(0, InstaActorComments.commentsVideo.size()));
            }
            logger.info(getName() + "Return Other Content comment");
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
        //TODO Post comment according to image type
        if(commentPost())
        {
            try {
                String commentText = getComment();
                logger.debug(getName() + "Trying to add comment: " + commentText);
                $(By.cssSelector("article textarea")).val(commentText);

                //TODO add emojji support
                //Commented part for posting emojji, not working yet
//                String JS_ADD_TEXT_TO_INPUT = "var elm = arguments[0], txt = arguments[1]; elm.value += txt; elm.dispatchEvent(new Event('change'));";
//                WebElement textBox = $(By.cssSelector("article textarea"));
//                executeJavaScript(JS_ADD_TEXT_TO_INPUT, textBox, commentText);

                mouseMoveToElementAndClick($(By.xpath("//button[attribute::type='submit']")));
                logger.info(getName() + "Comment added: " + commentText);
                totalComments++;
                addedComment = commentText;
            } catch (Error err) {
                logger.error(getName() + "ERROR on commenting" + err.getLocalizedMessage());
            }
        }
        else{
            logger.info(getName() + "Skip comment");
        }
    }

    private void  interactWithPosts(int maxPostsCount) throws InstaActorStopExecutionException {
        String rootElement = "//div[contains(text(), 'Top posts')]/../..";
        $(By.xpath(rootElement)).shouldBe(Condition.enabled).scrollIntoView(true);
        sleep(getRandonViewTimeout());
        WebElement firstPostToLike = $(By.xpath(rootElement+"//a")).shouldBe(Condition.enabled);
        mouseMoveToElementAndClick(firstPostToLike);

        for(int i = 1; i <= maxPostsCount; i++){

            if(!running.get()){
                throw new InstaActorStopExecutionException();
            }

            currentPostUrl = WebDriverRunner.url();
            InstaActorElements.getPostCloseButton().shouldBe(Condition.visible).shouldBe(Condition.enabled);
            if(InstaActorElements.getPostLikeButton()!=null){
                sleep(getRandonViewTimeout());
                if(processedPosts.get(currentTag).contains(currentPostUrl)){
                    logger.info(getName() + "Post was already processed");
                    logger.info(getName() + "SKIP - " + currentPostUrl);
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
                        logger.info(getName() + "!!!Likes option is disabled");
                    }
                    if(commentsEnabled) {
                        addCommentToPost();
                        if (suspectedActionsDetector())
                            return;
                    }
                }
            }
            logger.info(getName() + "Current post info:\n" + getCurrentStatusString(i));
            ArrayList<String> items = processedPosts.get(currentTag);
            items.add(currentPostUrl);
            processedPosts.put(currentTag, items);
            resetCurrentPostStatus();

            if (!moveToNextPostIfAvailable())
                break;
            sleep(getRandonViewTimeout());
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
            sendEmailMessage(viewCurrentParameters().replaceAll("\n", "<br/>"));
            interrupted = false;
            isStopped = false;
            if (isCompleted) {
                logger.info("All tags were processed");
                String message = getName() + " execution completed.</br>";
                sendEmailMessage(message + generateStatusForEmail());
                if(repeatActionsAfterComplete){
                    interrupted = false;
                    isStopped = false;
                    isCompleted = false;
                    crashCounter = 0;
                    defectedTags = new ArrayList<>();
                    completedTags = new ArrayList<>();
                    waitSomeTime(3600000);
                }
                else {
                    stopExecution();
                }
            }
            while (!isCompleted & !isStopped) {
                if (crashCounter > 10) {
                    stopExecution();
                    sendEmailMessage("CrashCounter exceed max value for - <b>" + name
                            + "</b><p>Stop execution.<p>"+generateStatusForEmail());
                    break;
                }
                try {
                    initDriver(debugMode);
                    isActive = true;
                    authentificate();
                    interactWithTurnOnNotificationsDialog();
                    Collections.shuffle(allTags);
                    int tagsCollectionSize = allTags.size();
                    AtomicInteger tagCounter = new AtomicInteger(1);
                    int reactionsCounter = 0;
                    for (String searchTag : allTags) {
                        processedPosts.put(searchTag, new ArrayList());
                        if(reactionsCounter == 3) {
                            analyseAndActoToReactions();
                            reactionsCounter = 0;
                        }
                        reactionsCounter++;
                        if (!completedTags.contains(searchTag)) {
                            completedTags.add(searchTag);
                            logger.info(getName() + "Current tag is " + tagCounter + " from " + tagsCollectionSize + " all of Tags");
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
                    sendEmailMessage(getName() + "SELENIDE Assert Error: " + err.getMessage(), screenshot("tmp/crash/assert_error_info.png"));
                    //crashCounter++;
                }
                catch (InstaActorStopExecutionException ex) {
                    running.set(false);
                    isStopped = true;
                    String message = getName() + "Execution stopped!!!";
                    logger.info(message);
                    message += "<p>Error details: <p>"+ex.getMessage();
                    sendEmailMessage(message + "<p>" + generateStatusForEmail(),screenshot("tmp/crash/stop_execution.png"));
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
                        isActive = false;
                        crashCounter++;
                    }
                } finally {
                    clearSession();
                }
            }
        }
    }

    private void analyseAndActoToReactions() {
        followAccounts();
    }

    private void followAccounts() {
        open("https://www.instagram.com/accounts/activity/");
        waitSomeTime(getRandonViewTimeout());
        ElementsCollection followButtons = $$(By.xpath("//button[text()='Follow']"));
        int maxItems = (followButtons.size()>5)?5:followButtons.size();
        for(int i=0; i<maxItems; i++){
            waitSomeTime(getRandonViewTimeout());
            logger.info(getName() + "follow account");
            followButtons.get(i).click();
            waitSomeTime(getRandonViewTimeout());
            if(InstaActorElements.getActionBlockedDialog()!=null){
                logger.info(getName() + "Action Blocked dialog");
                $(By.xpath("//div[attribute::role='dialog']//button[contains(text(),\"Report a Problem\")]")).click();
                return;
            }

            waitSomeTime(getRandonViewTimeout());
        }
    }

    private void clearSession(){
        try{
            logger.error(getName() + "Clear WebDriver session");
            closeWebDriver();
        }
        catch (IllegalStateException ex){
            logger.error(getName() + ex.getMessage());
        }
        catch (Exception ex){
            logger.error(getName() + ex.getMessage());
        }
        finally {
            driver = null;
        }
    }

    public Actor start () {
        logger.info(getName() + "Starting...");
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
        logger.info(getName() + "STOP");
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
                +"<p>Defected Tags: " + defectedTags.size()
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
        logger.info(getName() + "Current status:\n" + currentStatus);
        sendEmailMessage(generateStatusForEmail());
        return currentStatus;
    }

    public void  stopExecution(){
        running.set(false);
        isStopped = true;
        logger.info(getName() + "Stopping the execution");
    }

    public String getName() {
        return name + " >>> ";
    }
}
