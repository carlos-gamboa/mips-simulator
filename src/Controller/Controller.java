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

    /**
     * Starts the program
     */
    public void start(){
        try {
            this.simulation.setQuantum(this.terminal.getQuantum());
            this.simulation.setSlowMode(this.terminal.getSimulationMode());
            this.fileReader.readThreads();
            this.simulation.addInstructionsToMemory(this.fileReader.getInstructions());
            this.simulation.setContexts(this.fileReader.getThreadStartingPoint(), this.fileReader.getTextfiles());
            this.simulation.start();
            this.printCurrentStatus();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the current status of the contexts, cores, and memory.
     */
    public void printCurrentStatus(){
        this.simulation.printCurrentStatus();
    }




}
