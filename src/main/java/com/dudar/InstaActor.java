package com.dudar;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.dudar.utils.Utilities;
import com.google.common.base.Strings;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codeborne.selenide.Selenide.*;

public class InstaActor {

    private String login;
    private String pass;
    private boolean likesEnabled = false;
    private int totalLiked = 0;
    private final int maxPostsCount = 50;
    private int warningsCounter = 0;
    private final int minViewDelay = 100;
    private final int maxViewDelay = 500;
    private final int minVideoDelay = 100;
    private final int maxVideoDelay = 500;
    private final int likesPercentage = 10;
    private final int commentsPercentage = 10;
    List<String> tags = new ArrayList<>();
    final List<String> completedTags = new ArrayList<>();
    final List<String> defectedTags = new ArrayList<>();
    private boolean executionError;
    private int totalComments=0;
    private boolean commentsEnabled;

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
            System.out.println(Utilities.getCurrentTimestamp() + "No defected tags");
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
                return;
            }
            else
            {
                System.out.println("Post type - Gallery");
                for(int i =1; i < imagePost.size(); i++){
                    System.out.println("Navigate to next image > " + i);
                    mouseMoveToElementAndClick($(By.cssSelector(".coreSpriteRightChevron")));
                }
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
        }
    }

    private boolean likePost(){
        int min = 1;
        int max = 100;
        boolean result;
        if(ThreadLocalRandom.current().nextInt(min, max) <= likesPercentage) {
            System.out.println(Utilities.getCurrentTimestamp() + "Like it");
            totalLiked++;
            result = true;
        }
        else {
            System.out.println(Utilities.getCurrentTimestamp() + "Skip post. Do not like it.");
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
            totalComments++;
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
            System.out.println(Utilities.getCurrentTimestamp() + i + ". Current page - " + WebDriverRunner.url());
            $(By.xpath("//button[contains(text(), 'Close')]")).shouldBe(Condition.visible).shouldBe(Condition.enabled);
            if(InstaActorElements.getPostLikeButton()!=null){
                sleep(getRandonTimeout());
                detectPostTypeAndAct();
                if(likePost()){
                    System.out.println("!!!LIKE!!!");
                    if(likesEnabled) {
                        mouseMoveToElementAndClick(InstaActorElements.getPostLikeButton());
                        if (suspectedActionsDetector())
                            return;
                    }
                    else{
                        System.out.println("!!!Likes option is disabled");
                    }
                    if(commentsEnabled && commentPost()) {
                        addCommentToPost();
                        if (suspectedActionsDetector())
                            return;
                    }
                }
            }
            WebElement nextPostButton = $(By.xpath("//a[contains(text(), 'Next')]")).shouldBe(Condition.visible);
            mouseMoveToElementAndClick(nextPostButton);
            sleep(getRandonTimeout());
        }
    }

    private boolean suspectedActionsDetector() {
        ElementsCollection buttonReport = $$(By.xpath("//button[contains(text(),'Report a Problem')]"));
        if(buttonReport.size() > 0){
            warningsCounter++;
            if(warningsCounter>2){
                System.out.println(Utilities.getCurrentTimestamp() + "!!!WARNING!!!");
                System.out.println("SKIP CURRENT TAG Liking\nBREAK!!!!");
                System.out.println("Completed tags:");
                completedTags.forEach(System.out::println);
                System.out.println("Total LIKES - " + getTotalLikes());
                System.out.println("!!!STOP EXECUTION");
                System.exit(1);
                return true;
            }
            System.out.println(Utilities.getCurrentTimestamp() + "!!!WARNING!!!");
            System.out.println("Detected suspicious action detected by service");
            buttonReport.get(0).click();
            sleep(getRandonTimeout());
            if(InstaActorElements.getPostLikeButton()!=null) {
                System.out.println("Re Like current post");
                if(likesEnabled)
                    mouseMoveToElementAndClick(InstaActorElements.getPostLikeButton());
                sleep(getRandonTimeout());
            }
            System.out.println("Switching to next tag for likes");
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
                        System.out.println(Utilities.getCurrentTimestamp() + "Search Tag - " + searchTag);
                        System.out.println("Current tag is " + tagCounter + " from " + tagsCollectionSize + " all of Tags");
                        tagCounter.getAndIncrement();
                            if (searchByTag(searchTag)) {
                                    interactWithPosts(maxPostsCount);
                                WebElement closeButton = $(By.xpath("//button[contains(text(), 'Close')]")).shouldBe(Condition.visible);
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

    private List<String> comments = new ArrayList<>(Arrays.asList(
            "Cool post",
            "Nice shot",
            "I like it"
            ));

    private String getComment(){
        int maxVal = comments.size();
        int commentIndex = ThreadLocalRandom.current().nextInt(0, maxVal);
        return comments.get(commentIndex);
    }

    private void addCommentToPost(){
            try {
                String commentText = getComment();
                System.out.println(commentText);
                $(By.cssSelector("article textarea")).val(commentText);

                //TODO add emojji support

                mouseMoveToElementAndClick($(By.xpath("//button[attribute::type='submit']")));
                System.out.println("Comment added!!!");
                totalComments++;
            } catch (Error err) {
                System.out.println("ERROR on commenting");
                throw err;
            } catch (Exception ex) {
                System.out.println("EXCEPTION on commenting");
                throw ex;
            }
    }
}
