package Logic;

import Controller.Simulation;

public class SimpleCoreManager {

    private volatile SimpleCore core;
    private Thread thread;

    public SimpleCoreManager (Simulation simulation) {
        this.core = new SimpleCore(simulation);
        this.thread = (new Thread(new SimpleCoreThread(this.core)));
    }

    public void start(){
        this.thread.start();
    }

}
