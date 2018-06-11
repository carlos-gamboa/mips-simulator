package Logic;

import Controller.Simulation;
import Storage.CacheStatus;
import Storage.Context;
import Storage.DataBlock;

public class DualCore extends Core {

    private Context thread1Context;
    private Context thread2Context;
    private ThreadStatus thread1Status;
    private ThreadStatus thread2Status;
    //Ok se reserva antes de usar el bus
    private int thread2ReservedPosition;
    private Thread thread1;
    private Thread thread2;
    private int oldestThread;

    public DualCore(Simulation simulation){
        super(simulation, 8, false);
        this.thread1Context = super.getSimulation().getNextContext();
        this.thread2Context = null;
        this.thread1Status = ThreadStatus.Running;
        this.oldestThread = 1;
        this.thread1 = (new Thread(new CoreThread(this, true, this.thread1Context)));
        this.thread2 = (new Thread(new CoreThread(this, false, this.thread2Context)));
    }

    public void startRunning (){
        this.thread1.run();
        this.thread2.run();
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

    public void manageFIN (){

    }

    public void manageLoadWord(Context context, int destinyRegister, int sourceRegister, int immediate){
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(context.getRegister(sourceRegister) + immediate);
        int blockWord = this.simulation.getMainMemory().getBlockWordByAddress(context.getRegister(sourceRegister) + immediate);
        //Preguntar si esta reservada
        if (this.dataCache.getBlock(blockLabel).getLock().tryLock()){
            if (this.dataCache.hasBlock(blockLabel)){
                CacheStatus blockStatus = this.dataCache.getBlock(blockLabel).getBlockStatus();
                if (blockStatus == CacheStatus.Modified || blockStatus == CacheStatus.Shared){
                    this.dataCache.getBlock(blockLabel).getLock().unlock();
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
                            this.dataCache.getBlock(blockLabel).getLock().unlock();
                            this.nextCycle();
                            //TODO: start over
                        }
                    }
                    else {
                        this.dataCache.getBlock(blockLabel).getLock().unlock();
                        this.nextCycle();
                        //TODO: start over
                    }
                    this.dataCache.getBlock(blockLabel).getLock().unlock();
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
                        this.dataCache.getBlock(blockLabel).getLock().unlock();
                        this.nextCycle();
                        //TODO: start over
                    }
                }
                else {
                    this.dataCache.getBlock(blockLabel).getLock().unlock();
                    this.nextCycle();
                    //TODO: start over
                }
                this.dataCache.getBlock(blockLabel).getLock().unlock();
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
        if (this.dataCache.getBlock(blockLabel).getLock().tryLock()){
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
                            this.dataCache.getBlock(blockLabel).getLock().unlock();
                            this.nextCycle();
                            //TODO: start over
                        }
                        this.dataCache.getBlock(blockLabel).setData(blockWord, context.getRegister(destinyRegister));
                        this.dataCache.getBlock(blockLabel).setBlockStatus(CacheStatus.Modified);
                        this.nextCycle();
                    }
                    else {
                        this.dataCache.getBlock(blockLabel).getLock().unlock();
                        this.nextCycle();
                        //TODO: start over
                    }
                    this.dataCache.getBlock(blockLabel).getLock().unlock();
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
                        this.dataCache.getBlock(blockLabel).getLock().unlock();
                        this.nextCycle();
                        //TODO: start over
                    }
                }
                else {
                    this.dataCache.getBlock(blockLabel).getLock().unlock();
                    this.nextCycle();
                    //TODO: start over
                }
                this.dataCache.getBlock(blockLabel).getLock().unlock();
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
                    if (super.dataCache.getBlock(blockLabel).getLock().tryLock()){
                        //TODO: Resolve
                        super.dataCache.getBlock(blockLabel).getLock().unlock();
                    }
                    this.simulation.getDataBus().unlock();
                } else {
                    this.thread1Status = ThreadStatus.Waiting;
                    while (this.thread1Status == ThreadStatus.Waiting){
                        if (this.simulation.getDataBus().tryLock()){
                            if (super.dataCache.getBlock(blockLabel).getLock().tryLock()){
                                //TODO: Resolve
                                super.dataCache.getBlock(blockLabel).getLock().unlock();
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
