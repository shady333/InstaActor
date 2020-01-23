package com.dudar;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$$;

public class InstaActorElements {

    final static Logger logger = Logger.getLogger(InstaActorElements.class);

    public static SelenideElement getPostLikeButton() {
        ElementsCollection collection = $$(By.cssSelector("svg[aria-label=\"Like\"][height=\"24\"]"));
        if(collection.size() > 0) {
            return collection.get(0);
        }
        else {
            logger.debug("Like button is not available or already Liked");
            return null;
        }
    }

    public static SelenideElement getPostCloseButton() {
        ElementsCollection collection = $$(By.cssSelector("//button[contains(text(), 'Close')]"));
        if(collection.size() > 0) {
            return collection.get(0);
        }
        else {
            logger.debug("Close Post button not available");
            return null;
        }
    }


}
