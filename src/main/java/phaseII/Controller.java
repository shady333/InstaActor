package phaseII;

import com.dudar.utils.services.ActorActions;
import com.dudar.utils.services.EmailService;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Controller implements IController, Runnable {

    private String name;

    public Controller(String name){
        this.name = name;
    }

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

    public void deactivateAllActors(){
        registeredActors.forEach(item -> {
            (item).deactivate();
        });
    }

    public void activateAllActors(){
        registeredActors.forEach(item -> {
            (item).activate();
        });
    }

    private boolean completed = false;

    public void startAllActors(){

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

    public void proceedAction(AbstractMap.SimpleEntry<String, ActorActions> currentAction) {
        switch (currentAction.getValue()){
            case STATUS:
                String statusMessage = name + " STATUS for ALL\n";
                statusMessage += getStatusForAll();
                EmailService.generateAndSendEmail(statusMessage);
                break;
            case STOP:
                if(currentAction.getKey().equals("ALL")){
                    deactivateAllActors();
                }
                else if(containsActor(currentAction.getKey())){
                    stopActor(currentAction.getKey());
                }

                break;
            case START:
                if(currentAction.getKey().equals("ALL")){
                    activateAllActors();
                }
                else if(containsActor(currentAction.getKey())){
                    startActor(currentAction.getKey());
                }
                break;
            case DOWNLOAD:
                String propFilePath = "data/" + currentAction.getKey() + "_user.properties";
                EmailService.generateAndSendEmail(currentAction.getKey() + " PROPERTIES FILE", propFilePath);
                break;
            case UPLOAD:
                try {
                    Path from = Paths.get("tmp/" + currentAction.getKey() + "_user.properties"); //convert from File to Path
                    Path to = Paths.get("data/" + currentAction.getKey() + "_user.properties"); //convert from String to Path
                    Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
                    EmailService.generateAndSendEmail(currentAction.getKey() + "Properties file updated");
                }
                catch (IOException ex){
                    logger.error("Can't replace properties file\n" + ex.getMessage());
                }
                break;
            default:
                break;
        }
    }
}
