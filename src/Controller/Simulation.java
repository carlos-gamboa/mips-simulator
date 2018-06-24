package Controller;

import Logic.DualCore;
import Logic.SimpleCore;
import Storage.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
        this.barrier = new CyclicBarrier(4);
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

    public DualCore getDualCore() {
        return dualCore;
    }

    public void setDualCore(DualCore dualCore) {
        this.dualCore = dualCore;
    }

    public SimpleCore getSimpleCore() {
        return simpleCore;
    }

    public void setSimpleCore(SimpleCore simpleCore) {
        this.simpleCore = simpleCore;
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

        if (!slowMode){
            this.barrier = new CyclicBarrier(3);
        }
        else{
            this.barrier = new CyclicBarrier(4); //Hay una barrera más que es la del key listener
        }
    }

    /**
     * Starts the simulation
     */
    public void start(){
        this.dualCore = new DualCore(this, this.quantum);
        this.simpleCore = new SimpleCore(this, this.quantum);
        this.simpleCore.start();
        this.dualCore.start();

        while (this.dualCore.isRunning() || this.simpleCore.isRunning()){
            System.out.println(this.getCurrentThreads());
            ++this.clock;
            if(slowMode && this.clock % 20 == 0){
                this.printCurrentStatus();
                System.out.println("\nPresione Enter para continuar");
                try
                {
                    System.in.read();
                }
                catch(Exception e) {

                }
            }
            this.tickBarrier();
        }
        System.out.println("salio");
    }

    /**
     * Awaits at the barrier
     */
    private void tickBarrier(){
        try {
            this.barrier.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        } catch (TimeoutException e){
        }
    }

    /**
     * Print the status of the storage units
     */
    public void printCurrentStatus(){
        System.out.println(this.getContextsString() + "\n");
        System.out.println("--- Nucleo 0 ---");
        System.out.println(this.getDualCore().getDataCache().toString());
        System.out.println("--- Fin de Nucleo 0 ---\n");
        System.out.println("--- Nucleo 1 ---");
        System.out.println(this.getSimpleCore().getDataCache().toString());
        System.out.println("--- Fin de Nucleo 1 ---\n");
        System.out.println(this.getMainMemory().toString());
    }

    /**
     * Stores all the instructions to the main memory
     *
     * @param instructions ArrayList with all the instructions
     */
    public synchronized void addInstructionsToMemory(ArrayList<Instruction> instructions){

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

    /**
     * Adds the contexts of the threads to the queue
     *
     * @param threadStartingPoint PC of each thread
     * @param threadNames Name of each thread
     */
    public synchronized void setContexts(int [] threadStartingPoint, String[] threadNames){

        //We calculate the corresponding adress in memory by using the
        //equation address = 384 + (instructionNumber * 4)
        //384 being the byte in memory where instructions begin and 4 being the size in memory of an instruction
        Context context;

        for (int i = 0; i < threadStartingPoint.length; i++){
            context = new Context();
            int memoryAddress = 384 + (threadStartingPoint[i] * 4);
            context.setPc(memoryAddress);
            context.setThreadName(threadNames[i]);
            threadQueue.addLast(context);
        }
    }

    /**
     * Gets data from the cache of the other core
     *
     * @param isSimpleCore If the core only has 1 thread
     * @param blockLabel Label of the block to be returned
     * @return DataBlock of the desired label
     */
    public synchronized DataBlock getDataBlockFromOtherCache(boolean isSimpleCore, int blockLabel){
        if (isSimpleCore){
            return this.dualCore.getDataCache().getBlock(this.dualCore.getDataCache().calculateIndexByLabel(blockLabel));
        }
        else {
            return this.simpleCore.getDataCache().getBlock(this.simpleCore.getDataCache().calculateIndexByLabel(blockLabel));
        }
    }

    /**
     * Checks if the cache of the other core contains a certain block
     *
     * @param isSimpleCore If the core only has 1 thread
     * @param blockLabel Label of the block you want to check
     * @return True if the cache has the block
     */
    public synchronized boolean checkDataBlockOnOtherCache(boolean isSimpleCore, int blockLabel){
        if (isSimpleCore){
            return this.dualCore.getDataCache().hasBlock(blockLabel);
        }
        else {
            return this.simpleCore.getDataCache().hasBlock(blockLabel);
        }
    }

    /**
     * Changes the cache status of a block in the cache of the other core
     *
     * @param isSimpleCore If the core only has 1 thread
     * @param blockLabel Label of the block you want to change the status
     * @param status New status for the block
     */
    public synchronized void changeDataBlockStatusFromOtherCache(boolean isSimpleCore, int blockLabel, CacheStatus status){
        if (isSimpleCore){
            this.dualCore.getDataCache().getBlock(blockLabel).setBlockStatus(status);
        }
        else {
            this.simpleCore.getDataCache().getBlock(blockLabel).setBlockStatus(status);
        }
    }

    /**
     * Copies a data block to main memory
     *
     * @param block Block you want to copy
     * @param label Label of the block
     */
    public synchronized void saveDataBlockToMainMemory(DataBlock block, int label){
        this.mainMemory.setDataBlock(block, label);
    }

    public synchronized void invalidateBlockOnOtherCache(boolean isSimpleCore, int blockLabel){
        if (isSimpleCore){
            this.dualCore.getDataCache().getBlock(blockLabel).setBlockStatus(CacheStatus.Invalid);
        }
        else {
            this.simpleCore.getDataCache().getBlock(blockLabel).setBlockStatus(CacheStatus.Invalid);
        }
    }

    /**
     * Checks if the other core is still running
     *
     * @param isSimpleCore If the core only has 1 thread
     * @return True if the other core is still running
     */
    public synchronized boolean isOtherCoreRunning(boolean isSimpleCore){
        if (isSimpleCore){
            return this.dualCore.isRunning();
        }
        else {
            return this.simpleCore.isRunning();
        }
    }

    /**
     * Returns the context of the threads as string
     *
     * @return String containing the contexts
     */
    public String getContextsString(){
        String contexts = "";
        while (!this.finishedThreads.isEmpty()){
            contexts += this.finishedThreads.pop().toString();
        }
        return contexts;
    }

    /**
     * Gets the data of the current cycle
     *
     * @return String
     */
    public String getCurrentThreads(){
        String result = "Ciclo: " + this.clock + "\n";
        result += "Nucleo 0, hilo 0: " + this.dualCore.getThread1Name() + "\n";
        result += "Nucleo 0, hilo 1: " + this.dualCore.getThread2Name() + "\n";
        result += "Nucleo 1: " + this.simpleCore.getThreadName() + "\n";
        return result;
    }
}
