package com.dudar;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.dudar.insta.*;
import com.dudar.utils.Utilities;
import com.dudar.utils.services.EmailService;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.stream.Collectors;

import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.codeborne.selenide.Condition.disappears;
import static com.codeborne.selenide.Selenide.*;

public class ActorInsta implements IActor {

    private final Date creationDate;
    private LocalDateTime startTime;
    private String name;
    private boolean wasInterrupted;
    private int executionCounter;

    private AtomicBoolean isEnabled = new AtomicBoolean(false);
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    final static Logger logger = Logger.getLogger(ActorInsta.class);
    private InstaActorProperties prop;
    private static RemoteWebDriver driver;
    private List<String> allTags;
    private int crashesCount = 0;
    private int followedCount = 0;
    private List<String> completedTags = new ArrayList<>();
    private List<String> defectedTags = new ArrayList<>();
    private List<String> likedPosts = new ArrayList<>();
    private List<String> commentedPosts = new ArrayList<>();
    private Map<String, ArrayList> processedPosts = new HashMap<>();
    private String currentTag = "";
    private boolean currentPostLikeAdded = false;
    private int totalLiked = 0;
    private int totalComments = 0;
    private String addedComment = "";

    private boolean isCompleted = false;
    private String DAY_START_TIME = "10:00:00";
    private String DAY_END_TIME = "23:00:00";
    private String currentPostUrl = "";
    private LocalDateTime endTime;
    private PostType currentPostType = PostType.UNDEFINED;

//    private Emailer emailer;

    public ActorInsta(String name){
        this.name = name;
        crashesCount = 0;
        followedCount = 0;
        executionCounter = 0;
        creationDate = new Date();
        //emailer = new Emailer();
        isEnabled.set(true);
    }

    @Override
    public boolean isActive(){
        return isRunning.get();
    }

    @Override
    public void activate() {
        logger.info(getNameForLog() + "Activating");
        shouldRun(true);
    }

