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
            this.simulation.addInstructionsToMemory(this.fileReader.getInstructions());
            this.simulation.setContexts(this.fileReader.getThreadStartingPoint(), this.fileReader.getTextfiles());
            this.simulation.start();
            this.printCurrentStatus();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printCurrentStatus(){
        System.out.println(this.simulation.getContextsString() + "\n");
        System.out.println("--- Nucleo 0 ---");
        System.out.println(this.simulation.getDualCore().getDataCache().toString());
        System.out.println("--- Fin de Nucleo 0 ---\n");
        System.out.println("--- Nucleo 1 ---");
        System.out.println(this.simulation.getSimpleCore().getDataCache().toString());
        System.out.println("--- Fin de Nucleo 1 ---\n");
        System.out.println(this.simulation.getMainMemory().toString());
    }




}
