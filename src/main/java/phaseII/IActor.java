package phaseII;

public interface IActor {

    String name = null;

    String getStatus();

    void deactivate();

    String getName();

    boolean isEnabled();

    boolean isActive();

    void activate();

    String getActorStatusInfo();
}
