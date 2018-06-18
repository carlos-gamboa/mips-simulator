package Logic;

import Controller.Simulation;
import Storage.*;

import java.util.concurrent.BrokenBarrierException;

public class Core implements Runnable {

    public DataCache dataCache;
    public InstructionCache instructionCache;

    private Instruction currentInstruction;

    public Simulation simulation;

    public boolean isSimpleCore;
    public int clock;
    public boolean isRunning;

    public Core (Simulation simulation, int numberOfBlocks, boolean isSimpleCore) {
        this.simulation = simulation;
        this.dataCache = new DataCache(numberOfBlocks);
        this.instructionCache = new InstructionCache(numberOfBlocks);
        this.clock = 0;
        this.isSimpleCore = isSimpleCore;
        this.isRunning = true;
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

    public void nextCycle(){
        this.clock++;
        try {
            this.simulation.getBarrier().await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    public Instruction getInstruction(int pc){
        //TODO: Get next Instruction
        Instruction instruction = new Instruction();
        InstructionBlock block;
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(pc);
        int blockWord = this.simulation.getMainMemory().getBlockWordByAddress(pc);
        int cacheIndex = this.instructionCache.calculateIndexByLabel(blockLabel);
        if (this.instructionCache.getBlock(cacheIndex).getLock().tryLock()) {
            if (!this.instructionCache.hasBlock(cacheIndex)) { //InstructionCache Fail
                if (this.simulation.getInstructionsBus().tryLock()) {
                    this.copyFromMemoryToInstructionCache(cacheIndex);
                    for (int i = 0; i < 40; ++i) {
                        this.nextCycle();
                    }
                    this.simulation.getDataBus().unlock();
                }
                else {
                    this.instructionCache.getBlock(cacheIndex).getLock().unlock();
                    this.nextCycle();
                    //this.startOver();
                }
            }
            block = this.instructionCache.getBlock(cacheIndex);
            this.instructionCache.getBlock(cacheIndex).getLock().unlock();
            instruction = block.getValue(blockWord);
        }
        else
        {
            this.nextCycle();
            //this.startOver();
        }
        return instruction;
    }

    public void manageDADDI(Context context, int destinyRegister, int sourceRegister, int inmediate){
        context.setRegister(destinyRegister, context.getRegister(sourceRegister) + inmediate);
    }

    public void manageDADD(Context context, int destinyRegister, int sourceRegister1, int sourceRegister2){
        context.setRegister(destinyRegister, context.getRegister(sourceRegister1) + context.getRegister(sourceRegister2));
    }

    public void manageDSUB(Context context, int destinyRegister, int sourceRegister1, int sourceRegister2){
        context.setRegister(destinyRegister, context.getRegister(sourceRegister1) - context.getRegister(sourceRegister2));
    }

    public void manageDMUL(Context context, int destinyRegister, int sourceRegister1, int sourceRegister2){
        context.setRegister(destinyRegister, context.getRegister(sourceRegister1) * context.getRegister(sourceRegister2));
    }

    public void manageDDIV(Context context, int destinyRegister, int sourceRegister1, int sourceRegister2){
        context.setRegister(destinyRegister, context.getRegister(sourceRegister1) / context.getRegister(sourceRegister2));
    }

    public void manageBEQZ(Context context, int sourceRegister, int inmediate){
        if(context.getRegister(sourceRegister) == 0){
            context.setPc(context.getPc() + (4 * inmediate));
        }
    }

    public void manageBNEZ(Context context, int sourceRegister, int inmediate){
        if(context.getRegister(sourceRegister) != 0){
            context.setPc(context.getPc() + (4 * inmediate));
        }
    }

    public void manageJAL(Context context, int inmediate){
        //Copy the address of the next instruction on register 31
        context.setRegister(31, context.getPc());
        context.setPc(context.getPc() + inmediate);
    }

    public void manageJR(Context context, int sourceRegister){
        context.setPc(context.getRegister(sourceRegister));
    }

    public void copyFromMemoryToDataCache(int label){
        this.dataCache.setBlock(this.simulation.getMainMemory().getDataBlock(label), this.dataCache.calculateIndexByLabel(label));
        this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(label)).setBlockStatus(CacheStatus.Shared);
    }

    public void copyFromMemoryToInstructionCache(int label){
        this.instructionCache.setBlock(this.simulation.getMainMemory().getInstructionBlock(label), this.instructionCache.calculateIndexByLabel(label));
    }
}
