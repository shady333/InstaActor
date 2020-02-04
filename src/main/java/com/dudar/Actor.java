package com.dudar;

public interface Actor {

    Actor start();

    Actor stop();

    String getStatus();

    boolean isAlive();

    boolean isInterrupted();

    Thread.State getState();

    String getThreadStatus();

    boolean isCompleted();
}
