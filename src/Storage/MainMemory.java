package Storage;

public class MainMemory {

    private DataBlock[] dataBlocks;
    private InstructionBlock[] instructionBlocks;

    public MainMemory () {
        this.dataBlocks = new DataBlock[24];
        this.instructionBlocks = new InstructionBlock[40];
    }

    public DataBlock getDataBlock(int i) {
        return dataBlocks[i];
    }

    public InstructionBlock getInstructionBlock(int i) {
        return instructionBlocks[i - 24];
    }

    public int getBlockLabelByAddress(int address){
        return address / (16);
    }

    public int getBlockWordByAddress(int address){
        return (address % 16)/4;
    }

    public void setDataBlock(DataBlock block, int i){
        dataBlocks[i] = block;
    }

    public void setInstructionBlock(InstructionBlock block, int i){
        instructionBlocks[i - 24] = block;
    }

}
