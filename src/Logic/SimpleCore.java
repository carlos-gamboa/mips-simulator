package Logic;

import Controller.Simulation;
import Storage.CacheStatus;
import Storage.Context;
import Storage.DataBlock;
import Storage.Instruction;

public class SimpleCore extends Core {

    private volatile Context threadContext;
    private Thread thread;

    public SimpleCore(Simulation simulation, int quantum){
        super(simulation, 4, true, quantum);
        this.thread = new Thread(this, "Thread 0");
    }

    public SimpleCore(Simulation simulation, int quantum, boolean a){
        super(simulation, 4, a, quantum);
        this.thread = new Thread(this, "Thread 1");
    }

    public void start(){
        this.thread.start();
    }

    @Override
    public void run(){
        Instruction instruction;
        if (super.simulation.areMoreContexts()) {
            this.threadContext = super.simulation.getNextContext();
            this.threadContext.setRemainingQuantum(super.getQuantum());
            if (this.threadContext.getStartingCycle() == -1){
                this.threadContext.setStartingCycle(super.getClock());
            }
        }
        else {
            super.setRunning(false);
        }
        while (super.isRunning()){
            instruction = this.getInstruction(this.threadContext.getPc());
            if (instruction != null) {
                this.threadContext.setPc(this.threadContext.getPc() + 4);
                this.manageInstruction(instruction);
            }
        }
        while(super.simulation.isOtherCoreRunning(super.isSimpleCore)){
            this.nextCycle();
        }
    }

    public Context getCurrentThread() {
        return threadContext;
    }

    public void setCurrentThread(Context currentThread) {
        this.threadContext = currentThread;
    }

    /**
     * Execustes the FIN instruction
     */
    private void manageFIN (){
        this.threadContext.setFinishingCycle(super.getClock());
        super.simulation.addFinishedContext(this.threadContext);
        this.threadContext = null;
        if (!super.simulation.areMoreContexts()){
            super.setRunning(false);
        }
        else {
            this.threadContext = super.simulation.getNextContext();
            if (this.threadContext.getStartingCycle() == -1){
                this.threadContext.setStartingCycle(super.getClock());
            }
        }
        super.nextCycle();
    }

    /**
     * Executes an instruction
     *
     * @param instruction Instruction to be executed
     */
    private void manageInstruction(Instruction instruction){
        if (this.getCurrentThread().getRemainingQuantum() != 0){
            switch (instruction.getOperationCode()){
                case 8:
                    this.manageDADDI(this.threadContext, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                    break;
                case 32:
                    this.manageDADD(this.threadContext, instruction.getImmediate(), instruction.getSourceRegister(), instruction.getDestinyRegister());
                    break;
                case 34:
                    this.manageDSUB(this.threadContext, instruction.getImmediate(), instruction.getSourceRegister(), instruction.getDestinyRegister());
                    break;
                case 12:
                    this.manageDMUL(this.threadContext, instruction.getImmediate(), instruction.getSourceRegister(), instruction.getDestinyRegister());
                    break;
                case 14:
                    this.manageDDIV(this.threadContext, instruction.getImmediate(), instruction.getSourceRegister(), instruction.getDestinyRegister());
                    break;
                case 4:
                    this.manageBEQZ(this.threadContext, instruction.getSourceRegister(), instruction.getImmediate());
                    break;
                case 5:
                    this.manageBNEZ(this.threadContext, instruction.getSourceRegister(), instruction.getImmediate());
                    break;
                case 3:
                    this.manageJAL(this.threadContext, instruction.getImmediate());
                    break;
                case 2:
                    this.manageJR(this.threadContext, instruction.getSourceRegister());
                    break;
                case 35:
                    this.manageLoadWord(this.threadContext, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                    break;
                case 43:
                    this.manageStoreWord(this.threadContext, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                    break;
                case 63:
                    this.manageFIN();
                    break;
            }
        } else {
            this.manageQuantumEnd();
        }
    }

    /**
     * Manages the LW instruction
     *
     * @param context Context of the thread
     * @param destinyRegister Number of the destiny register
     * @param sourceRegister Number of the source register
     * @param immediate Value of the immediate
     */
    private void manageLoadWord(Context context, int destinyRegister, int sourceRegister, int immediate){
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(context.getRegister(sourceRegister) + immediate);
        int blockWord = this.simulation.getMainMemory().getBlockWordByAddress(context.getRegister(sourceRegister) + immediate);
        if (this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).tryLock()){
            if (this.dataCache.hasBlock(blockLabel)){
                CacheStatus blockStatus = this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getBlockStatus();
                if (blockStatus == CacheStatus.Modified || blockStatus == CacheStatus.Shared){
                    context.setRegister(destinyRegister, this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getData(blockWord));
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    super.nextCycle();
                }
                else {
                    if (this.simulation.getDataBus().tryLock()) {
                        boolean result = this.manageCheckOtherCache(blockLabel, true);
                        if (result){
                            this.finishLoadWord(context, blockLabel, blockWord, destinyRegister);
                            this.nextCycle();
                        }
                    }
                    else {
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        this.startOver();
                    }
                }
            }
            else {
                if (this.simulation.getDataBus().tryLock()) {
                    if (this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getBlockStatus() == CacheStatus.Modified){
                        this.manageDataCacheFail();
                        this.simulation.saveDataBlockToMainMemory(this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)), this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getLabel());
                    }
                    boolean result = this.manageCheckOtherCache(blockLabel, true);
                    if (result){
                        this.finishLoadWord(context, blockLabel, blockWord, destinyRegister);
                        this.nextCycle();
                    }
                }
                else {
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    this.nextCycle();
                    this.startOver();
                }
            }
        } else {
            this.nextCycle();
            this.startOver();
        }
    }

