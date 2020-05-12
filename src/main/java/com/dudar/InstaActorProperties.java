package com.dudar;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class InstaActorProperties {

    final static Logger logger = Logger.getLogger(InstaActorProperties.class);

    private int viewMinDelay;
    private int viewMaxDelay;

    public int getViewMinDelay() {
        return viewMinDelay;
    }

    public int getViewMaxDelay() {
        return viewMaxDelay;
    }

    public int getViewMinDelayVideo() {
        return viewMinDelayVideo;
    }

    public int getViewMaxDelayVideo() {
        return viewMaxDelayVideo;
    }

    public int getLikesPercentage() {
        return likesPercentage;
    }

    public void setLikesPercentage(int value) {
        likesPercentage = value;
    }

    public int getCommentsPercentage() {
        return commentsPercentage;
    }

    public void setCommentsPercentage(int value) {
        commentsPercentage = value;
    }

    public int getMaxPostsCount() {
        return maxPostsCount;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserPass() {
        return userPass;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isDetectMediaContent() {
        return detectMediaContant;
    }

    public boolean isEmailServiceEnabled() {
        return emailServiceEnabled;
    }

    public boolean isRepeatActionsAfterComplete() {
        return repeatActionsAfterComplete;
    }

    public int getSleepDurationBetweenRunsInHours() {
        return sleepDurationBetweenRunsInHours;
    }

    public String getProxyValue() {
        return proxyValue;
    }

    public boolean isNightMode() {
        return nightMode;
    }

    public String getHub() {
        return hub;
    }

    public int getHubPort() {
        return hubPort;
    }

    private String hub = "localhost";
    private int hubPort = 4444;
    private int viewMinDelayVideo;
    private int viewMaxDelayVideo;
    private int likesPercentage;
    private int commentsPercentage;
    private int maxPostsCount;
    private String userName;
    private String userPass;
    private boolean debugMode;
    private boolean detectMediaContant;
    private boolean emailServiceEnabled;
    private boolean repeatActionsAfterComplete;
    private int sleepDurationBetweenRunsInHours;
    private String proxyValue;
    private boolean nightMode;
    private int pauseDurationWhileDetected;

    public InstaActorProperties(String actorName){
        Properties actorProperties;
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream("data/" + actorName + "_user.properties"));
            actorProperties = prop;


            if(!StringUtils.isEmpty(actorProperties.getProperty("view.min.delay")))
                viewMinDelay = Integer.parseInt(actorProperties.getProperty("view.min.delay"));
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
            if(!StringUtils.isEmpty(actorProperties.getProperty("debug.mode")))
                debugMode = Boolean.parseBoolean(actorProperties.getProperty("debug.mode"));
            if(!StringUtils.isEmpty(actorProperties.getProperty("detect.media.content")))
                detectMediaContant = Boolean.parseBoolean(actorProperties.getProperty("detect.media.content"));
            if(!StringUtils.isEmpty(actorProperties.getProperty("email.service")))
                emailServiceEnabled = Boolean.parseBoolean(actorProperties.getProperty("email.service"));
            if(!StringUtils.isEmpty(actorProperties.getProperty("service.repeat")))
                repeatActionsAfterComplete = Boolean.parseBoolean(actorProperties.getProperty("service.repeat"));
            if(!StringUtils.isEmpty(actorProperties.getProperty("sleep.duration")))
                sleepDurationBetweenRunsInHours = Integer.parseInt(actorProperties.getProperty("sleep.duration"));
            if(!StringUtils.isEmpty(actorProperties.getProperty("proxy")))
                proxyValue = actorProperties.getProperty("proxy");
            if(!StringUtils.isEmpty(actorProperties.getProperty("night.mode")))
                nightMode = Boolean.parseBoolean(actorProperties.getProperty("night.mode"));

            if(!StringUtils.isEmpty(actorProperties.getProperty("hub.host")))
                hub = actorProperties.getProperty("hub.host");
            if(!StringUtils.isEmpty(actorProperties.getProperty("hub.port")))
                hubPort = Integer.parseInt(actorProperties.getProperty("hub.port"));

            if(!StringUtils.isEmpty(actorProperties.getProperty("pause.duration")))
                pauseDurationWhileDetected = Integer.parseInt(actorProperties.getProperty("pause.duration"));

        } catch (IOException e) {
            logger.error("Can't reinit properties from file");
            e.printStackTrace();
        }


    }

    public long getActionPauseDurationHours() {
        return pauseDurationWhileDetected;
    }
}
