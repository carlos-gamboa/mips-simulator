package Controller;

import java.io.IOException;

public class Controller {

    FileReader fileReader;
    Simulation simulation;
    Terminal terminal;

    public Controller(String[] arguments) {
        this.fileReader = new FileReader(arguments);
        this.simulation = new Simulation();
        this.terminal = new Terminal();
    }

    public void start(){
        try {
            this.simulation.setQuantum(this.terminal.getQuantum());
            this.simulation.setSlowMode(this.terminal.getSimulationMode());
            this.fileReader.readThreads();
            this.fileReader.printInstructionsStartingPoints();
            this.fileReader.addInstructionsToMemory(this.simulation.getMainMemory());
            this.fileReader.addContextsToSimulation(this.simulation.getThreadQueue());
            this.fileReader.printMemoryBlocks(this.simulation.getMainMemory().getInstructionBlocks());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
