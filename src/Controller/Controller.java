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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
