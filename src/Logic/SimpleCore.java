package Logic;

import Controller.Simulation;
import Storage.CacheStatus;
import Storage.Context;
import Storage.DataBlock;

public class SimpleCore extends Core {

    private volatile Context currentThread;

    public SimpleCore(Simulation simulation){
        super(simulation, 4, true);
        this.currentThread = super.getSimulation().getNextContext();
    }

    public Context getCurrentThread() {
        return currentThread;
    }

    public void setCurrentThread(Context currentThread) {
        this.currentThread = currentThread;
    }

    public void manageFIN (){
        if (!super.simulation.areMoreContexts()){
            super.setRunning(false);
        }
        else {
            super.simulation.addFinishedContext(this.currentThread);
            this.currentThread = super.simulation.getNextContext();
        }
    }

    public void manageLoadWord(Context context, int destinyRegister, int sourceRegister, int immediate){
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(context.getRegister(sourceRegister) + immediate);
        int blockWord = this.simulation.getMainMemory().getBlockWordByAddress(context.getRegister(sourceRegister) + immediate);
        if (this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getLock().tryLock()){
            if (this.dataCache.hasBlock(blockLabel)){
                CacheStatus blockStatus = this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getBlockStatus();
                if (blockStatus == CacheStatus.Modified || blockStatus == CacheStatus.Shared){
                    this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getLock().unlock();
                    context.setRegister(destinyRegister, this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getData(blockWord));
                    super.nextCycle();
                }
                else {
                    if (this.simulation.getDataBus().tryLock()) {
                        boolean result = this.manageCheckOtherCache(blockLabel, true);
                        if (result){
                            this.simulation.getDataBus().unlock();
                            this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getLock().unlock();
                            context.setRegister(destinyRegister, this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getData(blockWord));
                            this.nextCycle();
                        }
                    }
                    else {
                        this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getLock().unlock();
                        this.nextCycle();
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
                        this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getLock().unlock();
                        context.setRegister(destinyRegister, this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getData(blockWord));
                        this.nextCycle();
                    }
                }
                else {
                    this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getLock().unlock();
                    this.nextCycle();
                    this.startOver();
                }
            }
        } else {
            this.nextCycle();
            this.startOver();
        }
    }

    public void manageStoreWord(Context context, int destinyRegister, int sourceRegister, int immediate){
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(context.getRegister(sourceRegister) + immediate);
        int blockWord = this.simulation.getMainMemory().getBlockWordByAddress(context.getRegister(sourceRegister) + immediate);
        if (this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getLock().tryLock()){
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
                            this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getLock().unlock();
                            this.simulation.getDataBus().unlock();
                            this.nextCycle();
                            this.startOver();
                        }
                        this.simulation.getDataBus().unlock();
                    }
                    else {
                        this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getLock().unlock();
                        this.nextCycle();
                        this.startOver();
                    }
                    this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setData(blockWord, context.getRegister(destinyRegister));
                    this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setBlockStatus(CacheStatus.Modified);
                    this.nextCycle();
                }
                else if (blockStatus == CacheStatus.Modified){
                    this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setData(blockWord, context.getRegister(destinyRegister));
                    this.nextCycle();
                }
                else {
                    if (this.simulation.getDataBus().tryLock()) {
                        this.manageCheckOtherCache(blockLabel, false);

                        this.dataCache.getBlock(blockLabel).setData(blockWord, context.getRegister(destinyRegister));
                        this.dataCache.getBlock(blockLabel).setBlockStatus(CacheStatus.Modified);
                        this.nextCycle();
                    }
                    else {
                        this.dataCache.getBlock(blockLabel).getLock().unlock();
                        this.nextCycle();
                        this.startOver();
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

    private void manageDataCacheFail(){
        for (int i = 0; i < 40; ++i){
            super.nextCycle();
        }
    }

    private void startOver(){
        this.currentThread.setPc(this.currentThread.getPc() + 4);
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
            this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getLock().unlock();
            this.nextCycle();
            this.startOver();
            successful = false;
        }
        return successful;
    }
}
