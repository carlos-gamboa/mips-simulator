package Logic;

import Controller.Simulation;

public class DualCoreManager {

    private volatile DualCore dualCore;
    private Thread thread1;
    private Thread thread2;

    public DualCoreManager (Simulation simulation) {
        this.dualCore = new DualCore(simulation);
        this.thread1 = (new Thread(new CoreThread(this.dualCore, true)));
        this.thread2 = (new Thread(new CoreThread(this.dualCore, false)));
    }

    public void start(){
        this.thread1.start();
        this.thread2.start();
    }

}
