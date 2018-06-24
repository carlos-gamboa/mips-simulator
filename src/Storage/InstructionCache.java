package Storage;

import java.util.concurrent.locks.ReentrantLock;

public class InstructionCache {

    private InstructionBlock[] blocks;
    private ReentrantLock[] locks;

    public InstructionCache (int numberOfBlocks) {
        this.blocks = new InstructionBlock[numberOfBlocks];
        this.locks = new ReentrantLock[numberOfBlocks];
        for (int i = 0; i < numberOfBlocks; ++i){
            this.blocks[i] = new InstructionBlock(-1);
            this.locks[i] = new ReentrantLock();
        }
    }

    public ReentrantLock getLock(int i) {
        return locks[i];
    }

    public InstructionBlock getBlock(int i) {
        return blocks[i];
    }

    public void setBlock(InstructionBlock block, int i) {
        this.blocks[i] = block;
    }

    /**
     * Checks if the cache has a block with the label
     *
     * @param label Label of the block
     * @return True | False
     */
    public boolean hasBlock(int label){
        return (this.blocks[this.calculateIndexByLabel(label)].getLabel() == label);
    }

    /**
     * Calculates the index of the block in the cache by it's label
     *
     * @param label Label of the block
     * @return Index of the block in the cache
     */
    public int calculateIndexByLabel(int label){
        return label % blocks.length;
    }

    public String toString(){
        String cache = "--- CACHE DE INSTRUCCIONES ---\n";
        for (int i = 0; i < this.blocks.length; ++i){
            cache += this.blocks[i].toString();
            cache += "\n";
        }
        cache += "--- FIN DE CACHE DE INSTRUCCIONES ---";
        return cache;
    }

}
