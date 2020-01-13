package com.dudar;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codeborne.selenide.Selenide.*;

public class InstaActor {

    private String login;
    private String pass;
    private boolean debug = false;
    private boolean likesEnabled = false;
    private int totalLiked = 0;
    private int maxPostsCount = 50;
    private int warningsCounter = 0;
    private int minViewDelay = 1000;
    private int maxViewDelay = 5000;
    private int minVideoDelay = 2000;
    private int maxVideoDelay = 10000;
    private int likesPercentage = 90;
    List<String> tags = new ArrayList<>();
    List<String> completedTags = new ArrayList<>();
    private boolean executionError;

    public InstaActor(){
        ;
    }

    public List<String> getCompletedTags(){
        return this.completedTags;
    }

    public InstaActor loadTags(List<String> tagsToLoad){
        this.tags = tagsToLoad;
        return this;
    }

    public InstaActor enableDebug(){
        this.debug = true;
        return this;
    }

    public InstaActor enableLikes(String value){
        if(value.equalsIgnoreCase("true"))
            this.likesEnabled = true;
        else
            this.likesEnabled = false;
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

    public InstaActor build(){
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
        try {
            element.click();
        }
        catch (Exception ex){
            throw ex;
        }
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
            return false;
        }
    }

    private String detectPostTypeAndAct() {
        ElementsCollection imagePost = $$(By.xpath("//div[attribute::role='dialog']//article//img[attribute::style='object-fit: cover;']"));
        if(imagePost.size()>0){
            if(imagePost.size()==1){
                System.out.println("Post type - Image");
                return "IMG";
            }
            else
            {
                System.out.println("Post type - Gallery");
                for(int i =1; i < imagePost.size(); i++){
                    System.out.println("Navigate to next image > " + i);
                    mouseMoveToElementAndClick($(By.cssSelector(".coreSpriteRightChevron")));
                }
                return "GALLERY";
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
            return "VIDEO";
        }
        return "undefined";
    }

    private boolean likePost(){
        int min = 1;
        int max = 100;
        boolean result;
        if(ThreadLocalRandom.current().nextInt(min, max + 1) <= likesPercentage) {
            System.out.println("Like it");
            totalLiked++;
            result = true;
        }
        else {
            System.out.println("Skip post. Do not like it.");
            result = false;
        }
        return result;
    }

    private void likePosts(int maxPostsCount){
        String rootElement = "//div[contains(text(), 'Top posts')]/../..";
        String likesCollectionElementsLocator =
                "//article//span[attribute::aria-label='Share Post']/../..//span[attribute::aria-label='Like']";
//        try {
        $(By.xpath(rootElement)).shouldBe(Condition.enabled).scrollIntoView(true);
//        }
//        catch (AssertionError ex){
//            System.out.println(ex.getLocalizedMessage());
//            throw new MyCustomException("asd");
//        }
        sleep(getRandonTimeout());

        WebElement firstPostToLike = $(By.xpath(rootElement+"//a")).shouldBe(Condition.enabled);
        mouseMoveToElementAndClick(firstPostToLike);

        for(int i = 0; i < maxPostsCount; i++){
            System.out.println(i + ". Current page - " + WebDriverRunner.url());
            $(By.xpath("//button[contains(text(), 'Close')]")).shouldBe(Condition.visible).shouldBe(Condition.enabled);
            ElementsCollection likesCollection = $$(By.xpath(likesCollectionElementsLocator));
            if(likesCollection.size() > 0){
                sleep(getRandonTimeout());

                detectPostTypeAndAct();

                if(likePost()){
                    System.out.println("!!!LIKE!!!");
                    if(likesEnabled)
                        mouseMoveToElementAndClick(likesCollection.get(0));
                    else
                        System.out.println("!!!Liking option is disabled!!!");

                    //suspected actions
                    ElementsCollection buttonReport = $$(By.xpath("//button[contains(text(),'Report a Problem')]"));
                    if(buttonReport.size() > 0){
                        warningsCounter++;
                        if(warningsCounter>2){
                            System.out.println("!!!WARNING!!!");
                            System.out.println("SKIP CURRENT TAG Liking\nBREAK!!!!");
                            System.out.println("Completed tags:");
                            completedTags.forEach(el -> System.out.println(el));
                            System.out.println("Total LIKES - " + getTotalLikes());
                            System.out.println("!!!STOP EXECUTION");

                            System.exit(1);
                            return;
                        }
                        System.out.println("!!!WARNING!!!");
                        System.out.println("Detected suspicious action detected by service");
                        buttonReport.get(0).click();
                        sleep(getRandonTimeout());
                        likesCollection = $$(By.xpath(likesCollectionElementsLocator));
                        if(likesCollection.size() > 0) {
                            System.out.println("Re Like current post");
                            if(likesEnabled)
                                mouseMoveToElementAndClick(likesCollection.get(0));
                            sleep(getRandonTimeout());
                        }
                        System.out.println("Switching to next tag for likes");
                        return;
                    }

                }
            }
//            try {
                WebElement nextPostButton = $(By.xpath("//a[contains(text(), 'Next')]")).shouldBe(Condition.visible);
                mouseMoveToElementAndClick(nextPostButton);
//            }
//            catch (Exception ex){
//                System.out.println("!!!No next button");
//                return;
//            }

            sleep(getRandonTimeout());
        }
    }

    private void emergencyReload(){
        open("https://instagram.com");
        $(By.cssSelector("input[placeholder=\"Search\"]")).shouldBe(Condition.visible);
        $(By.cssSelector("svg[aria-label=\"Instagram\"]")).shouldBe(Condition.visible);

        checkIfPopupShown();
    }

    private boolean likeComplated = false;

    public void start(){
        while(!likeComplated) {
//            try {
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
                        System.out.println("Search Tag - " + searchTag);
                        System.out.println("Current tag is " + tagCounter + " from " + tagsCollectionSize + " all of Tags");
                        tagCounter.getAndIncrement();
//                        try {
                            if (searchByTag(searchTag)) {
//                                try {
                                    likePosts(maxPostsCount);
//                                }
//                                 catch (AssertionError ex){
//                                        System.out.println(ex.getLocalizedMessage());
////                                        throw new MyCustomException("asd");
//                                    }
                                WebElement closeButton = $(By.xpath("//button[contains(text(), 'Close')]")).shouldBe(Condition.visible);
                                mouseMoveToElementAndClick(closeButton);

                            }
//                            completedTags.add(searchTag);

//                        } catch (NoSuchElementException ex) {
//                            System.out.println("!!!Error occured\n" + ex.getLocalizedMessage());
//                            emergencyReload();
//                        }
                    }
                }
                likeComplated = true;

//            } catch (AssertionError ex) {
//                System.out.println("!!!Error on InstActor: " + ex.getLocalizedMessage());
//                clearBrowserLocalStorage();
//                clearBrowserCookies();
//                close();
//                sleep(10000);
//                start();
//            }

        }
        System.out.println("*******************************");
        System.out.println("Total likes = " + totalLiked);

//        clearBrowserLocalStorage();
//        clearBrowserCookies();
//        close();
    }

    public int getTotalLikes(){
        return this.totalLiked;
    }
}
