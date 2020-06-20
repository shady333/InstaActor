package phaseII;

import com.dudar.utils.services.ActorActions;
import org.apache.log4j.Logger;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Controller implements IController, Runnable {

    final static Logger logger = Logger.getLogger(Controller.class);

    private Set<IActor> registeredActors = new HashSet<>();

    public String getStatusForAll(){
        final String[] status = {""};
        registeredActors.forEach(item -> {
            status[0] += item.getName() + "\nActivated : " + item.isEnabled() + ";\n"
            + "Is Running now: " + item.isActive() + "\n";
        });
        return status[0];
    }

    @Override
    public void registerActor(String actorName) {
        ActorInsta actorInsta = new ActorInsta(actorName);
        registeredActors.add(actorInsta);
    }

    @Override
    public void createCopyController(IController controller) {
        ;
    }

    @Override
    public boolean containsActor(String actorName) {
        long result = registeredActors.stream().filter(item -> item.getName().equals(actorName)).count();
        registeredActors.stream().close();
        return result > 0 ? true : false;
    }

    @Override
    public void stopActor(String actorName) {
        registeredActors.stream().filter(item -> item.getName().equals(actorName))
                .forEach(item -> ((ActorInsta)item).shouldRun(false));
    }

    @Override
    public void startActor(String actorName) {
        registeredActors.stream().filter(item -> item.getName().equals(actorName))
                .forEach(item -> ((ActorInsta)item).shouldRun(true));
    }

    public void stopAllActors(){
        registeredActors.forEach(item -> {
            (item).stop();
        });
    }

    private boolean completed = false;

    public void startAllActors(){

        registeredActors.forEach(item -> {
            item.activate();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        for(IActor item : registeredActors){
            (((ActorInsta) item)).start();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private AtomicBoolean running = new AtomicBoolean(true);

    public void run() {
        int i = 0;
        while(running.get()){
            startAllActors();
            i++;
            logger.info(i + " run completed.");
        }
    }
}
