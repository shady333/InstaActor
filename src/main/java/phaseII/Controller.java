package phaseII;

import com.dudar.runner.Runner;
import com.dudar.utils.services.ActorActions;
import org.apache.log4j.Logger;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Set;

public class Controller implements IController {

    final static Logger logger = Logger.getLogger(Runner.class);

    private Set<IActor> registeredActors = new HashSet<>();

    @Override
    public void registerActor(String actorName) {
        ActorInsta actorInsta = new ActorInsta(actorName);
        registeredActors.add(actorInsta);
        Thread thread = new Thread(actorInsta);
        thread.start();

    }

    @Override
    public void proceedAction(AbstractMap.SimpleEntry<String, ActorActions> action) {
        if(action.getValue() != ActorActions.UNDEFINED){
            switch (action.getValue()){
                case START:
                    logger.info("Received action " + ActorActions.START + " for Actor - " + action.getKey());
                    if(action.getKey().equals("ALL")){
                        registeredActors.stream()
                                .filter(item -> !item.isRunning())
                                .forEach(item ->
                                        (new Thread((ActorInsta)item)).start()
                                );
                    }
                    else{
                        registeredActors.stream()
                                .filter(item -> item.getName().equals(action.getKey()))
                                .forEach(item -> (new Thread((ActorInsta)item)).start());
                    }

                    break;
                case STOP:
                    logger.info("Received action " + ActorActions.STOP + " for Actor - " + action.getKey());
                    if(action.getKey().equals("ALL")){
                        registeredActors.forEach(item -> {
                            (item).stop();
                        });
                    }
                    else {
                        registeredActors.stream()
                                .filter(item -> item.getName().equals(action.getKey()))
                                .forEach(item -> item.stop());
                    }
                    break;
            }
        }
    }

    @Override
    public void createCopyController(IController controller) {

    }

    public void getStatusOfActors(){
        for(IActor actor : registeredActors){
            logger.info(actor.getName() + " : " + actor.getStatus());
        }
    }
}
