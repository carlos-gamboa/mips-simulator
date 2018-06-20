package Logic;

import Controller.Simulation;
import Storage.CacheStatus;
import Storage.Context;
import Storage.DataBlock;
import Storage.Instruction;

public class DualCore extends Core {

    private Context thread1Context;
    private Context thread2Context;
    private ThreadStatus thread1Status;
    private ThreadStatus thread2Status;
    private Thread thread1;
    private Thread thread2;

    //Ok se reserva antes de usar el bus
    private int thread2ReservedPosition;

    private int oldestThread;

    public DualCore(Simulation simulation, int quantum){
        super(simulation, 8, false, quantum);
        this.thread1Context = super.getSimulation().getNextContext();
        this.thread2Context = null;
        this.thread1Status = ThreadStatus.Running;
        this.oldestThread = 1;
        this.thread1 = new Thread(this, "Thread 1");
        this.thread2 = new Thread(this, "Thread 2");
    }

    public void start(){
        this.thread1.start();
        this.thread2.start();
    }

    @Override
    public void run(){
        if (Thread.currentThread().getName() == "Thread 1"){
            this.runThread1();
        } else {
            this.runThread2();
        }
    }

    public Context getThread1Context() {
        return thread1Context;
    }

    public void setThread1Context(Context thread1Context) {
        this.thread1Context = thread1Context;
    }

    public Context getThread2Context() {
        return thread2Context;
    }

    public void setThread2Context(Context thread2Context) {
        this.thread2Context = thread2Context;
    }

    public ThreadStatus getThread1Status() {
        return thread1Status;
    }

    public void setThread1Status(ThreadStatus thread1Status) {
        this.thread1Status = thread1Status;
    }

    public ThreadStatus getThread2Status() {
        return thread2Status;
    }

    public void setThread2Status(ThreadStatus thread2Status) {
        this.thread2Status = thread2Status;
    }

