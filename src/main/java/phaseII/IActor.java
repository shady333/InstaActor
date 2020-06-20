package phaseII;

public interface IActor {

    String name = null;

    String getStatus();

    void stop();

    String getName();

    boolean isEnabled();

    boolean isActive();

    void activate();
}
