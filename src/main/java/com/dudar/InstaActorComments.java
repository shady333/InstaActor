package com.dudar;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class InstaActorComments {

    final static Logger logger = Logger.getLogger(InstaActorComments.class);

    private static List<String> commentsVideo = new ArrayList<>(Arrays.asList(
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
    private static List<String> comment1 = new ArrayList<>(Arrays.asList(
            "Cool",
            "Nice",
            "Good",
            "Wow"
    ));
    private static List<String> comment2 = new ArrayList<>(Arrays.asList(
            " shots",
            " Shots",
            " picture",
            " Picture",
            " photo",
            " Photo"
    ));
    private static List<String> comment3 = new ArrayList<>(Arrays.asList(
            ".",
            "!",
            "!!!",
            " !",
            " !!!",
            "!!"
    ));
    private static List<String> comments = new ArrayList<>(Arrays.asList(
            "Awesome!",
            "AWESOME!!!",
            "Amazing",
            "Thumb Up!",
            "Get my like"
    ));

    public static String generateComment(PostType postType){

        //TODO add emojji support
        //Commented part for posting emojji, not working yet
//                String JS_ADD_TEXT_TO_INPUT = "var elm = arguments[0], txt = arguments[1]; elm.value += txt; elm.dispatchEvent(new Event('change'));";
//                WebElement textBox = $(By.cssSelector("article textarea"));
//                executeJavaScript(JS_ADD_TEXT_TO_INPUT, textBox, commentText);

        if(ThreadLocalRandom.current().nextInt(0, 100) > 50){
            int maxVal = comments.size();
            int commentIndex = ThreadLocalRandom.current().nextInt(0, maxVal);
            return comments.get(commentIndex);
        }
        else{
            if(postType == PostType.VIDEO){
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
}