    @Override
    public String getActorStatusInfo() {
        return getName()
                + "\nActivated : " + isEnabled() + ";\n"
                + "Is Running now: " + isActive() + "\n"
                + "Completed runs: " + executionCounter + "\n"
                + "Latest run duration (min): " + getExecutionDuration() + "\n\n";
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj == null || obj.getClass() != getClass()) {
            result = false;
        } else {
            ActorInsta actor = (ActorInsta) obj;
            if(this.name == actor.getName())
                result = true;
        }
        return result;
    }

    private void initProperties(){
        this.prop = new InstaActorProperties(this.name);
    }

    private void initPropertiesAndSetInitVariables() {
        initProperties();

        commentedPosts.addAll(Utilities.getAllTags(getCommentedPostsFilePath()));
        commentedPosts = commentedPosts.stream()
                .distinct()
                .collect(Collectors.toList());
        likedPosts.addAll(Utilities.getAllTags(getLikedPostsFilePath()));
        likedPosts = likedPosts.stream()
                .distinct()
                .collect(Collectors.toList());
//        defectedTags.addAll(Utilities.getAllTags(getDefectedTagsFilePath()));
//        defectedTags = defectedTags.stream()
//                .distinct()
//                .collect(Collectors.toList());
        allTags = Utilities.getAllTags("data/" + name + "_tags.csv");
        allTags = allTags.stream()
                .distinct()
                .collect(Collectors.toList());
//        allTags.removeAll(defectedTags);

        sleep(5000);
    }

    @Override
    public String getStatus() {
        return String.valueOf(isEnabled.get());
    }

    @Override
    public void deactivate() {
        isEnabled.set(false);
        resetCurrentPostStatus();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled.get();
    }

    public void shouldRun(boolean value){
        isEnabled.set(value);
    }

    public void start() {
        startTime = LocalDateTime.now();
        wasInterrupted = false;
        isCompleted = false;
        completedTags = new ArrayList<>();
        defectedTags = new ArrayList<>();
        isRunning.set(true);
        while (isEnabled.get() && Utilities.isInternetConnection()) {
            logger.info(getNameForLog() + "Running: " + this.name);
            initPropertiesAndSetInitVariables();
            try {
                verifyPreConditions();
                initSession();
                mainActivities();
                completeRun();
            }
            catch (AssertionError err){
                logger.error(getNameForLog() + " ASSERTION ERROR \n"
                    + err.getMessage());
                //emailer.generateAndSendEmail(getNameForLog() + err.getMessage(), screenshot("tmp/crash/assertion_error.png"));
                crashesCount++;
            }
            catch (InstaActorBreakExecutionException executionException){
                logger.error(getNameForLog() + " BREAK EXECUTION \n" + executionException.getMessage());
                completeRun();
            }
            catch (InstaActorStopExecutionException executionException){
                logger.error(getNameForLog() + " STOP EXECUTION \n" + executionException.getMessage());
                isCompleted = true;
                isEnabled.set(false);
            }
            catch (Exception ex){
                logger.error("UNEXPECTED EXCEPTION: " + ex.getMessage());
                //emailer.generateAndSendEmail(getNameForLog() + ex.getMessage(), screenshot("tmp/crash/exception_error.png"));
                crashesCount++;
                closeSession();
                try {
                    logger.info("Wait some time after crash");
                    TimeUnit.MINUTES.sleep(3);
                } catch (InterruptedException e) {
                    logger.error("Can't sleep\n" + e.getMessage());
                }
            }
            finally {
                closeSession();
                if(crashesOverExpected()){
                    //emailer.generateAndSendEmail(getNameForLog() + "Crashes over expected", screenshot("tmp/crash/crash_error.png"));
                    //deactivate();
                    isRunning.set(false);
                    crashesCount = 0;
                    return;
                }
                if(isCompleted || !isEnabled.get()){
                    isRunning.set(false);
                    return;
                }

            }
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.error("Can't sleep\n" + e.getMessage());
            }
        }
        endTime = LocalDateTime.now();
        isRunning.set(false);
    }

    private void mainActivities() throws InstaActorStopExecutionException, InstaActorBreakExecutionException {
        authenticate();
        checkSuspectedPopups();
        checkAndCloseNotificationsPopup();
        Collections.shuffle(allTags);
        int reactionsCounter = 0;
        for (String searchTag : allTags) {
            processedPosts.put(searchTag, new ArrayList());
            if(reactionsCounter > ThreadLocalRandom.current().nextInt(3, 10)) {
                followAccountFromYourFeed();
                reactionsCounter = 0;
            }
            if (!completedTags.contains(searchTag) && !defectedTags.contains(searchTag)) {
                if (searchByTag(searchTag)) {
                    interactWithPosts();
                    WebElement closeButton = InstaActorElements.getPostCloseButton().shouldBe(Condition.visible);
                    mouseMoveToElementAndClick(closeButton);
                }
                completedTags.add(searchTag);
            }
            reactionsCounter++;
        }
        someActions();
        followSuggestedAccounts();
    }

    private int getRandomPostsCountToView(){
        int postsToView = prop.getMaxPostsCount();
        logger.debug(getNameForLog() + "Posts to view - " + postsToView);
        return postsToView;
    }

    private void completeRun() {
        isCompleted = true;
        executionCounter++;
        endTime = LocalDateTime.now();
        sendStatusAfterCompletion();
    }

    private void sendStatusAfterCompletion() {
        EmailService.generateAndSendEmail(getNameForLog() + "Execution Completed." + generateStatusForEmail());
    }

    private void  interactWithPosts() throws InstaActorStopExecutionException, InstaActorBreakExecutionException {
        String rootElement = "//div[contains(text(), 'Top posts')]/../..";
        $(By.xpath(rootElement)).shouldBe(Condition.enabled).scrollIntoView(true);
        sleep(getRandomViewTimeout());
        WebElement firstPostToLike = $(By.xpath(rootElement+"//a")).shouldBe(Condition.enabled);
        mouseMoveToElementAndClick(firstPostToLike);
        int maxPosts = getRandomPostsCountToView();
        for(int i = 1; i <= maxPosts; i++){
            if(!isEnabled.get()){
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
                else
                {
                    logger.info("Skip post like");
                }
                if(shouldCommentPost()){
                    if(!commentedPosts.contains(currentPostUrl)) {
                        addCommentToPost();
                    }
                }
                else{
                    logger.info("Skip post comment");
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

    private void addCommentToPost() throws InstaActorBreakExecutionException {
            try {
                String commentText = InstaActorComments.generateComment(currentPostType);
                logger.debug(getNameForLog() + "Trying to add comment: " + commentText);
                sleep(getRandomViewTimeout());
                $(By.cssSelector("article textarea")).val(commentText);
                mouseMoveToElementAndClick($(By.xpath("//button[attribute::type='submit']")));
                if (suspectedActionsDetectorOnAction()) {
                    logger.info(getNameForLog() + "DISABLE LIKE AND COMMENTS ACTION!!!");
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

    private void addLikeToPost() throws InstaActorBreakExecutionException {
        if(InstaActorElements.getPostLikeButton() != null){
            logger.info(getNameForLog() + "Like post");
            sleep(getRandomViewTimeout());
            mouseMoveToElementAndClick(InstaActorElements.getPostLikeButton());
            if(suspectedActionsDetectorOnAction()){
                logger.info(getNameForLog() + "DISABLE LIKE AND COMMENTS ACTION!!!");
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
    }

    private boolean suspectedActionsDetectorOnAction() throws InstaActorBreakExecutionException {
        ElementsCollection buttonReport = $$(By.xpath("//button[contains(text(),'Report a Problem')]"));
        if(buttonReport.size() > 0){
            logger.warn(getNameForLog() + "!!!WARNING!!!");
            EmailService.generateAndSendEmail(getNameForLog() + "Like or Comment action was blocked by Instagram service\n"
                    + "<b>LIKE and COMMENT option will be disabled for current instance: " + name + "\n"
                    +"<b>Tag name:</b> " + currentTag + "\n"
                    +"<b>Post Url:</b> " + currentPostUrl + "\n", screenshot("tmp/crash/blocked_action_error.png"));
            buttonReport.get(0).click();
            sleep(10000);
            LocalDateTime triggerTime =
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()),
                            TimeZone.getDefault().toZoneId());
            prop.setBlockActionPoint(getName(), triggerTime.toString());
            throw new InstaActorBreakExecutionException("Like or Comment block. STOP!");
        }
        return false;
    }

    private boolean shouldLikePost(){
        int min = 1;
        int max = 100;
        if(ThreadLocalRandom.current().nextInt(min, max) <= prop.getLikesPercentage()) {
            return !actionsAreBlocked();
        }
        return false;
    }

    private boolean shouldCommentPost(){
        int min = 1;
        int max = 100;
        if(ThreadLocalRandom.current().nextInt(min, max) <= prop.getCommentsPercentage()) {
            return !actionsAreBlocked();
        }
        return false;
    }

    private void detectPostTypeAndAct() {
        ElementsCollection imagePost = $$(By.xpath("//div[attribute::role='dialog']//article//img[attribute::style='object-fit: cover;']"));
        if(imagePost.size()>0){
            if(imagePost.size()==1){
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

    private int getVideoRandonTimeout(){
        return ThreadLocalRandom.current().nextInt(prop.getViewMinDelayVideo(), prop.getViewMaxDelayVideo() + 1);
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
        String currentStatus = "";
        currentStatus += "\n/***************InstaActor " + name + " POST INFO*****************/\n";
        currentStatus += "|\n";
        currentStatus += "|   Tag: " + currentTag + ".\n";
        currentStatus += "|   Tags count: " + allTags.size() + ".\n";
        currentStatus += "|   Completed Tags: " + completedTags.size() + ".\n";
        currentStatus += "|   Defected Tags: " + defectedTags.size() + ".\n";
        currentStatus += "|   Number is " + currentPostPosition + " from " + prop.getMaxPostsCount() + ".\n";
        currentStatus += "|   Url: " + currentPostUrl + ".\n";
        currentStatus += "|   Type: " + currentPostType.toString() + ".\n";
        currentStatus += "|   Like: " + currentPostLikeAdded + ".\n";
        if(!Strings.isNullOrEmpty(addedComment)){
            currentStatus += "|   Added comment: " + addedComment + ".";
        }
        currentStatus += "|\n";
        return currentStatus;
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

    private void checkAndCloseNotificationsPopup() {
        //Save login info popup
        ElementsCollection otherButtons = $$(By.xpath("//button[contains(text(), 'Not Now')]"));
        if(otherButtons.size() > 0) {
            mouseMoveToElementAndClick(otherButtons.get(0));
        }
        sleep(5000);
        //Notifications
        ElementsCollection popupWindow = $$(By.xpath("//div[attribute::role='dialog']"));
        if(popupWindow.size() > 0){
            String popupText = popupWindow.get(0).find("h2").getText();
            System.out.println("!!!Popup detected - " + popupText);
            if(popupText.equalsIgnoreCase("Turn on Notifications")){
                mouseMoveToElementAndClick($(By.xpath("//button[contains(text(), 'Not Now')]")));
            }
        }

    }

    private void mouseMoveToElementAndClick(WebElement element){
        Actions action = new Actions(WebDriverRunner.getWebDriver());
        action.moveToElement(element).perform();
        element.click();
        sleep(getRandomViewTimeout());
    }

    private void authenticate() throws InstaActorStopExecutionException {
        open("https://www.instagram.com/accounts/login/?source=auth_switcher");
        sleep(getRandomViewTimeout());
        InstaActorElements.getUserLoginInput().val(prop.getUserName()).pressTab();
        InstaActorElements.getUserPasswordInput().val(prop.getUserPass()).pressEnter();
        logger.info(getNameForLog() + "Press ENTER on login");
        $(By.xpath("//button[contains(.,'Log In')]")).waitUntil(disappears, 30000);
        sleep(3000);
        checkSuspectedActionsDetectorAfterLogin();
    }

    private void checkSuspectedActionsDetectorAfterLogin() throws InstaActorStopExecutionException {
        ElementsCollection caption = $$(By.xpath("//h3[contains(.,'Your Account Was Compromised')]"));
        ElementsCollection buttonReport = $$(By.xpath("//button[contains(text(),'Report a Problem')]"));
        ElementsCollection changePasswordButton = $$(By.xpath("//button[contains(text(),'Change Password')]"));
        if((caption.size() > 0) && (changePasswordButton.size() > 0)){
            changePasswordButton.get(0).click();
            InstaActorElements.getUserOldPassInput().val(prop.getUserPass()).pressTab();
            generateNewUserPassword();
            initPropertiesAndSetInitVariables();
            InstaActorElements.getUserNewPassInput().val(prop.getUserPass()).pressTab();
            InstaActorElements.getUserConfirmNewPassInput().val(prop.getUserPass()).pressEnter();
            $(By.xpath("//button[contains(.,'Change Password')]")).waitUntil(disappears, 10000);
            sleep(3000);
        }
        if((caption.size() > 0) && (buttonReport.size() > 0)){
            logger.warn(getNameForLog() + "Can't login to account!!!. Your Account Was Compromised.");
            throw new InstaActorStopExecutionException(getNameForLog() + "CAN'T LOGIN ERROR. Your Account Was Compromised");
        }
        if($$(By.xpath("//button[contains(.,'Send Security Code')]")).size() > 0){
            logger.error(getNameForLog() + "Can't login to account");
            throw new InstaActorStopExecutionException(getNameForLog() + "LOGIN SECURITY CODE");
        }
    }

    private void generateNewUserPassword() {
        prop.setNewPassword(getName(), generatePassword(15));
    }

    private static String generatePassword(int length) {
        String capitalCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String specialCharacters = "!@#$";
        String numbers = "1234567890";
        String combinedChars = capitalCaseLetters + lowerCaseLetters + specialCharacters + numbers;
        Random random = new Random();
        char[] password = new char[length];
        password[0] = lowerCaseLetters.charAt(random.nextInt(lowerCaseLetters.length()));
        password[1] = capitalCaseLetters.charAt(random.nextInt(capitalCaseLetters.length()));
        password[2] = specialCharacters.charAt(random.nextInt(specialCharacters.length()));
        password[3] = numbers.charAt(random.nextInt(numbers.length()));

        for(int i = 4; i< length ; i++) {
            password[i] = combinedChars.charAt(random.nextInt(combinedChars.length()));
        }
        return String.valueOf(password);
    }

    private void checkSuspectedPopups() throws InstaActorStopExecutionException {
        if(InstaActorElements.getCompromisedAccountInfo()!=null){
            throw new InstaActorStopExecutionException(getNameForLog() + "COMPROMIZED");
        }
    }

    private void someActions() {
        try {
            logger.info(getNameForLog() + "Start doing smth.");
            TimeUnit.SECONDS.sleep(10);
            logger.info(getNameForLog() + "End doing smth.");
        } catch (InterruptedException e) {
            logger.error("Can't sleep\n" + e.getMessage());
        }
    }

    private void closeSession() {
        try{
            //writeListToFile(defectedTags, getDefectedTagsFilePath());
            writeListToFile(likedPosts, getLikedPostsFilePath());
            writeListToFile(commentedPosts, getCommentedPostsFilePath());
            sleep(10000);
            logger.info(getNameForLog() + "Clear WebDriver session");
            closeWebDriver();
        }
        finally {
            driver = null;
            logger.debug(getNameForLog() + "Waiting for drivers close");
            sleep(30000);
        }
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

    private void initSession() {
        logger.info(getNameForLog() + "Create WebDriver session");
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR,
                UnexpectedAlertBehaviour.IGNORE);
        chromeOptions.setHeadless(true);
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--enable-automation");
        if(!StringUtils.isEmpty(prop.getProxyValue()))
            chromeOptions.addArguments("--proxy-server=" +  prop.getProxyValue());
        if(!prop.isDebugMode()) {
            String seleniumHub = prop.getHub();
            String seleniumHubPort = String.valueOf(prop.getHubPort());
            if(Strings.isNullOrEmpty(seleniumHub) || Strings.isNullOrEmpty(seleniumHubPort)){
                seleniumHub = "localhost";
                seleniumHubPort = "4444";
            }
            String gridHubUrl = "http://" + seleniumHub + ":" + seleniumHubPort;
            if(!Utilities.checkGridStatus(gridHubUrl))
            {
                logger.error(getNameForLog() + "GRID not ready for execution. Stop service.");
                deactivate();
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

    private void verifyPreConditions() throws InstaActorBreakExecutionException {
        nigthModeCheck();
    }

    private boolean crashesOverExpected() {
        logger.debug(getNameForLog() + "Current crashes: " + crashesCount);
        if(crashesCount > 10){
            return true;
        }
        return false;
    }

    private void nigthModeCheck() throws InstaActorBreakExecutionException {
        if(prop.isNightMode()){
            throw new InstaActorBreakExecutionException("Night mode is active.");
        }
    }

    public String getNameForLog() {
        return name + " >>> ";
    }

    private void waitSomeTime(int duration){
        waitSomeTime(duration, "");
    }

    private void waitSomeTime(int duration, String msg){
        long currentPoint = System.currentTimeMillis();
        while(System.currentTimeMillis() < (currentPoint + duration)){
            logger.debug(getNameForLog() + msg + " wait duration - " + duration/1000 + " seconds.\n"
                    + ((currentPoint + duration) - System.currentTimeMillis())/1000 + " seconds left.");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private long getExecutionDuration(){
        if(startTime != null)
            return ChronoUnit.MINUTES.between(startTime, (endTime == null) ? LocalDateTime.now() : endTime);
        else
            return 0;
    }

    private String generateStatusForEmail(){
        if(prop == null){
            initProperties();
        }
        String status = "InstaActor STATUS\n"
                +"\nService name: " + name
                +"\nService is running: " + isEnabled.get()
                +"\nStarted: " + creationDate
                +"\nCurrent run start time: " + startTime
                +"\nCurrent(latest) duration (minutes): " + getExecutionDuration()
                +"\nCompleted executions: " + executionCounter
//                +"\nWas interrupted: " + interrupted
                +"\nLike percentage: " + prop.getLikesPercentage() + "%"
                +"\nComments percentage: " + prop.getCommentsPercentage() + "%"
                +"\nCompleted Tags: " + completedTags.size()
                +"\nDefected Tags: " + defectedTags.size()
                +"\nTag: " + currentTag + " from " + allTags.size()
                + "\nUrl: " + currentPostUrl
                + "\nType: " + currentPostType.toString()
                + "\nLike: " + currentPostLikeAdded;
        if(!Strings.isNullOrEmpty(addedComment)){
            status += "\nAdded comment: " + addedComment;
        }
        status += "\nLikes added Total: " + totalLiked;
        status += "\nComments added Total: " + totalComments;
        status += "\nFollow accounts Total: " + followedCount;
        return status;
    }

    private int getRandomViewTimeout(){
        return ThreadLocalRandom.current().nextInt(prop.getViewMinDelay(), prop.getViewMaxDelay() + 1);
    }

    private void followAccountFromYourFeed() throws InstaActorStopExecutionException, InstaActorBreakExecutionException {
        open("https://www.instagram.com/accounts/activity/");
        followAccounts();
    }

    private void followSuggestedAccounts() throws InstaActorStopExecutionException, InstaActorBreakExecutionException {
        open("https://www.instagram.com/explore/people/suggested/");
        sleep(10000);
        followAccounts();
    }

    private boolean actionsAreBlocked(){
        LocalDateTime stopPoint = prop.getStopPoint();
        if(stopPoint == null)
            return false;
        if (System.currentTimeMillis() > stopPoint.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + prop.getActionPauseDurationHours() * 3600000)
            return false;
        return true;
    }

    private void followAccounts() throws InstaActorBreakExecutionException {
        waitSomeTime(getRandomViewTimeout());
        ElementsCollection followButtons = $$(By.xpath("//button[text()='Follow']"));
        int maxItems = (followButtons.size()>5)?5:followButtons.size();
        if(!actionsAreBlocked()) {
            logger.info(getNameForLog() + "Review and follow accounts");
            for (int i = 0; i < maxItems; i++) {
                if (followButtons.get(0).is(Condition.visible)) {
                    logger.info(getNameForLog() + "follow account");
                    try {
                        mouseMoveToElementAndClick(followButtons.get(0));
                        waitSomeTime(getRandomViewTimeout());
                        suspectedActionsDetectorOnAction();
                        followedCount++;
                    } catch (ElementClickInterceptedException ex) {
                        logger.debug(getNameForLog() + "Can't click at follow button");
                    }
                }
                waitSomeTime(getRandomViewTimeout());
            }
        }
        logger.info(getNameForLog() + "SKIP. Review and follow accounts");
    }
}
