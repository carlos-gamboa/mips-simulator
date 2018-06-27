package Logic;

import Controller.Simulation;
import Storage.*;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Core implements Runnable {

    public DataCache dataCache;
    public InstructionCache instructionCache;

    private Instruction currentInstruction;

    public Simulation simulation;

    public boolean isSimpleCore;
    public int clock;
    public boolean isRunning;
    public int quantum;
    public boolean tick;

    public Core (Simulation simulation, int numberOfBlocks, boolean isSimpleCore, int quantum) {
        this.simulation = simulation;
        this.quantum = quantum;
        this.dataCache = new DataCache(numberOfBlocks);
        this.instructionCache = new InstructionCache(numberOfBlocks);
        this.clock = 0;
        this.isSimpleCore = isSimpleCore;
        this.isRunning = true;
        this.tick = false;
    }

    public void run(){

    }

    public Instruction getCurrentInstruction() {
        return currentInstruction;
    }

    public void setCurrentInstruction(Instruction currentInstruction) {
        this.currentInstruction = currentInstruction;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public DataCache getDataCache() {
        return this.dataCache;
    }

    public void setDataCache(DataCache dataCache) {
        this.dataCache = dataCache;
    }

    public InstructionCache getInstructionCache() {
        return this.instructionCache;
    }

    public void setInstructionCache(InstructionCache instructionCache) {
        this.instructionCache = instructionCache;
    }

    public Simulation getSimulation() {
        return this.simulation;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

    public int getClock() {
        return this.clock;
    }

    public void setClock(int clock) {
        this.clock = clock;
    }

    public int getQuantum() {
        return quantum;
    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }

    /**
     * Advances the core clock and waits fot the barrier
     */
    public void nextCycle(){
        if (!this.isSimpleCore){
            if (this.tick){
                this.clock++;
                this.tick = false;
            } else {
                this.tick = true;
            }
        } else {
            this.clock++;
        }
        try {
            this.simulation.getBarrier().await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
        } catch (TimeoutException e){
        }
    }

    /**
     * Gets an instruction based on the given PC
     *
     * @param pc PC of the thread.
     * @return Null | Instruction
     */
    public Instruction getInstruction(int pc){
        Instruction instruction = null;
        InstructionBlock block;
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(pc);
        int blockWord = this.simulation.getMainMemory().getBlockWordByAddress(pc);
        int cacheIndex = this.instructionCache.calculateIndexByLabel(blockLabel);
        if (this.instructionCache.getLock(cacheIndex).tryLock()) {
            if (!this.instructionCache.hasBlock(blockLabel)) { //InstructionCache Fail
                if (this.simulation.getInstructionsBus().tryLock()) {
                    for (int i = 0; i < 40; ++i) {
                        this.nextCycle();
                    }
                    this.copyFromMemoryToInstructionCache(blockLabel);
                    this.simulation.getInstructionsBus().unlock();
                    block = this.instructionCache.getBlock(cacheIndex);
                    instruction = block.getValue(blockWord);
                    this.instructionCache.getLock(cacheIndex).unlock();
                }
                else {
                    this.instructionCache.getLock(cacheIndex).unlock();
                    this.nextCycle();
                }
            } else {
                block = this.instructionCache.getBlock(cacheIndex);
                instruction = block.getValue(blockWord);
                this.instructionCache.getLock(cacheIndex).unlock();
            }
        }
        else
        {
            this.nextCycle();
        }
        return instruction;
    }

    /**
     * Stores the value of the source register + the immediate in the destiny register
     *
     * @param context Context of the thread
     * @param destinyRegister Number of the destiny register
     * @param sourceRegister Number of the source register
     * @param immediate Value of the immediate
     */
    public void manageDADDI(Context context, int destinyRegister, int sourceRegister, int immediate){
        context.setRegister(destinyRegister, context.getRegister(sourceRegister) + immediate);
        this.substractQuantum(context);
        this.nextCycle();
    }

    /**
     * Stores the value of the source register 1 + the source register 2 in the destiny register
     *
     * @param context Context of the thread
     * @param destinyRegister Number of the destiny register
     * @param sourceRegister1 Number of the first source register
     * @param sourceRegister2 Number of the second source register
     */
    public void manageDADD(Context context, int destinyRegister, int sourceRegister1, int sourceRegister2){
        context.setRegister(destinyRegister, context.getRegister(sourceRegister1) + context.getRegister(sourceRegister2));
        this.substractQuantum(context);
        this.nextCycle();
    }

    /**
     * Stores the value of the source register 1 - the source register 2 in the destiny register
     *
     * @param context Context of the thread
     * @param destinyRegister Number of the destiny register
     * @param sourceRegister1 Number of the first source register
     * @param sourceRegister2 Number of the second source register
     */
    public void manageDSUB(Context context, int destinyRegister, int sourceRegister1, int sourceRegister2){
        context.setRegister(destinyRegister, context.getRegister(sourceRegister1) - context.getRegister(sourceRegister2));
        this.substractQuantum(context);
        this.nextCycle();
    }

    /**
     * Stores the value of the source register 1 * the source register 2 in the destiny register
     *
     * @param context Context of the thread
     * @param destinyRegister Number of the destiny register
     * @param sourceRegister1 Number of the first source register
     * @param sourceRegister2 Number of the second source register
     */
    public void manageDMUL(Context context, int destinyRegister, int sourceRegister1, int sourceRegister2){
        context.setRegister(destinyRegister, context.getRegister(sourceRegister1) * context.getRegister(sourceRegister2));
        this.substractQuantum(context);
        this.nextCycle();
    }

    /**
     * Stores the value of the source register 1 / the source register 2 in the destiny register
     *
     * @param context Context of the thread
     * @param destinyRegister Number of the destiny register
     * @param sourceRegister1 Number of the first source register
     * @param sourceRegister2 Number of the second source register
     */
    public void manageDDIV(Context context, int destinyRegister, int sourceRegister1, int sourceRegister2){
        context.setRegister(destinyRegister, context.getRegister(sourceRegister1) / context.getRegister(sourceRegister2));
        this.substractQuantum(context);
        this.nextCycle();
    }

    /**
     * If the value in the source register is 0, adds 4 * the value of the immediate to the PC
     *
     * @param context Context of the thread
     * @param sourceRegister Number of the source register
     * @param immediate Value of the immediate
     */
    public void manageBEQZ(Context context, int sourceRegister, int immediate){
        if(context.getRegister(sourceRegister) == 0){
            context.setPc(context.getPc() + (4 * immediate));
        }
        this.substractQuantum(context);
        this.nextCycle();
    }

    /**
     * If the value in the source register is not 0, adds 4 * the value of the immediate to the PC
     *
     * @param context Context of the thread
     * @param sourceRegister Number of the source register
     * @param immediate Value of the immediate
     */
    public void manageBNEZ(Context context, int sourceRegister, int immediate){
        if(context.getRegister(sourceRegister) != 0){
            context.setPc(context.getPc() + (4 * immediate));
        }
        this.substractQuantum(context);
        this.nextCycle();
    }

    /**
     * Adds the value of the immediate to the PC
     *
     * @param context Context of the thread
     * @param immediate Value of the immediate
     */
    public void manageJAL(Context context, int immediate){
        //Copy the address of the next instruction on register 31
        context.setRegister(31, context.getPc());
        context.setPc(context.getPc() + immediate);
        this.substractQuantum(context);
        this.nextCycle();
    }

    /**
     * Sets the PC of a thread to the value of the source register
     *
     * @param context Context of the thread
     * @param sourceRegister Number of the source register
     */
    public void manageJR(Context context, int sourceRegister){
        context.setPc(context.getRegister(sourceRegister));
        this.substractQuantum(context);
        this.nextCycle();
    }

    /**
     * Copies a data block from main memory to the core's data cache
     *
     * @param label Label of the block to be copied
     */
    public void copyFromMemoryToDataCache(int label){
        this.dataCache.setBlock(this.simulation.getMainMemory().getDataBlock(label), this.dataCache.calculateIndexByLabel(label));
        this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(label)).setBlockStatus(CacheStatus.Shared);
    }

    /**
     * Copies a data block from main memory to the core's instruction cache
     *
     * @param label Label of the block to be copied
     */
    public void copyFromMemoryToInstructionCache(int label){
        this.instructionCache.setBlock(this.simulation.getMainMemory().getInstructionBlock(label), this.instructionCache.calculateIndexByLabel(label));
    }

    /**
     * Substract 1 cycle from the quantum
     *
     * @param context Context of the thread
     */
    public void substractQuantum(Context context){
        if (context != null){
            context.setRemainingQuantum(context.getRemainingQuantum() - 1);
        }
    }
}
