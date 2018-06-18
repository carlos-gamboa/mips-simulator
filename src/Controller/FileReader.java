package Controller;


import Storage.Context;
import Storage.Instruction;
import Storage.InstructionBlock;
import Storage.MainMemory;

import java.io.*;
import java.util.ArrayList;
import java.util.Deque;

public class FileReader {

    String[] textfiles;
    ArrayList<Instruction> instructions;
    int [] threadStartingPoint;
    int instructionNumber;
    int fileNumber;


    public FileReader(String[] textfiles) {

        this.textfiles = textfiles;
        this.instructions = new ArrayList<Instruction>();
        this.threadStartingPoint = new int[textfiles.length];
        this.instructionNumber = 0;
        this.fileNumber = 0;

    }

    public void readThreads() throws IOException {

        this.threadStartingPoint[0]=0;
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
        /*for (int i = 0; i < instructions.size() ;  i++) {
            System.out.println("INSTRUCTIONS");
            System.out.println(instructions.get(i).getOperationCode());
            System.out.println(instructions.get(i).getSourceRegister());
            System.out.println(instructions.get(i).getDestinyRegister());
            System.out.println(instructions.get(i).getImmediate());
        }*/
    }



    public void addInstructionsToMemory(MainMemory memory){

        int blockNumber = 0;
        int instructionNumber = 1;
        InstructionBlock[] instructionBlocks = new InstructionBlock[40];
        InstructionBlock block = new InstructionBlock();
        Instruction instruction;
        boolean isWritingBlock = false;

        for (int i = 0; i < instructions.size() ;  i++) {

            instruction = instructions.get(i);

/*            System.out.println("I = "+i);
            System.out.println("INSTRUCTIONS");
            System.out.println(instruction.getOperationCode());
            System.out.println(instruction.getSourceRegister());
            System.out.println(instruction.getDestinyRegister());
            System.out.println(instruction.getImmediate());*/
            //System.out.println(instructionNumber +  "   " + blockNumber);

            if (instructionNumber == 4){ //If it is the last instruction in the block, change block and add block to array of blocks
                isWritingBlock = false;
                block.setValue(instructionNumber - 1, instruction);
                instructionBlocks[blockNumber] = block;
                blockNumber++;
                instructionNumber = 1;
                block = new InstructionBlock();
            }
            else{ //If it is any other instruction on the block, add it and add to the insturction number counter
                isWritingBlock = true;
                block.setValue(instructionNumber - 1, instruction);
                instructionNumber++;
            }
        }
        if (isWritingBlock){ //If one block was being written and interations runs out of instructions it writes the unfinished block
            instructionBlocks[blockNumber] = block;
        }

/*        System.out.println("MEMORY INSTRUCTION BLOCKS");
        for (int i = 0; i < instructionBlocks.length ;  i++) {
            for (int j = 0; j < 4; j++) {
                System.out.println("I = "+i +"J = "+j);
                System.out.println(instructionBlocks[i].getValue(j).getOperationCode());
                System.out.println(instructionBlocks[i].getValue(j).getSourceRegister());
                System.out.println(instructionBlocks[i].getValue(j).getDestinyRegister());
                System.out.println(instructionBlocks[i].getValue(j).getImmediate());
                System.out.println("---end instruction---");
            }
            System.out.println("---end BLOCK---");
        }*/
        memory.setInstructionBlocks(instructionBlocks);
    }

    public void addContextsToSimulation(Deque<Context> threadQueue){

        //We calculate the corresponding adress in memory by using the
        //equation address = 384 + (instructionNumber * 4)
        //384 being the byte in memory where instructions begin and 4 being the size in memory of an instruction
        Context context;
        System.out.println("Contexts");

        for (int i = 0; i < this.threadStartingPoint.length; i++){
            context = new Context();
            int memoryAddress = 384 + (this.threadStartingPoint[i] * 4);
            context.setPc(memoryAddress);
            threadQueue.addLast(context);
            System.out.println(memoryAddress);
        }
    }

    public void printInstructionsStartingPoints(){

        for(int i = 0; i < threadStartingPoint.length ; i++){
            System.out.println(threadStartingPoint[i]);
        }
    }

    public void printMemoryBlocks(InstructionBlock[] memoryInstructionBlocks){
        System.out.println("MEMORY INSTRUCTION BLOCKS");
        for (int i = 0; i < memoryInstructionBlocks.length ;  i++){
            for (int j = 0; j<4 ; j++){
                System.out.println(memoryInstructionBlocks[i].getValue(j).getOperationCode());
                System.out.println(memoryInstructionBlocks[i].getValue(j).getSourceRegister());
                System.out.println(memoryInstructionBlocks[i].getValue(j).getDestinyRegister());
                System.out.println(memoryInstructionBlocks[i].getValue(j).getImmediate());
                System.out.println("---end instruction---");
            }
            System.out.println("---end BLOCK---");
        }

    }


}