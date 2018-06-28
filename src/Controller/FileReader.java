package Controller;


import Storage.Context;
import Storage.Instruction;
import Storage.InstructionBlock;
import Storage.MainMemory;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;

public class FileReader {

    private String[] textfiles;
    private ArrayList<Instruction> instructions;
    private int [] threadStartingPoint;
    private int instructionNumber;
    private int fileNumber;


    public FileReader() {

        //this.textfiles = textfiles;
        this.instructions = new ArrayList<Instruction>();
        //this.threadStartingPoint = new int[textfiles.length];
        this.instructionNumber = 0;
        this.fileNumber = 0;

    }

    public String[] getTextfiles() {
        return textfiles;
    }

    public void setTextfiles(String[] textfiles) {
        this.textfiles = textfiles;
    }

    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }

    public void setInstructions(ArrayList<Instruction> instructions) {
        this.instructions = instructions;
    }

    public int[] getThreadStartingPoint() {
        return threadStartingPoint;
    }

    public void setThreadStartingPoint(int[] threadStartingPoint) {
        this.threadStartingPoint = threadStartingPoint;
    }

    public int getInstructionNumber() {
        return instructionNumber;
    }

    public void setInstructionNumber(int instructionNumber) {
        this.instructionNumber = instructionNumber;
    }

    public int getFileNumber() {
        return fileNumber;
    }

    public void setFileNumber(int fileNumber) {
        this.fileNumber = fileNumber;
    }

    public void findFiles(){

        File file = new File(".");

        FilenameFilter filter = new FilenameFilter(){
            public boolean accept(File dir, String fileName) {
                return fileName.endsWith("txt");
            }
        };


        this.textfiles = file.list(filter);
        if(this.textfiles != null){
            this.threadStartingPoint = new int[this.textfiles.length];
        }
        else {
            System.out.println("No se encontraron archivos.");
        }
    }

    /**
     * Reads the thread files and stores the instructions as well as the PC.
     *
     * @throws IOException
     */
    public void readThreads() throws IOException {
        this.findFiles();
        this.threadStartingPoint[0]=0;
        for (String filename : this.textfiles)
        {
            try {
                FileInputStream fstream = new FileInputStream(filename);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                String instructionLine;

                while ((instructionLine = br.readLine()) != null) {
                    String[] instructionParts = instructionLine.split("\\s+");
                    Instruction i = new Instruction(Integer.parseInt(instructionParts[0]),Integer.parseInt(instructionParts[1]),Integer.parseInt(instructionParts[2]),Integer.parseInt(instructionParts[3]));
                    this.instructions.add(i);
                    this.instructionNumber++;
                }

                br.close();

                this.fileNumber++;
                if(this.fileNumber != this.textfiles.length) { //Write where all files end as starting points for the next one
                    this.threadStartingPoint[this.fileNumber] = this.instructionNumber;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}