package phaseII;

public interface IActor {

    String name = null;

    String getStatus();

    void stop();

    String getName();

    boolean isRunning();

}