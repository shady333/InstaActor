package com.dudar;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.dudar.utils.ImageAnalyzer;
import com.dudar.utils.Utilities;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codeborne.selenide.Selenide.*;

public class InstaActor {

    private boolean currentPostLikeAdded;
    private boolean currentAlreadyLike;
    private String addedComment = "";
    private String currentStatus = "";

    enum PostType{
        PHOTO,
        VIDEO,
        GALLERY,
        UNDEFINED
    }

    private List<String> commentsVideo = new ArrayList<>(Arrays.asList(
            "Cool Video!",
            "Cool Video!!!",
            "Cool video !",
            "Cool video !!!",
            "Cool VIDEO!",
            "Cool VIDEO!!!",
            "cool video !",
            "cool video !!!",
            "Nice Video",
            "Nice Video !",
            "Nice Video!!!",
            "Nice VIDEO !",
            "NICE VIDEO!",
            "Good Video!!!",
            "Good Video"
    ));

    private List<String> comment1 = new ArrayList<>(Arrays.asList(
            "Cool",
            "Nice",
            "Good"
    ));

    private List<String> comment2 = new ArrayList<>(Arrays.asList(
            " shots",
            " Shots",
            " picture",
            " Picture",
            " photo",
            " Photo"
    ));

    private List<String> comment3 = new ArrayList<>(Arrays.asList(
            ".",
            "!",
            "!!!",
            " !",
            " !!!",
            "!!"
    ));

    private List<String> comments = new ArrayList<>(Arrays.asList(
            "Awesome!",
            "AWESOME!!!",
            "Amazing",
            "Thumb Up!",
            "Get my like"
    ));

    final static Logger logger = Logger.getLogger(InstaActor.class);

    private String login;
    private String pass;
    private boolean likesEnabled = false;
    private int totalLiked = 0;
    private int maxPostsCount = 10;
    private int warningsCounter = 0;
    private int minViewDelay = 500;
    private int maxViewDelay = 1000;
    private int minVideoDelay = 2000;
    private int maxVideoDelay = 3000;
    private int likesPercentage = 50;
    private int commentsPercentage = 10;
    List<String> tags = new ArrayList<>();
    final List<String> completedTags = new ArrayList<>();
    final List<String> defectedTags = new ArrayList<>();
    private int totalComments=0;
    private boolean commentsEnabled;
    private PostType currentPostType = PostType.UNDEFINED;

    private String currentTag;

    public String getCurrentTag() {
        return currentTag;
    }

