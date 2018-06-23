package Controller;

import Logic.DualCore;
import Logic.SimpleCore;
import Storage.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.concurrent.BrokenBarrierException;
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
        this.barrier = new CyclicBarrier(3);
    }

    public void start(){
        this.dualCore = new DualCore(this, this.quantum);
        this.simpleCore = new SimpleCore(this, this.quantum);
        this.dualCore.start();
        //this.simpleCore.start();
        while (this.dualCore.isRunning){
            ++this.clock;
            try {
                this.barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
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
        return !this.threadQueue.isEmpty();
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

    public void addInstructionsToMemory(ArrayList<Instruction> instructions){

        int blockNumber = 0;
        int instructionNumber = 0;
        InstructionBlock[] instructionBlocks = new InstructionBlock[40];
        InstructionBlock block = new InstructionBlock(blockNumber + 24);
        Instruction instruction;
        boolean isWritingBlock = false;

        for (int i = 0; i < instructions.size() ;  i++) {

            instruction = instructions.get(i);

            if (instructionNumber == 3){ //If it is the last instruction in the block, change block and add block to array of blocks
                isWritingBlock = false;
                block.setValue(instructionNumber, instruction);
                instructionBlocks[blockNumber] = block;
                blockNumber++;
                instructionNumber = 0;
                block = new InstructionBlock(blockNumber + 24);
            }
            else{ //If it is any other instruction on the block, add it and add to the instruction number counter
                isWritingBlock = true;
                block.setValue(instructionNumber, instruction);
                instructionNumber++;
            }
        }
        if (isWritingBlock){ //If one block was being written and interations runs out of instructions it writes the unfinished block
            for (int i = instructionNumber; i < 4; ++i){
                block.setValue(i, new Instruction(0,0,0,0));
            }
            instructionBlocks[blockNumber] = block;
            ++blockNumber;
        }
        for (int i = blockNumber; i < 40; ++i){
            instructionBlocks[i] = new InstructionBlock(i + 24);
        }
        this.mainMemory.setInstructionBlocks(instructionBlocks);
    }

    public void setContexts(int [] threadStartingPoint){

        //We calculate the corresponding adress in memory by using the
        //equation address = 384 + (instructionNumber * 4)
        //384 being the byte in memory where instructions begin and 4 being the size in memory of an instruction
        Context context;

        for (int i = 0; i < threadStartingPoint.length; i++){
            context = new Context();
            int memoryAddress = 384 + (threadStartingPoint[i] * 4);
            context.setPc(memoryAddress);
            threadQueue.addLast(context);
        }
    }
}
