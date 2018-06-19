package Storage;

public class InstructionCache {

    private InstructionBlock[] blocks;

    public InstructionCache (int numberOfBlocks) {
        this.blocks = new InstructionBlock[numberOfBlocks];
        for (int i = 0; i < numberOfBlocks; ++i){
            this.blocks[i] = new InstructionBlock();
        }
    }

    public InstructionBlock getBlock(int i) {
        return blocks[i];
    }

    public void setBlock(InstructionBlock block, int i) {
        this.blocks[i] = block;
    }

    public boolean hasBlock(int label){
        return (this.blocks[this.calculateIndexByLabel(label)].getLabel() == label);
    }

    public int calculateIndexByLabel(int label){
        return label % blocks.length;
    }

}
