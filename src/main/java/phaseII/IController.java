package phaseII;

import com.dudar.utils.services.ActorActions;

import java.util.AbstractMap;

public interface IController {
    void registerActor(String actorName);

    void createCopyController(IController controller);

    boolean containsActor(String actorName);

    void stopActor(String actorName);

    void startActor(String key);
}
