package phaseII;

import com.dudar.utils.services.ActorActions;
import org.apache.log4j.Logger;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Controller implements IController {

    final static Logger logger = Logger.getLogger(Controller.class);

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
                        AtomicBoolean registered = new AtomicBoolean(false);
                        registeredActors.forEach(item -> {
                            if(item.getName().equals(action.getKey())){
                                registered.set(true);
                            }
                        });
                        if(!registered.get())
                        {
                            registerActor(action.getKey());
                        }
                        else{
                            //TODO make actor running
//                            registeredActors.stream()
//                                    .filter(item -> !item.isRunning())
//                                    .forEach(item ->
//                                            (new Thread((ActorInsta)item)).start());
                        }
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

    public void stopAllActors(){
        registeredActors.forEach(item -> {
            (item).stop();
        });
    }

    public void startAllActors(){
        registeredActors.stream()
                .filter(item -> !item.isRunning())
                .forEach(item -> {
                            (new Thread((ActorInsta) item)).start();
                            try {
                                TimeUnit.SECONDS.sleep(30);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                );
    }

    public void startAllInterruptedActors(){
        registeredActors.stream()
                .filter(item -> !item.isRunning())
                .forEach(item -> {
                            if (((ActorInsta) item).wasInterrupted()) {
                                (new Thread((ActorInsta) item)).start();
                                try {
                                    TimeUnit.SECONDS.sleep(30);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                );
    }

    public void getStatusOfActors(){
        for(IActor actor : registeredActors){
            logger.info(actor.getName() + " : " + actor.getStatus());
        }
    }
}
