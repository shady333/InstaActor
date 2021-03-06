package com.dudar;

public interface Actor {

    String getNameForLog();

    Actor start();

    Actor stop();

    String getStatus();

    boolean isAlive();

    boolean isActive();

    boolean isInterrupted();

    Thread.State getState();

    String getThreadStatus();

    boolean isCompleted();

    boolean isStopped();

    void enableLikeAction();

    void disableLikeAction();

    void enableCommentsAction();

    void disableCommentsAction();
}
