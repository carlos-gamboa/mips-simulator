package Storage;

public class MainMemory {

    private DataBlock[] dataBlocks;
    private InstructionBlock[] instructionBlocks;

    public MainMemory () {
        this.dataBlocks = new DataBlock[24];
        this.instructionBlocks = new InstructionBlock[40];
    }

    public Block getBlock(int i) {
        if (i < 24){
            return dataBlocks[i];
        }
        else {
            return instructionBlocks[i - 24];
        }
    }

    public int getBlockLabelByAddress(int address){
        //TODO: Calculate block label by address.
        return 1;
    }

    public int getBlockWordByAddress(int address){
        //TODO: Calculate block label by address.
        return 1;
    }

    public void setDataBlock(DataBlock block, int i){
        dataBlocks[i] = block;
    }

    public void setInstructionBlock(InstructionBlock block, int i){
        instructionBlocks[i - 24] = block;
    }

}
