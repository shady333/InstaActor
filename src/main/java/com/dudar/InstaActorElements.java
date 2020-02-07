package com.dudar;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$$;

public class InstaActorElements {

    final static Logger logger = Logger.getLogger(InstaActorElements.class);

    public static SelenideElement getUserLoginInput(){
        return getControl("User Login input", By.name("username"));
    }

    public static SelenideElement getUserPasswordInput(){
        return getControl("User Password input", By.name("password"));
    }

    public static SelenideElement getPostLikeButton() {
        return getControlNoException("Post Like button", By.cssSelector("svg[aria-label=\"Like\"][height=\"24\"]"));
    }

    public static SelenideElement getPostCloseButton() {
        return getControl("Close Post button", By.cssSelector("svg[aria-label=\"Close\"]"));
    }

    private static SelenideElement getControl(String name, By by){
        ElementsCollection collection = $$(by);
        if(collection.size() > 0) {
            return collection.get(0).shouldBe(Condition.visible);
        }
        else {
            String errorMessage = "Can't find element - " + name
                    +" by locator " + by.toString();
            logger.error(errorMessage);
            throw new AssertionError(errorMessage);
        }
    }

    private static SelenideElement getControlNoException(String name, By by){
        ElementsCollection collection = $$(by);
        if(collection.size() > 0) {
            return collection.get(0).waitUntil(Condition.visible, 30000);
        }
        else {
            String errorMessage = "Can't find element - " + name
                    +" by locator " + by.toString();
            logger.error(errorMessage);
            return null;
        }
    }


}
