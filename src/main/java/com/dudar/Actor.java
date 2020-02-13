package com.dudar;

public interface Actor {

    String getName();

    Actor start();

    Actor stop();

    String getStatus();

    boolean isAlive();

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
