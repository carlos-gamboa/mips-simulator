package Storage;

import java.util.concurrent.locks.ReentrantLock;

public class Block {

    private int label;
    private ReentrantLock lock;

    public Block(){
        this.label = 0;
        this.lock = new ReentrantLock();
    }

    public int getLabel() {
        return this.label;
    }

    public void setLabel(int label) {
        this.label = label;
    }

    public ReentrantLock getLock() {
        return this.lock;
    }

}
