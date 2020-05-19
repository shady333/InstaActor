package phaseII;

import com.dudar.runner.Runner;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActorInsta implements IActor, Runnable {

    private String name;

    private AtomicBoolean running = new AtomicBoolean(false);
    final static Logger logger = Logger.getLogger(Runner.class);

    public ActorInsta(String name){
        this.name = name;
    }

    @Override
    public String getStatus() {
        return String.valueOf(running.get());
    }

    @Override
    public void stop() {
        running.set(false);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void run() {
        running.set(true);
        while (running.get()) {
            logger.info("Running: " + this.name);

            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.error("Can't sleep\n" + e.getMessage());
            }
        }
    }
}
