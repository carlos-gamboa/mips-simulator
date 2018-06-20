package Logic;

import Controller.Simulation;
import Storage.CacheStatus;
import Storage.Context;
import Storage.DataBlock;
import Storage.Instruction;

public class SimpleCore extends Core {

    private volatile Context threadContext;
    private Thread thread;
    private int remainingQuantum;

    public SimpleCore(Simulation simulation, int quantum){
        super(simulation, 4, true, quantum);
        this.thread = new Thread(this, "Thread 0");
    }

    public void start(){
        this.thread.start();
    }

    @Override
    public void run(){
        this.threadContext = super.simulation.getNextContext();
        this.remainingQuantum = super.getQuantum();
        Instruction instruction;
        while (super.isRunning()){
            do {
                instruction = this.getInstruction(this.threadContext.getPc());
            } while (instruction == null);
            this.threadContext.setPc(this.threadContext.getPc() + 4);
            this.manageInstruction(instruction);
            System.out.println("Instruction: " + instruction.toString());
        }
    }

    public Context getCurrentThread() {
        return threadContext;
    }

    public void setCurrentThread(Context currentThread) {
        this.threadContext = currentThread;
    }

    public void manageFIN (){
        if (!super.simulation.areMoreContexts()){
            super.setRunning(false);
        }
        else {
            super.simulation.addFinishedContext(this.threadContext);
            this.threadContext = super.simulation.getNextContext();
        }
        super.nextCycle();
    }

    private void manageInstruction(Instruction instruction){
        if (this.remainingQuantum != 0){
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
            this.remainingQuantum--;
        } else {
            this.manageQuantumEnd();
        }
    }

    public void manageLoadWord(Context context, int destinyRegister, int sourceRegister, int immediate){
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(context.getRegister(sourceRegister) + immediate);
        int blockWord = this.simulation.getMainMemory().getBlockWordByAddress(context.getRegister(sourceRegister) + immediate);
        if (this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).tryLock()){
            if (this.dataCache.hasBlock(blockLabel)){
                CacheStatus blockStatus = this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getBlockStatus();
                if (blockStatus == CacheStatus.Modified || blockStatus == CacheStatus.Shared){
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    context.setRegister(destinyRegister, this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getData(blockWord));
                    super.nextCycle();
                    this.remainingQuantum--;
                }
                else {
                    if (this.simulation.getDataBus().tryLock()) {
                        boolean result = this.manageCheckOtherCache(blockLabel, true);
                        if (result){
                            this.simulation.getDataBus().unlock();
                            this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                            context.setRegister(destinyRegister, this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getData(blockWord));
                            this.nextCycle();
                            this.remainingQuantum--;
                        }
                    }
                    else {
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        this.remainingQuantum--;
                        this.startOver();
                    }
                }
            }
            else {
                if (this.simulation.getDataBus().tryLock()) {
                    if (this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getBlockStatus() == CacheStatus.Modified){
                        this.manageDataCacheFail();
                        this.simulation.saveDataBlockToMainMemory(this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)), blockLabel);
                    }
                    boolean result = this.manageCheckOtherCache(blockLabel, true);
                    if (result){
                        this.simulation.getDataBus().unlock();
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        context.setRegister(destinyRegister, this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getData(blockWord));
                        this.nextCycle();
                        this.remainingQuantum--;
                    }
                }
                else {
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    this.nextCycle();
                    this.remainingQuantum--;
                    this.startOver();
                }
            }
        } else {
            this.nextCycle();
            this.remainingQuantum--;
            this.startOver();
        }
    }

    public void manageStoreWord(Context context, int destinyRegister, int sourceRegister, int immediate){
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
                        } else {
                            this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                            this.simulation.getDataBus().unlock();
                            this.nextCycle();
                            this.remainingQuantum--;
                            this.startOver();
                        }
                        this.simulation.getDataBus().unlock();
                    }
                    else {
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        this.remainingQuantum--;
                        this.startOver();
                    }
                    this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setData(blockWord, context.getRegister(destinyRegister));
                    this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setBlockStatus(CacheStatus.Modified);
                    this.nextCycle();
                    this.remainingQuantum--;
                }
                else if (blockStatus == CacheStatus.Modified){
                    this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setData(blockWord, context.getRegister(destinyRegister));
                    this.nextCycle();
                    this.remainingQuantum--;
                }
                else {
                    if (this.simulation.getDataBus().tryLock()) {
                        boolean result = this.manageCheckOtherCache(blockLabel, false);
                        if (result){
                            this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setData(blockWord, context.getRegister(destinyRegister));
                            this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setBlockStatus(CacheStatus.Modified);
                            this.simulation.getDataBus().unlock();
                            this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                            this.nextCycle();
                            this.remainingQuantum--;
                        }
                    }
                    else {
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        this.remainingQuantum--;
                        this.startOver();
                    }
                }
            }
            else {
                if (this.simulation.getDataBus().tryLock()) {
                    if (this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getBlockStatus() == CacheStatus.Modified){
                        this.manageDataCacheFail();
                        this.simulation.saveDataBlockToMainMemory(this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)), blockLabel);
                    }
                    boolean result = this.manageCheckOtherCache(blockLabel, false);
                    if (result){
                        this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setData(blockWord, context.getRegister(destinyRegister));
                        this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setBlockStatus(CacheStatus.Modified);
                        this.simulation.getDataBus().unlock();
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        this.remainingQuantum--;
                    }
                }
                else {
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    this.nextCycle();
                    this.remainingQuantum--;
                    this.startOver();
                }
            }
        } else {
            this.nextCycle();
            this.remainingQuantum--;
            this.startOver();
        }
    }

    private void manageDataCacheFail(){
        for (int i = 0; i < 40; ++i){
            super.nextCycle();
        }
    }

    private void startOver(){
        this.threadContext.setPc(this.threadContext.getPc() + 4);
    }

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

    private void manageQuantumEnd(){
        super.simulation.addContext(this.threadContext);
        this.threadContext = super.simulation.getNextContext();
    }
}