    private void manageInstruction(Instruction instruction, Context context){
        switch (instruction.getOperationCode()){
            case 8:
                this.manageDADDI(context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 32:
                this.manageDADD(context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 34:
                this.manageDSUB(context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 12:
                this.manageDMUL(context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 14:
                this.manageDDIV(context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 4:
                this.manageBEQZ(context, instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 5:
                this.manageBNEZ(context, instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 3:
                this.manageJAL(context, instruction.getImmediate());
                break;
            case 2:
                this.manageJR(context, instruction.getSourceRegister());
                break;
            case 35:
                this.manageLoadWord(context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 43:
                this.manageStoreWord(context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 63:
                this.manageFIN();
                break;
        }
    }

    public void manageFIN (){

    }

    private void runThread1(){
        if (this.getThread1Status() == ThreadStatus.Running){
            this.manageInstruction(this.getInstruction(this.thread1Context.getPc()), this.thread1Context);
        }
    }

    private void runThread2(){
        if (this.getThread2Status() == ThreadStatus.Running){
            this.manageInstruction(this.getInstruction(this.thread2Context.getPc()), this.thread2Context);
        }
    }

    public void manageLoadWord(Context context, int destinyRegister, int sourceRegister, int immediate){
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(context.getRegister(sourceRegister) + immediate);
        int blockWord = this.simulation.getMainMemory().getBlockWordByAddress(context.getRegister(sourceRegister) + immediate);
        //Preguntar si esta reservada
        if (this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).tryLock()){
            if (this.dataCache.hasBlock(blockLabel)){
                CacheStatus blockStatus = this.dataCache.getBlock(blockLabel).getBlockStatus();
                if (blockStatus == CacheStatus.Modified || blockStatus == CacheStatus.Shared){
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    context.setRegister(destinyRegister, this.dataCache.getBlock(blockLabel).getData(blockWord));
                    super.nextCycle();
                }
                else {
                    // Se pregunta si el otro hilo esta en fallo de esa cache
                    if (this.simulation.getDataBus().tryLock()) {
                        if (this.simulation.tryLockDataCacheBlock(this.isSimpleCore, blockLabel)){
                            if (this.simulation.checkDataBlockOnOtherCache(this.isSimpleCore, blockLabel)){
                                DataBlock blockFromOtherCache = this.simulation.getDataBlockFromOtherCache(this.isSimpleCore, blockLabel);
                                if (blockFromOtherCache.getBlockStatus() == CacheStatus.Modified){
                                    //TODO: Copy to cache
                                    this.simulation.saveDataBlockToMainMemory(blockFromOtherCache, blockLabel);
                                    this.simulation.changeDataBlockStatusFromOtherCache(this.isSimpleCore, blockLabel, CacheStatus.Shared);
                                    this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                                }
                                else {
                                    this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                                    //TODO: Copy from memory
                                }
                            }
                            else {
                                this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                                //TODO: Copy from memory
                            }
                        }
                        else {
                            this.simulation.getDataBus().unlock();
                            this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                            this.nextCycle();
                            //TODO: start over
                        }
                    }
                    else {
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        //TODO: start over
                    }
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    context.setRegister(destinyRegister, this.dataCache.getBlock(blockLabel).getData(blockWord));
                    this.nextCycle();
                }
            }
            else {
                if (this.simulation.getDataBus().tryLock()) {
                    if (this.dataCache.getBlock(blockLabel).getBlockStatus() == CacheStatus.Modified){
                        this.simulation.saveDataBlockToMainMemory(this.dataCache.getBlock(blockLabel), blockLabel);
                        //TODO: 40 cycles
                    }
                    if (this.simulation.tryLockDataCacheBlock(this.isSimpleCore, blockLabel)){
                        if (this.simulation.checkDataBlockOnOtherCache(this.isSimpleCore, blockLabel)){
                            DataBlock blockFromOtherCache = this.simulation.getDataBlockFromOtherCache(this.isSimpleCore, blockLabel);
                            if (blockFromOtherCache.getBlockStatus() == CacheStatus.Shared){
                                this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                                //TODO: Copy from memory
                            }
                            else if (blockFromOtherCache.getBlockStatus() == CacheStatus.Modified){
                                //TODO: Copy to cache
                                this.simulation.saveDataBlockToMainMemory(blockFromOtherCache, blockLabel);
                                this.simulation.changeDataBlockStatusFromOtherCache(this.isSimpleCore, blockLabel, CacheStatus.Shared);
                                this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                            }
                            else {
                                this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                                //TODO: Copy from memory
                            }
                        }
                        else {
                            this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                            //TODO: Copy from memory
                        }
                    }
                    else {
                        this.simulation.getDataBus().unlock();
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        //TODO: start over
                    }
                }
                else {
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    this.nextCycle();
                    //TODO: start over
                }
                this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                context.setRegister(destinyRegister, this.dataCache.getBlock(blockLabel).getData(blockWord));
                this.nextCycle();
            }
        } else {
            this.nextCycle();
            //TODO: start over
        }
    }

    public void manageStoreWord(Context context, int destinyRegister, int sourceRegister, int immediate){
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(context.getRegister(sourceRegister) + immediate);
        int blockWord = this.simulation.getMainMemory().getBlockWordByAddress(context.getRegister(sourceRegister) + immediate);
        if (this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).tryLock()){
            if (this.dataCache.hasBlock(blockLabel)){
                CacheStatus blockStatus = this.dataCache.getBlock(blockLabel).getBlockStatus();
                if (blockStatus == CacheStatus.Shared){
                    if (this.simulation.getDataBus().tryLock()) {
                        if (this.simulation.tryLockDataCacheBlock(this.isSimpleCore, blockLabel)) {
                            if (this.simulation.checkDataBlockOnOtherCache(this.isSimpleCore, blockLabel)) {
                                this.simulation.invalidateBlockOnOtherCache(this.isSimpleCore, blockLabel);
                            }
                            this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                        } else {
                            this.nextCycle();
                            this.simulation.getDataBus().unlock();
                            //TODO: Start again
                        }
                        this.simulation.getDataBus().unlock();
                    }
                    else {
                        this.nextCycle();
                        //TODO: Start again
                    }
                    this.dataCache.getBlock(blockLabel).setData(blockWord, context.getRegister(destinyRegister));
                    this.dataCache.getBlock(blockLabel).setBlockStatus(CacheStatus.Modified);
                    this.nextCycle();
                }
                else if (blockStatus == CacheStatus.Modified){
                    this.dataCache.getBlock(blockLabel).setData(blockWord, context.getRegister(destinyRegister));
                    this.nextCycle();
                }
                else {
                    if (this.simulation.getDataBus().tryLock()) {
                        if (this.simulation.tryLockDataCacheBlock(this.isSimpleCore, blockLabel)){
                            if (this.simulation.checkDataBlockOnOtherCache(this.isSimpleCore, blockLabel)){
                                DataBlock blockFromOtherCache = this.simulation.getDataBlockFromOtherCache(this.isSimpleCore, blockLabel);
                                if (blockFromOtherCache.getBlockStatus() == CacheStatus.Modified){
                                    //TODO: Copy to cache
                                    this.simulation.saveDataBlockToMainMemory(blockFromOtherCache, blockLabel);
                                    this.simulation.changeDataBlockStatusFromOtherCache(this.isSimpleCore, blockLabel, CacheStatus.Invalid);
                                    this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                                }
                                else if (blockFromOtherCache.getBlockStatus() == CacheStatus.Shared){
                                    this.simulation.changeDataBlockStatusFromOtherCache(this.isSimpleCore, blockLabel, CacheStatus.Invalid);
                                    this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                                    //TODO: Copy to cache
                                }
                                else {
                                    this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                                    //TODO: Copy from memory
                                }
                            }
                            else {
                                this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                                //TODO: Copy from memory
                            }
                        }
                        else {
                            this.simulation.getDataBus().unlock();
                            this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                            this.nextCycle();
                            //TODO: start over
                        }
                        this.dataCache.getBlock(blockLabel).setData(blockWord, context.getRegister(destinyRegister));
                        this.dataCache.getBlock(blockLabel).setBlockStatus(CacheStatus.Modified);
                        this.nextCycle();
                    }
                    else {
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        //TODO: start over
                    }
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    context.setRegister(destinyRegister, this.dataCache.getBlock(blockLabel).getData(blockWord));
                    this.nextCycle();
                }
            }
            else {
                if (this.simulation.getDataBus().tryLock()) {
                    if (this.dataCache.getBlock(blockLabel).getBlockStatus() == CacheStatus.Modified){
                        this.simulation.saveDataBlockToMainMemory(this.dataCache.getBlock(blockLabel), blockLabel);
                        //TODO: 40 cycles
                    }
                    if (this.simulation.tryLockDataCacheBlock(this.isSimpleCore, blockLabel)){
                        if (this.simulation.checkDataBlockOnOtherCache(this.isSimpleCore, blockLabel)){
                            DataBlock blockFromOtherCache = this.simulation.getDataBlockFromOtherCache(this.isSimpleCore, blockLabel);
                            if (blockFromOtherCache.getBlockStatus() == CacheStatus.Modified){
                                //TODO: Copy to cache
                                this.simulation.saveDataBlockToMainMemory(blockFromOtherCache, blockLabel);
                                this.simulation.changeDataBlockStatusFromOtherCache(this.isSimpleCore, blockLabel, CacheStatus.Invalid);
                                this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                            }
                            else if (blockFromOtherCache.getBlockStatus() == CacheStatus.Shared){
                                this.simulation.changeDataBlockStatusFromOtherCache(this.isSimpleCore, blockLabel, CacheStatus.Invalid);
                                this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                                //TODO: Copy to cache
                            }
                            else {
                                this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                                //TODO: Copy from memory
                            }
                        }
                        else {
                            this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                            //TODO: Copy from memory
                        }
                    }
                    else {
                        this.simulation.getDataBus().unlock();
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        //TODO: start over
                    }
                }
                else {
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    this.nextCycle();
                    //TODO: start over
                }
                this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                context.setRegister(destinyRegister, this.dataCache.getBlock(blockLabel).getData(blockWord));
                this.nextCycle();
            }
        } else {
            this.nextCycle();
            //TODO: start over
        }
    }

    public void manageDataCacheFail(Context context, int blockLabel){
        if (this.thread1Context == context){
            this.thread1Status = ThreadStatus.DataCacheFail;
            // If there's not another thread
            if (this.thread2Context == null){
                this.thread2Context = this.simulation.getNextContext();
                this.thread2Status = ThreadStatus.Running;
                for (int i = 0; i < 40; ++i){
                    super.nextCycle();
                }
            }
            else {
                if (this.simulation.getDataBus().tryLock()){
                    if (super.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).tryLock()){
                        //TODO: Resolve
                        super.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    }
                    this.simulation.getDataBus().unlock();
                } else {
                    this.thread1Status = ThreadStatus.Waiting;
                    while (this.thread1Status == ThreadStatus.Waiting){
                        if (this.simulation.getDataBus().tryLock()){
                            if (super.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).tryLock()){
                                //TODO: Resolve
                                super.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                            }
                            this.simulation.getDataBus().unlock();
                        }
                    }

                }
            }
        } else {
            this.thread2Status = ThreadStatus.DataCacheFail;
        }

        this.manageDataCacheSolved(context);
    }

    public void manageDataCacheSolved(Context context){
        if (this.thread1Context == context){
            this.thread1Status = ThreadStatus.DataCacheFail;
        } else {
            this.thread2Status = ThreadStatus.DataCacheFail;
        }
    }
}
