package Storage;

public class DataCache {

    private DataBlock[] blocks;

    public DataCache (int numberOfBlocks) {
        this.blocks = new DataBlock[numberOfBlocks];
        for (int i = 0; i < numberOfBlocks; ++i){
            this.blocks[i] = new DataBlock(-1);
        }
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

    public String toString(){
        String cache = "--- DATA CACHE ---\n";
        for (int i = 0; i < this.blocks.length; ++i){
            cache += this.blocks[i].toString();
            cache += "\n";
        }
        cache += "--- END OF INSTRUCTIONS CACHE ---";
        return cache;
    }

}
