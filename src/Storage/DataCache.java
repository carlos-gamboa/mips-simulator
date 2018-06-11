package Storage;

public class DataCache {

    private DataBlock[] blocks;

    public DataCache (int numberOfBlocks) {
        this.blocks = new DataBlock[numberOfBlocks];
    }

    public DataBlock getBlock(int i) {
        return blocks[i];
    }

    public boolean hasBlock(int label){
        return (this.blocks[this.calculateIndexByLabel(label)].getLabel() == label);
    }

    private int calculateIndexByLabel(int label){
        return label % blocks.length;
    }
}