    /**
     * Finishes the Load Word
     *
     * @param context Context of the thread
     * @param blockLabel Label of the block to be loaded
     * @param blockWord Number of the word in the block
     * @param destinyRegister Number of the destiny register
     */
    private void finishLoadWord(Context context, int blockLabel, int blockWord, int destinyRegister){
        this.simulation.getDataBus().unlock();
        context.setRegister(destinyRegister, this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getData(blockWord));
        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
    }

    /**
     * Manages the SW instruction
     *
     * @param context Context of the thread
     * @param destinyRegister Number of the destiny register
     * @param sourceRegister Number of the source register
     * @param immediate Value of the immediate
     */
    private void manageStoreWord(Context context, int destinyRegister, int sourceRegister, int immediate){
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(context.getRegister(sourceRegister) + immediate);
        int blockWord = this.simulation.getMainMemory().getBlockWordByAddress(context.getRegister(sourceRegister) + immediate);
        if (this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).tryLock()){
            if (this.dataCache.hasBlock(blockLabel)){
                CacheStatus blockStatus = this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getBlockStatus();
                if (blockStatus == CacheStatus.Shared){
                    if (this.simulation.getDataBus().tryLock()) {
                        if (this.simulation.tryLockDataCacheBlock(this.isSimpleCore, blockLabel)) {
                            if (this.simulation.checkDataBlockOnOtherCache(this.isSimpleCore, blockLabel)) {
                                this.simulation.invalidateBlockOnOtherCache(this.isSimpleCore, blockLabel);
                            }
                            this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                            this.simulation.getDataBus().unlock();
                            this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setData(blockWord, context.getRegister(destinyRegister));
                            this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setBlockStatus(CacheStatus.Modified);
                            this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                            super.substractQuantum(context);
                            this.nextCycle();
                        } else {
                            this.simulation.getDataBus().unlock();
                            this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                            this.nextCycle();
                            this.startOver();
                        }
                    }
                    else {
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        this.startOver();
                    }
                }
                else if (blockStatus == CacheStatus.Modified){
                    this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setData(blockWord, context.getRegister(destinyRegister));
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    this.nextCycle();
                }
                else {
                    if (this.simulation.getDataBus().tryLock()) {
                        boolean result = this.manageCheckOtherCache(blockLabel, false);
                        if (result){
                            this.finishStoreWord(context, blockLabel, blockWord, destinyRegister);
                            this.nextCycle();
                        }
                    }
                    else {
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        this.startOver();
                    }
                }
            }
            else {
                if (this.simulation.getDataBus().tryLock()) {
                    if (this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getBlockStatus() == CacheStatus.Modified){
                        this.manageDataCacheFail();
                        this.simulation.saveDataBlockToMainMemory(this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)), this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getLabel());
                    }
                    boolean result = this.manageCheckOtherCache(blockLabel, false);
                    if (result){
                        this.finishStoreWord(context, blockLabel, blockWord, destinyRegister);
                        this.nextCycle();
                    }
                }
                else {
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    this.nextCycle();
                    this.startOver();
                }
            }
        } else {
            this.nextCycle();
            this.startOver();
        }
    }

    /**
     * Finishes the Load Word
     *
     * @param context Context of the thread
     * @param blockLabel Label of the block to be loaded
     * @param blockWord Number of the word in the block
     * @param destinyRegister Number of the destiny register
     */
    private void finishStoreWord(Context context, int blockLabel, int blockWord, int destinyRegister){
        this.simulation.getDataBus().unlock();
        this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setData(blockWord, context.getRegister(destinyRegister));
        this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setBlockStatus(CacheStatus.Modified);
        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
    }

    /**
     * Manages a Data cache fail
     */
    private void manageDataCacheFail(){
        for (int i = 0; i < 40; ++i){
            super.nextCycle();
        }
    }

    /**
     * Makes the thread redo the last instruction
     */
    private void startOver(){
        this.threadContext.setPc(this.threadContext.getPc() - 4);
    }

    /**
     * Checks if the other cache has the desired block
     *
     * @param blockLabel Label of the desired block
     * @param isLoad If it's a load instruction
     * @return True if was executed successfully
     */
    private boolean manageCheckOtherCache(int blockLabel, boolean isLoad){
        boolean successful = true;
        if (this.simulation.tryLockDataCacheBlock(this.isSimpleCore, blockLabel)){
            if (this.simulation.checkDataBlockOnOtherCache(this.isSimpleCore, blockLabel)){
                DataBlock blockFromOtherCache = this.simulation.getDataBlockFromOtherCache(this.isSimpleCore, blockLabel);
                if (blockFromOtherCache.getBlockStatus() == CacheStatus.Shared){
                    if (!isLoad){
                        this.simulation.changeDataBlockStatusFromOtherCache(this.isSimpleCore, blockLabel, CacheStatus.Invalid);
                    }
                    this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                    this.manageDataCacheFail();
                    super.copyFromMemoryToDataCache(blockLabel);
                }
                else if (blockFromOtherCache.getBlockStatus() == CacheStatus.Modified){
                    this.manageDataCacheFail();
                    this.simulation.saveDataBlockToMainMemory(blockFromOtherCache, blockLabel);
                    super.copyFromMemoryToDataCache(blockLabel);
                    if (isLoad){
                        this.simulation.changeDataBlockStatusFromOtherCache(this.isSimpleCore, blockLabel, CacheStatus.Shared);
                    } else {
                        this.simulation.changeDataBlockStatusFromOtherCache(this.isSimpleCore, blockLabel, CacheStatus.Invalid);
                    }
                    this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                }
                else {
                    this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                    this.manageDataCacheFail();
                    super.copyFromMemoryToDataCache(blockLabel);
                }
            }
            else {
                this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                this.manageDataCacheFail();
                super.copyFromMemoryToDataCache(blockLabel);
            }
        }
        else {
            this.simulation.getDataBus().unlock();
            this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
            this.nextCycle();
            this.startOver();
            successful = false;
        }
        return successful;
    }

    /**
     * Manages the end of the quantum
     */
    private void manageQuantumEnd(){
        this.threadContext.setPc(this.threadContext.getPc() - 4);
        super.simulation.addContext(this.threadContext);
        this.threadContext = super.simulation.getNextContext();
        if (this.threadContext.getStartingCycle() == -1){
            this.threadContext.setStartingCycle(super.getClock());
        }
        this.threadContext.setRemainingQuantum(super.quantum);
    }

    /**
     * Get the name of the running thread
     *
     * @return String
     */
    public String getThreadName(){
        if (this.threadContext != null){
            return this.threadContext.getThreadName();
        } else {
            return "No hay hilo corriendo";
        }
    }
}