    public String getCurrentPostUrl() {
        return currentPostUrl;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    private String currentPostUrl;
    private int currentPosition;

    public void viewCurrentParameters(){
        logger.info("*****InstaActor Parameters*****");
        logger.info("Like enabled - " + likesEnabled);
        logger.info("Like percentage - " + likesPercentage);
        logger.info("Comment enabled - " + commentsEnabled);
        logger.info("Comment percentage - " + commentsPercentage);
        logger.info("View parameters: " + minViewDelay + " " + maxViewDelay);
        logger.info("Video parameters: " + minVideoDelay + " " + maxVideoDelay);
        logger.info("*****InstaActor Parameters*****");
    }

    public InstaActor setMaxPostsCount(int value){
        if(value > 0)
            this.maxPostsCount = value;
        return this;
    }

    public InstaActor setMinViewDelay(int value){
        if(value > 0)
            this.minViewDelay = value;
        return this;
    }

    public InstaActor setMaxViewDelay(int value){
        if(value > 0)
            this.maxViewDelay = value;
        return this;
    }

    public InstaActor setMinVideoDelay(int value){
        if(value > 0)
            this.minVideoDelay = value;
        return this;
    }

    public InstaActor setMaxVideoDelay(int value){
        if(value > 0)
            this.maxVideoDelay = value;
        return this;
    }

    public InstaActor setLikesPercentage(int value){
        if(value > 0)
            this.likesPercentage = value;
        return this;
    }

    public InstaActor setCommentsPercentage(int value){
        if(value > 0)
            this.commentsPercentage = value;
        return this;
    }

    public InstaActor(){
    }

    public List<String> getCompletedTags(){
        return this.completedTags;
    }

    public void printDefectedTags(){
        if(defectedTags.size()!=0){
            defectedTags.forEach(System.out::println);
        }
        else{
            logger.info("No defected tags");
        }
    }

    public InstaActor loadTags(List<String> tagsToLoad){
        this.tags = tagsToLoad;
        return this;
    }

    public InstaActor enableLikes(String value){
        if(!Strings.isNullOrEmpty(value))
            this.likesEnabled = value.equalsIgnoreCase("true");
        return this;
    }

    public InstaActor enableComments(String value){
        if(!Strings.isNullOrEmpty(value))
            this.commentsEnabled = value.equalsIgnoreCase("true");
        return this;
    }

    public InstaActor setLogin(String login){
        this.login = login;
        return this;
    }

    public InstaActor setPassword(String password){
        this.pass = password;
        return this;
    }

    private void authentificate() {
        open("https://www.instagram.com/accounts/login/?source=auth_switcher");
        sleep(3000);
        $(By.name("username")).val(this.login).pressTab();
        $(By.name("password")).val(this.pass).pressEnter();
        sleep(3000);
    }

    private void checkIfPopupShown() {
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
        sleep(getRandonTimeout());
        Actions action = new Actions(WebDriverRunner.getWebDriver());
        action.moveToElement(element).perform();
        element.click();
        sleep(getRandonTimeout());
    }

    private int getRandonTimeout(){
        return ThreadLocalRandom.current().nextInt(minViewDelay, maxViewDelay + 1);
    }

    private int getVideoRandonTimeout(){
        return ThreadLocalRandom.current().nextInt(minVideoDelay, maxVideoDelay + 1);
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
        System.out.println("Current page Tag - "+tagLocator.getText());
        if(tagLocator.getText().equalsIgnoreCase("#"+searchTag)){
            this.currentTag = searchTag;
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
                System.out.println("Post type - Image");

                try {
                    String imageUrl = imagePost.get(0).getAttribute("srcset").split(" ")[0];

                    URL imageURL = new URL(imageUrl);
                    BufferedImage saveImage = ImageIO.read(imageURL);
                    String savedImagePath = "tmp/current_post_image.jpg";
                    ImageIO.write(saveImage, "jpg", new File(savedImagePath));

                    //TODO Image Recognition

                    ImageAnalyzer.imageType(savedImagePath);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.currentPostType = PostType.PHOTO;
                return;
            }
            else
            {
                System.out.println("Post type - Gallery");
                for(int i =1; i < imagePost.size(); i++){
                    System.out.println("Navigate to next image > " + i);
                    mouseMoveToElementAndClick($(By.cssSelector(".coreSpriteRightChevron")));
                }
                this.currentPostType = PostType.GALLERY;
                return;
            }
        }
        imagePost = $$(By.xpath("//div[attribute::role='dialog']//article//video[attribute::type='video/mp4']"));
        if(imagePost.size() > 0)
        {
            System.out.println("Post type - Video");
            WebElement videoButton = $(By.xpath(
                    "//div[attribute::role='dialog']//article//video[attribute::type='video/mp4']/../../../../..")).shouldBe(Condition.enabled);
            videoButton.click();
            sleep(getVideoRandonTimeout());
            videoButton.click();
            this.currentPostType = PostType.VIDEO;
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

    private void interactWithPosts(int maxPostsCount){
        String rootElement = "//div[contains(text(), 'Top posts')]/../..";
        $(By.xpath(rootElement)).shouldBe(Condition.enabled).scrollIntoView(true);
        sleep(getRandonTimeout());

        WebElement firstPostToLike = $(By.xpath(rootElement+"//a")).shouldBe(Condition.enabled);
        mouseMoveToElementAndClick(firstPostToLike);

        //TODO detect count of available posts. Should not exceed maxPostsCount
        for(int i = 1; i <= maxPostsCount; i++){
            currentPostUrl = WebDriverRunner.url();
            $(By.xpath("//button[contains(text(), 'Close')]")).shouldBe(Condition.visible).shouldBe(Condition.enabled);
            if(InstaActorElements.getPostLikeButton()!=null){
                sleep(getRandonTimeout());
                detectPostTypeAndAct();
                if(likePost()){

                    if(likesEnabled) {
                        addLikeToPost();
                        if (suspectedActionsDetector())
                            return;
                    }
                    else{
                        logger.info("!!!Likes option is disabled");
                    }
                    if(commentsEnabled) {
                        //TODO Post comment according to image type
                        addCommentToPost();
                        if (suspectedActionsDetector())
                            return;
                    }
                }
            }
            currentStatus += "\n/***************InstaActor POST INFO*****************/\n";
            currentStatus += "|\n";
            currentStatus += "|   Tag: " + this.currentTag + ".\n";
            currentStatus += "|   Number is " + i + " from " + maxPostsCount + ".\n";
            currentStatus += "|   Url: " + this.currentPostUrl + ".\n";
            currentStatus += "|   Type: " + this.currentPostType.toString() + ".\n";
            currentStatus += "|   Like: " + this.currentPostLikeAdded + ".\n";
            if(!Strings.isNullOrEmpty(this.addedComment)){
                currentStatus += "|   Added comment: " + this.addedComment + ".";
            }
            currentStatus += "|\n";

            logger.info("Current post info:\n" + currentStatus);

            resetCurrentPostStatus();

            WebElement nextPostButton = $(By.xpath("//a[contains(text(), 'Next')]")).shouldBe(Condition.visible);
            mouseMoveToElementAndClick(nextPostButton);
            sleep(getRandonTimeout());
        }
    }

    private void resetCurrentPostStatus() {
        this.currentPostLikeAdded = false;
        this.addedComment = "";
        this.currentPostType = PostType.UNDEFINED;
        this.currentStatus = "";
    }

    private void addLikeToPost() {
        logger.info("Like post");
        mouseMoveToElementAndClick(InstaActorElements.getPostLikeButton());
        this.currentPostLikeAdded = true;
        totalLiked++;
    }

    private boolean suspectedActionsDetector() {
        ElementsCollection buttonReport = $$(By.xpath("//button[contains(text(),'Report a Problem')]"));
        if(buttonReport.size() > 0){
            warningsCounter++;
            if(warningsCounter>2){
                logger.warn("!!!WARNING!!!");
                logger.warn("SKIP CURRENT TAG Liking\nBREAK!!!!");
                System.out.println("Completed tags:");
                completedTags.forEach(System.out::println);
                System.out.println("Total LIKES - " + getTotalLikes());
                System.out.println("!!!STOP EXECUTION");
                System.exit(1);
                resetCurrentPostStatus();
                return true;
            }
            logger.warn("!!!WARNING!!!");
            logger.warn("Detected suspicious action detected by service");
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

    private boolean likeComplated = false;

    public void start(){
        while(!likeComplated) {
                authentificate();
                $(By.cssSelector("input[placeholder=\"Search\"]")).shouldBe(Condition.visible);
                $(By.cssSelector("svg[aria-label=\"Instagram\"]")).shouldBe(Condition.visible);
                checkIfPopupShown();
                Collections.shuffle(this.tags);
                int tagsCollectionSize = this.tags.size();
                AtomicInteger tagCounter = new AtomicInteger(1);
                for(String searchTag : tags){
                    if (!completedTags.contains(searchTag)) {
                        completedTags.add(searchTag);
                        logger.info("Current tag is " + tagCounter + " from " + tagsCollectionSize + " all of Tags");
                        tagCounter.getAndIncrement();
                            if (searchByTag(searchTag)) {
                                    interactWithPosts(maxPostsCount);
                                WebElement closeButton = InstaActorElements.getPostCloseButton().shouldBe(Condition.visible);
                                mouseMoveToElementAndClick(closeButton);
                            }
                    }
                }
                likeComplated = true;
        }
    }

    public int getTotalLikes(){
        return this.totalLiked;
    }

    public int getTotalComments(){
        return this.totalComments;
    }



    private String getComment(){
        if(ThreadLocalRandom.current().nextInt(0, 100) > 50){
            int maxVal = comments.size();
            int commentIndex = ThreadLocalRandom.current().nextInt(0, maxVal);
            return comments.get(commentIndex);
        }
        else{
            if(this.currentPostType == PostType.VIDEO){
                logger.info("Return Video comment");
                return commentsVideo.get(ThreadLocalRandom.current().nextInt(0, commentsVideo.size()));
            }
            logger.info("Return Other Content comment");
            return
                    comment1.get(ThreadLocalRandom.current().nextInt(0, comment1.size()))
                            .concat(
                                    comment2.get(ThreadLocalRandom.current().nextInt(0, comment2.size()))
                            ).concat(
                            comment3.get(ThreadLocalRandom.current().nextInt(0, comment3.size()))
                    );
        }
    }

    private void addCommentToPost(){
        if(commentPost())
        {
        try {
                String commentText = getComment();
                logger.debug("Trying to add comment: " + commentText);
                $(By.cssSelector("article textarea")).val(commentText);

                //TODO add emojji support
                //Commented part for posting emojji, not working yet
//                String JS_ADD_TEXT_TO_INPUT = "var elm = arguments[0], txt = arguments[1]; elm.value += txt; elm.dispatchEvent(new Event('change'));";
//                WebElement textBox = $(By.cssSelector("article textarea"));
//                executeJavaScript(JS_ADD_TEXT_TO_INPUT, textBox, commentText);

                mouseMoveToElementAndClick($(By.xpath("//button[attribute::type='submit']")));
                logger.info("Comment added: " + commentText);
                totalComments++;
                addedComment = commentText;
            } catch (Error err) {
                logger.error("ERROR on commenting" + err.getLocalizedMessage());
            }
        }
        else{
            logger.info("Skip comment");
        }
    }
}
