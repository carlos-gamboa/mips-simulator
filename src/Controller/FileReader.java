package Controller;


import Storage.Instruction;

import java.io.*;
import java.util.ArrayList;

public class FileReader {

    String[] textfiles;
    ArrayList<Instruction> instructions;
    int [] threadStartingPoint;


    public FileReader(String[] textfiles) {
        this.textfiles = textfiles;
        this.instructions = new ArrayList<Instruction>();
        this.threadStartingPoint = new int[textfiles.length];
    }

    public void readThreads() throws IOException {
        for (String filename : textfiles)
        {
            try {
                FileInputStream fstream = new FileInputStream(filename);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                String instructionLine;

                while ((instructionLine = br.readLine()) != null) {
                    String[] instructionParts = instructionLine.split("\\s+");
                    Instruction i = new Instruction(Integer.parseInt(instructionParts[0]),Integer.parseInt(instructionParts[1]),Integer.parseInt(instructionParts[2]),Integer.parseInt(instructionParts[3]));
                    this.instructions.add(i);
                }
                br.close();
                System.out.print("---------EOF--------\n");

            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}