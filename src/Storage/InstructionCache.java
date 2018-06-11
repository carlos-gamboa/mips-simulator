package Storage;

public class InstructionCache {

    private InstructionBlock[] blocks;

    public InstructionCache (int numberOfBlocks) {
        this.blocks = new InstructionBlock[numberOfBlocks];
    }

    public InstructionBlock getBlock(int i) {
        return blocks[i];
    }

    public boolean hasBlock(int label){
        return (this.blocks[this.calculateIndexByLabel(label)].getLabel() == label);
    }

    private int calculateIndexByLabel(int label){
        //TODO: Calculate index of the block by it's label
        return 0;
    }

}
