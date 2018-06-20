package Storage;

import java.util.concurrent.locks.ReentrantLock;

public class Block {

    private int label;

    public Block(int label){
        this.label = label;
    }

    public int getLabel() {
        return this.label;
    }

    public void setLabel(int label) {
        this.label = label;
    }

}
