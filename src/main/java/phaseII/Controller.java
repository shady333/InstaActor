package phaseII;

import com.dudar.utils.services.ActorActions;
import org.apache.log4j.Logger;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Controller implements IController, Runnable {

    final static Logger logger = Logger.getLogger(Controller.class);

    private Set<IActor> registeredActors = new HashSet<>();

    @Override
    public void registerActor(String actorName) {
        ActorInsta actorInsta = new ActorInsta(actorName);
//        actorInsta.shouldRun(true);
        registeredActors.add(actorInsta);

//        Thread thread = new Thread(actorInsta);
//        thread.start();
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
                                        (((ActorInsta)item)).shouldRun(true)
                                );
                    }
                    else{
                        registeredActors.stream()
                                .filter(item -> item.getName().equals(action.getKey()))
                                .forEach(item -> ((ActorInsta)item).shouldRun(true));
                    }

                    break;
                case STOP:
                    logger.info("Received action " + ActorActions.STOP + " for Actor - " + action.getKey());
                    if(action.getKey().equals("ALL")){
                        registeredActors.forEach(item -> {
                            ((ActorInsta)item).shouldRun(false);
                        });
                    }
                    else {
                        registeredActors.stream()
                                .filter(item -> item.getName().equals(action.getKey()))
                                .forEach(item -> ((ActorInsta)item).shouldRun(false));
                    }
                    break;
                case STATUS:
                    logger.info("Received action " + ActorActions.STATUS + " for Actor - " + action.getKey());
                    if(action.getKey().equals("ALL")){
                        registeredActors.forEach(item -> {
                            ((ActorInsta)item).sendStatus();
                        });
                    }
                    else {
                        registeredActors.stream()
                                .filter(item -> item.getName().equals(action.getKey()))
                                .forEach(item -> ((ActorInsta)item).sendStatus());
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

    private boolean completed = false;

    private void startAllActors(){

        for(IActor item : registeredActors){
            (((ActorInsta) item)).shouldRun(true);
            (((ActorInsta) item)).start();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

//        Iterator<IActor> itr = registeredActors.iterator();
//
//        while(itr.hasNext()){
//            (((ActorInsta) itr)).start();
//            try {
//                TimeUnit.SECONDS.sleep(30);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//        }


//        registeredActors.stream()
////                .filter(item -> !item.isRunning())
//                .forEach(item -> {
//                            //(((ActorInsta) item)).shouldRun(true);
//                            (((ActorInsta) item)).start();
//                            try {
//                                TimeUnit.SECONDS.sleep(30);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                );

        //startAllActors();
    }

    public void startAllInterruptedActors(){
        registeredActors.stream()
                .filter(item -> !item.isRunning())
                .forEach(item -> {
                            if (((ActorInsta) item).wasInterrupted()) {
                                (((ActorInsta) item)).start();
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

    private AtomicBoolean running = new AtomicBoolean(true);

    public boolean isRunning(){
        return running.get();
    }

    public void run() {
        int i = 0;
        while(running.get()){
            startAllActors();
            i++;
            logger.info(i + " run completed.");
        }
    }
}
