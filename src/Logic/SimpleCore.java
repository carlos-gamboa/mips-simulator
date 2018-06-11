package Logic;

import Controller.Simulation;
import Storage.Context;

public class SimpleCore extends Core {

    private Context currentThread;

    public SimpleCore(Simulation simulation){
        super(simulation, 4, true);
        this.currentThread = super.getSimulation().getNextContext();
    }

}
