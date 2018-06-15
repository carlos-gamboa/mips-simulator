package Controller;

import java.io.IOException;

public class Controller {

    FileReader fileReader;
    Simulation simulation;

    public Controller(String[] arguments) {
        this.fileReader = new FileReader(arguments);
        this.simulation = new Simulation();
    }

    public void start(){
        try {
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
