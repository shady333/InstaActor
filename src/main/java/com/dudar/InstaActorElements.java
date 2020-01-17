package com.dudar;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$$;

public class InstaActorElements {

    public static SelenideElement getPostLikeButton() {
        ElementsCollection collection = $$(By.cssSelector("svg[aria-label=\"Like\"][height=\"24\"]"));
        if(collection.size() > 0) {
            return collection.get(0);
        }
        else {
            System.out.println("!!! - Can't find LIKE button");
            return null;
        }
    }


}
