package Controller;

import Logic.DualCore;
import Logic.SimpleCore;
import Storage.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

public class Simulation {

    private Deque<Context> threadQueue;
    private Deque<Context> finishedThreads;
    private FileReader fileReader;

    private CyclicBarrier barrier;
    private ReentrantLock dataBus;
    private ReentrantLock instructionsBus;
    private MainMemory mainMemory;

    private DualCore dualCore;
    private SimpleCore simpleCore;

    private int clock;
    private int quantum;
    private boolean slowMode;

    public Simulation(){
        this.clock = 0;
        this.quantum = 0;
        this.slowMode = false;
        this.mainMemory = new MainMemory();
        this.dataBus = new ReentrantLock();
        this.instructionsBus = new ReentrantLock();
        this.threadQueue = new ArrayDeque<>();
        this.finishedThreads = new ArrayDeque<>();
    }

    public void start(){
        this.dualCore = new DualCore(this, this.quantum);
        this.simpleCore = new SimpleCore(this, this.quantum);
    }

    public CyclicBarrier getBarrier() {
        return this.barrier;
    }

    public synchronized Context getNextContext(){
        return this.threadQueue.pop();
    }

    public synchronized void addContext(Context context){
        this.threadQueue.push(context);
    }

    public synchronized void addFinishedContext(Context context){
        this.finishedThreads.push(context);
    }

    public MainMemory getMainMemory() {
        return this.mainMemory;
    }

    public void setMainMemory(MainMemory mainMemory) {
        this.mainMemory = mainMemory;
    }

    public ReentrantLock getDataBus() {
        return this.dataBus;
    }

    public void setDataBus(ReentrantLock dataBus) {
        this.dataBus = dataBus;
    }

    public ReentrantLock getInstructionsBus() {
        return this.instructionsBus;
    }

    public void setInstructionsBus(ReentrantLock instructionsBus) {
        this.instructionsBus = instructionsBus;
    }

    public boolean tryLockInstructionsCacheBlock(){
        return true;
    }

    public void unlockInstructionsCacheBlock(){
    }

    public boolean areMoreContexts(){
        return this.threadQueue.isEmpty();
    }

    public boolean tryLockDataCacheBlock(boolean isSimpleCore, int blockLabel){
        return true;
    }

    public void unlockDataCacheBlock(boolean isSimpleCore, int blockLabel){
    }
    
    public Deque<Context> getThreadQueue() {
        return threadQueue;
    }

    public void setThreadQueue(Deque<Context> threadQueue) {
        this.threadQueue = threadQueue;
    }

    public DataBlock getDataBlockFromOtherCache(boolean isSimpleCore, int blockLabel){
        if (isSimpleCore){
            return this.dualCore.getDataCache().getBlock(blockLabel);
        }
        else {
            return this.simpleCore.getDataCache().getBlock(blockLabel);
        }
    }

    public boolean checkDataBlockOnOtherCache(boolean isSimpleCore, int blockLabel){
        if (isSimpleCore){
            return this.dualCore.getDataCache().hasBlock(blockLabel);
        }
        else {
            return this.simpleCore.getDataCache().hasBlock(blockLabel);
        }
    }

    public void changeDataBlockStatusFromOtherCache(boolean isSimpleCore, int blockLabel, CacheStatus status){
        if (isSimpleCore){
            this.dualCore.getDataCache().getBlock(blockLabel).setBlockStatus(status);
        }
        else {
            this.simpleCore.getDataCache().getBlock(blockLabel).setBlockStatus(status);
        }
    }

    public void saveDataBlockToMainMemory(DataBlock block, int label){
        this.mainMemory.setDataBlock(block, label);
    }

    public void invalidateBlockOnOtherCache(boolean isSimpleCore, int blockLabel){
        if (isSimpleCore){
            this.dualCore.getDataCache().getBlock(blockLabel).setBlockStatus(CacheStatus.Invalid);
        }
        else {
            this.simpleCore.getDataCache().getBlock(blockLabel).setBlockStatus(CacheStatus.Invalid);
        }
    }

    public int getQuantum() {
        return quantum;
    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }

    public boolean isSlowMode() {
        return slowMode;
    }

    public void setSlowMode(boolean slowMode) {
        this.slowMode = slowMode;
    }
}
