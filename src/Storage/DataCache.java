package Storage;

public class DataCache {

    private DataBlock[] blocks;

    public DataCache (int numberOfBlocks) {
        this.blocks = new DataBlock[numberOfBlocks];
    }

    public DataBlock getBlock(int i) {
        return blocks[i];
    }

    public void setBlock(DataBlock block, int i) {
        this.blocks[i] = block;
    }

    public boolean hasBlock(int label){
        return (this.blocks[this.calculateIndexByLabel(label)].getLabel() == label);
    }

    public int calculateIndexByLabel(int label){
        return label % blocks.length;
    }
}
