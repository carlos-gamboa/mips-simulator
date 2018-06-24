package Logic;

import Controller.Simulation;
import Storage.*;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class DualCore extends Core {

    private Context thread1Context;
    private Context thread2Context;
    private ThreadStatus thread1Status;
    private ThreadStatus thread2Status;
    private Thread thread1;
    private Thread thread2;

    private int thread1DataReservedPosition;
    private int thread2DataReservedPosition;
    private int thread1InstructionReservedPosition;
    private int thread2InstructionReservedPosition;
    private int dataBusReserved;
    private int instructionBusReserved;

    private int oldestThread;

    public DualCore(Simulation simulation, int quantum){
        super(simulation, 8, false, quantum);
        this.thread1Status = ThreadStatus.Running;
        this.thread2Status = ThreadStatus.Null;
        this.oldestThread = 1;
        this.thread1DataReservedPosition = -1;
        this.thread2DataReservedPosition = -1;
        this.thread1InstructionReservedPosition = -1;
        this.thread2InstructionReservedPosition = -1;
        this.thread1 = new Thread(this, "Thread 1");
        this.thread2 = new Thread(this, "Thread 2");
    }

    /**
     * Starts the core
     */
    public void start(){
        this.thread1.start();
        this.thread2.start();
    }

    @Override
    public void run(){
        if (Thread.currentThread().getName().equals("Thread 1")){
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

    /**
     * Starts the Thread 1
     */
    private void runThread1(){
        Instruction instruction;
        //Checks if there are more contexts in the queue
        if (super.simulation.areMoreContexts()) {
            this.thread1Context = super.simulation.getNextContext();
            this.thread1Context.setRemainingQuantum(super.getQuantum());
            if (this.thread1Context.getStartingCycle() == -1){
                this.thread1Context.setStartingCycle(super.getClock());
            }
        }
        else {
            this.thread1Status = ThreadStatus.Finished;
            this.thread2Status = ThreadStatus.Finished;
            super.setRunning(false);
        }
        while (this.thread1Status != ThreadStatus.Finished){
            while (this.getThread1Status() == ThreadStatus.Running || this.getThread1Status() == ThreadStatus.DataCacheFailRunning || this.getThread1Status() == ThreadStatus.InstructionCacheFailRunning){
                instruction = this.checkReservedInstructionPosition(this.thread1Context,this.thread1Context.getPc(), true);
                if (instruction != null) {
                    this.thread1Context.setPc(this.thread1Context.getPc() + 4);
                    this.manageInstruction(instruction, this.thread1Context, true);
                }
            }
            if (super.isRunning) {
                this.nextCycle();
            }
        }
        //Adds cycles while the other threads are still running
        while(super.isRunning || super.simulation.isOtherCoreRunning(super.isSimpleCore)){
            this.nextCycle();
        }
    }

    /**
     * Starts the Thread 2
     */
    private void runThread2(){
        Instruction instruction;
        while (this.thread2Status == ThreadStatus.Null){
            //Waits until the Thread 1 allows Thread 2 to start
        }
        //Checks if there are more contexts in the queue
        if (super.simulation.areMoreContexts()) {
            this.thread2Context = super.simulation.getNextContext();
            this.thread2Context.setRemainingQuantum(super.getQuantum());
            if (this.thread2Context.getStartingCycle() == -1){
                this.thread2Context.setStartingCycle(super.getClock());
            }        } else {
            this.thread2Status = ThreadStatus.Finished;
        }
        while (this.thread2Status != ThreadStatus.Finished) {
            while (this.getThread2Status() == ThreadStatus.Running || this.getThread2Status() == ThreadStatus.DataCacheFailRunning || this.getThread2Status() == ThreadStatus.InstructionCacheFailRunning) {
                instruction = checkReservedInstructionPosition(this.thread2Context,this.thread2Context.getPc(), false);
                if (instruction != null) {
                    this.thread2Context.setPc(this.thread2Context.getPc() + 4);
                    this.manageInstruction(instruction, this.thread2Context, false);
                }
            }
            if (super.isRunning) {
                this.nextCycle();
            }
        }
        //Adds cycles while the other threads are still running
        while(super.isRunning || super.simulation.isOtherCoreRunning(super.isSimpleCore)){
            this.nextCycle();
        }
    }

    /**
     * Manages an instruction
     *
     * @param instruction Instruction to be executed
     * @param context Context of the thread that's going to execute it
     * @param isThread1 If the thread is the thread 1
     */
    private void manageInstruction(Instruction instruction, Context context, boolean isThread1){
        if (context.getRemainingQuantum() != 0){
            switch (instruction.getOperationCode()){
                case 8:
                    this.manageDADDI(context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                    break;
                case 32:
                    this.manageDADD(context, instruction.getImmediate(), instruction.getSourceRegister(), instruction.getDestinyRegister());
                    break;
                case 34:
                    this.manageDSUB(context, instruction.getImmediate(), instruction.getSourceRegister(), instruction.getDestinyRegister());
                    break;
                case 12:
                    this.manageDMUL(context, instruction.getImmediate(), instruction.getSourceRegister(), instruction.getDestinyRegister());
                    break;
                case 14:
                    this.manageDDIV(context, instruction.getImmediate(), instruction.getSourceRegister(), instruction.getDestinyRegister());
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
                    this.checkReservedDataPosition(context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate(), isThread1, true);
                    break;
                case 43:
                    this.checkReservedDataPosition(context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate(), isThread1, false);
                    break;
                case 63:
                    this.manageFIN(isThread1);
                    break;
            }
            context.setRemainingQuantum(context.getRemainingQuantum() - 1);
        } else {
            this.manageQuantumEnd(isThread1);
        }
    }

    /**
     * Executes the final instruction
     *
     * @param isThread1 If the thread is the thread 1
     */
    private void manageFIN (boolean isThread1){
        if (isThread1){
            this.thread1Context.setFinishingCycle(super.getClock());
            super.simulation.addFinishedContext(this.thread1Context);
            if (!super.simulation.areMoreContexts()){
                if (this.thread2Status == ThreadStatus.Finished){
                    super.setRunning(false);
                    this.thread1Status = ThreadStatus.Finished;
                }
                else {
                    this.thread1Status = ThreadStatus.Finished;
                    if (this.thread2Status == ThreadStatus.Waiting){
                        this.thread2Status = ThreadStatus.Running;
                    }
                }
            }
            else {
                this.thread1Context = super.simulation.getNextContext();
                if (this.thread1Context.getStartingCycle() == -1){
                    this.thread1Context.setStartingCycle(super.getClock());
                }
                this.oldestThread = 2;
                if (this.thread2Status == ThreadStatus.Waiting){
                    this.thread2Status = ThreadStatus.Running;
                    this.thread1Status = ThreadStatus.Waiting;
                }
            }
        } else {
            this.thread2Context.setFinishingCycle(super.getClock());
            super.simulation.addFinishedContext(this.thread2Context);
            if (!super.simulation.areMoreContexts()){
                if (this.thread1Status == ThreadStatus.Finished){
                    super.setRunning(false);
                    this.thread2Status = ThreadStatus.Finished;
                }
                else {
                    this.thread2Status = ThreadStatus.Finished;
                    if (this.thread1Status == ThreadStatus.Waiting){
                        this.thread1Status = ThreadStatus.Running;
                    }
                }
            }
            else {
                this.thread2Context = super.simulation.getNextContext();
                if (this.thread2Context.getStartingCycle() == -1){
                    this.thread2Context.setStartingCycle(super.getClock());
                }
                this.oldestThread = 1;
                if (this.thread1Status == ThreadStatus.Waiting){
                    this.thread1Status = ThreadStatus.Running;
                    this.thread2Status = ThreadStatus.Waiting;
                }
            }
        }
        super.nextCycle();
    }

    /**
     * Manages the LW instruction
     *
     * @param context Context of the thread
     * @param destinyRegister Number of the destiny register
     * @param sourceRegister Number of the source register
     * @param immediate Value of the immediate
     * @param isThread1 If the thread is the thread 1
     */
    private void manageLoadWord(Context context, int destinyRegister, int sourceRegister, int immediate, boolean isThread1){
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(context.getRegister(sourceRegister) + immediate);
        int blockWord = this.simulation.getMainMemory().getBlockWordByAddress(context.getRegister(sourceRegister) + immediate);
        if (this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).tryLock()){
            if (this.dataCache.hasBlock(blockLabel)){
                CacheStatus blockStatus = this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getBlockStatus();
                if (blockStatus == CacheStatus.Modified || blockStatus == CacheStatus.Shared){
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    context.setRegister(destinyRegister, this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getData(blockWord));
                    super.nextCycle();
                }
                else {
                    if (this.reserveDataPosition(this.dataCache.calculateIndexByLabel(blockLabel), isThread1) && this.simulation.getDataBus().tryLock()) {
                        boolean result = this.manageCheckOtherDataCache(blockLabel, true, context, isThread1);
                        if (result){
                            this.simulation.getDataBus().unlock();
                            this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                            this.checkIfCanContinueData(isThread1);
                            context.setRegister(destinyRegister, this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getData(blockWord));
                            this.nextCycle();
                        }
                    }
                    else {
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        this.startOver(context);
                    }
                }
            }
            else {
                if (this.reserveDataPosition(this.dataCache.calculateIndexByLabel(blockLabel), isThread1) && this.simulation.getDataBus().tryLock()) {
                    if (this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getBlockStatus() == CacheStatus.Modified){
                        this.manageDataCacheFail(isThread1, blockLabel);
                        this.simulation.saveDataBlockToMainMemory(this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)), blockLabel);
                    }
                    boolean result = this.manageCheckOtherDataCache(blockLabel, true, context, isThread1);
                    if (result){
                        this.simulation.getDataBus().unlock();
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.checkIfCanContinueData(isThread1);
                        context.setRegister(destinyRegister, this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getData(blockWord));
                        this.nextCycle();
                    }
                }
                else {
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    this.nextCycle();
                    this.startOver(context);
                }
            }
        } else {
            this.nextCycle();
            this.startOver(context);
        }
    }

    /**
     * Manages the SW instruction
     *
     * @param context Context of the thread
     * @param destinyRegister Number of the destiny register
     * @param sourceRegister Number of the source register
     * @param immediate Value of the immediate
     * @param isThread1 If the thread is the thread 1
     */
    private void manageStoreWord(Context context, int destinyRegister, int sourceRegister, int immediate, boolean isThread1){
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
                            this.startOver(context);
                        }
                        this.simulation.getDataBus().unlock();
                    }
                    else {
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        this.startOver(context);
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
                        //TODO: CHECK LW
                        boolean result = this.manageCheckOtherDataCache(blockLabel, false, context, isThread1);
                        if (result){
                            this.simulation.getDataBus().unlock();
                            this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                            this.checkIfCanContinueData(isThread1);
                            this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setData(blockWord, context.getRegister(destinyRegister));
                            this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setBlockStatus(CacheStatus.Modified);
                            this.nextCycle();
                        }
                    }
                    else {
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.nextCycle();
                        this.startOver(context);
                    }
                }
            }
            else {
                if (this.simulation.getDataBus().tryLock()) {
                    if (this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).getBlockStatus() == CacheStatus.Modified){
                        this.manageDataCacheFail(isThread1, blockLabel);
                        this.simulation.saveDataBlockToMainMemory(this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)), blockLabel);
                    }
                    //TODO: CHECK LW
                    boolean result = this.manageCheckOtherDataCache(blockLabel, false, context, isThread1);
                    if (result){
                        this.simulation.getDataBus().unlock();
                        this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                        this.checkIfCanContinueData(isThread1);
                        this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setData(blockWord, context.getRegister(destinyRegister));
                        this.dataCache.getBlock(this.dataCache.calculateIndexByLabel(blockLabel)).setBlockStatus(CacheStatus.Modified);
                        this.nextCycle();
                    }
                }
                else {
                    this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
                    this.nextCycle();
                    this.startOver(context);
                }
            }
        } else {
            this.nextCycle();
            this.startOver(context);
        }
    }

    /**
     * Makes a thread redo an instruction
     *
     * @param context Context of the thread
     */
    private void startOver(Context context){
        context.setPc(context.getPc() - 4);
    }

    /**
     * Checks if the other cache has the desired block
     *
     * @param blockLabel Label of the desired block
     * @param isLoad If the instruction is LW
     * @param context Context of the thread
     * @param isThread1 If the thread is Thread 1
     * @return True if the process was fully executed
     */
    private boolean manageCheckOtherDataCache(int blockLabel, boolean isLoad, Context context, boolean isThread1){
        boolean successful = true;
        if (this.simulation.tryLockDataCacheBlock(this.isSimpleCore, blockLabel)){
            if (this.simulation.checkDataBlockOnOtherCache(this.isSimpleCore, blockLabel)){
                DataBlock blockFromOtherCache = this.simulation.getDataBlockFromOtherCache(this.isSimpleCore, blockLabel);
                if (blockFromOtherCache.getBlockStatus() == CacheStatus.Shared){
                    if (!isLoad){
                        this.simulation.changeDataBlockStatusFromOtherCache(this.isSimpleCore, blockLabel, CacheStatus.Invalid);
                    }
                    this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                    this.manageDataCacheFail(isThread1, blockLabel);
                    super.copyFromMemoryToDataCache(blockLabel);
                }
                else if (blockFromOtherCache.getBlockStatus() == CacheStatus.Modified){
                    this.manageDataCacheFail(isThread1, blockLabel);
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
                    this.manageDataCacheFail(isThread1, blockLabel);
                    super.copyFromMemoryToDataCache(blockLabel);
                }
            }
            else {
                this.simulation.unlockDataCacheBlock(this.isSimpleCore, blockLabel);
                this.manageDataCacheFail(isThread1, blockLabel);
                super.copyFromMemoryToDataCache(blockLabel);
            }
        }
        else {
            this.simulation.getDataBus().unlock();
            this.dataCache.getLock(this.dataCache.calculateIndexByLabel(blockLabel)).unlock();
            this.nextCycle();
            this.startOver(context);
            successful = false;
        }
        return successful;
    }

    /**
     * Manages a Data cache fail
     *
     * @param isThread1 If the thread is thread 1
     * @param blockLabel Label of the block
     */
    private void manageDataCacheFail(boolean isThread1, int blockLabel){
        if (isThread1){
            this.thread1Status = ThreadStatus.DataCacheFail;
            if (this.thread2Status !=   ThreadStatus.Finished) {
                if (this.thread2Status == ThreadStatus.Null){
                    this.thread2Status = ThreadStatus.Running;

                }
                else if(this.thread2Status == ThreadStatus.Waiting){
                    this.thread2Status = ThreadStatus.Running;
                }
            }
        } else {
            this.thread2Status = ThreadStatus.DataCacheFail;
            if (this.thread1Status !=   ThreadStatus.Finished) {
                if (this.thread1Status == ThreadStatus.Waiting) {
                    this.thread1Status = ThreadStatus.Running;
                }
            }
        }
        for (int i = 0; i < 40; ++i){
            super.nextCycle();
        }
    }

    /**
     * Checks if the block in the data cache is reserved
     *
     * @param context Context of the thread
     * @param destinyRegister Number of the destiny register
     * @param sourceRegister Number of the source register
     * @param immediate Value of the immediate
     * @param isThread1 If the thread is the thread 1
     * @param isLoad If the instruction is LW
     */
    private void checkReservedDataPosition(Context context, int destinyRegister, int sourceRegister, int immediate, boolean isThread1, boolean isLoad){
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(context.getRegister(sourceRegister) + immediate);
        if (isThread1){
            if (this.thread2DataReservedPosition != this.dataCache.calculateIndexByLabel(blockLabel)){
                if (isLoad) {
                    this.manageLoadWord(context, destinyRegister, sourceRegister, immediate, isThread1);
                } else {
                    this.manageStoreWord(context, destinyRegister, sourceRegister, immediate, isThread1);
                }
            }
            else {
                this.nextCycle();
                this.startOver(context);
            }
        } else {
            if (this.thread1DataReservedPosition != this.dataCache.calculateIndexByLabel(blockLabel)){
                if (isLoad) {
                    this.manageLoadWord(context, destinyRegister, sourceRegister, immediate, isThread1);
                } else {
                    this.manageStoreWord(context, destinyRegister, sourceRegister, immediate, isThread1);
                }
            }
            else {
                this.nextCycle();
                this.startOver(context);
            }
        }
    }

    /**
     * Checks if the desired instruction block is reserved
     *
     * @param context Context of the thread
     * @param pc Pc of the thread
     * @param isThread1 If the thread is thread 1
     * @return Null | Instruction
     */
    private Instruction checkReservedInstructionPosition(Context context, int pc, boolean isThread1){
        Instruction instruction = null;
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(pc);
        if (isThread1){
            if (this.thread2InstructionReservedPosition != this.instructionCache.calculateIndexByLabel(blockLabel)){
                instruction = this.getInstruction(pc, isThread1);
            }
            else {
                this.nextCycle();
                this.startOver(context);
            }
        } else {
            if (this.thread1InstructionReservedPosition != this.instructionCache.calculateIndexByLabel(blockLabel)){
                instruction = this.getInstruction(pc, isThread1);
            }
            else {
                this.nextCycle();
                this.startOver(context);
            }
        }
        return instruction;
    }

    /**
     * Tries to reserve a position in the data cache
     *
     * @param blockIndex Index of the block to reserve
     * @param isThread1 If the thread is thread 1
     * @return True if was reserved
     */
    private boolean reserveDataPosition(int blockIndex, boolean isThread1){
        boolean reserved = false;
        if (isThread1 && this.thread2Status != ThreadStatus.DataCacheFail && this.thread2Status != ThreadStatus.DataCacheFailRunning){
            this.thread1Status = ThreadStatus.DataCacheFailRunning;
            this.dataBusReserved = 1;
            this.thread1DataReservedPosition = blockIndex;
            reserved = true;
        } else if (!isThread1 && this.thread1Status != ThreadStatus.DataCacheFail && this.thread1Status != ThreadStatus.DataCacheFailRunning) {
            this.thread2Status = ThreadStatus.DataCacheFailRunning;
            this.thread2DataReservedPosition = blockIndex;
            this.dataBusReserved = 1;
            reserved = true;
        }
        return reserved;
    }

    /**
     * Checks if a thread can continue executing after solving a Data cache fail
     *
     * @param isThread1 If the thread is thread 1
     */
    private void checkIfCanContinueData(boolean isThread1){
        if (isThread1){
            if (this.thread2Status == ThreadStatus.Finished){
                this.thread1Status = ThreadStatus.Running;
                this.thread1InstructionReservedPosition = -1;
                this.dataBusReserved = 0;
            } else {
                this.thread1DataReservedPosition = -1;
                this.dataBusReserved = 0;
                if (this.thread2Status == ThreadStatus.DataCacheFail || this.thread2Status == ThreadStatus.InstructionCacheFail) {
                    this.thread1Status = ThreadStatus.Running;
                } else {
                    if (this.oldestThread == 1) {
                        if (this.thread2Status == ThreadStatus.Running || this.thread2Status == ThreadStatus.DataCacheFailRunning || this.thread2Status == ThreadStatus.InstructionCacheFailRunning || this.thread2Status == ThreadStatus.Waiting) {
                            this.thread2Status = ThreadStatus.Waiting;
                            this.thread1Status = ThreadStatus.Running;
                        }
                    } else {
                        this.thread1Status = ThreadStatus.Waiting;
                        while (this.thread1Status != ThreadStatus.Running && this.thread1Status != ThreadStatus.DataCacheFailRunning && this.thread1Status != ThreadStatus.InstructionCacheFailRunning) {
                            super.nextCycle();
                        }
                    }

                }
            }
        } else {
            if (this.thread1Status == ThreadStatus.Finished){
                this.thread2Status = ThreadStatus.Running;
                this.thread2InstructionReservedPosition = -1;
                this.dataBusReserved = 0;
            } else {
                this.thread2DataReservedPosition = -1;
                this.dataBusReserved = 0;
                if (this.thread1Status == ThreadStatus.DataCacheFail || this.thread1Status == ThreadStatus.InstructionCacheFail) {
                    this.thread2Status = ThreadStatus.Running;
                } else {
                    if (this.oldestThread == 2) {
                        if (this.thread1Status == ThreadStatus.Running || this.thread1Status == ThreadStatus.DataCacheFailRunning || this.thread1Status == ThreadStatus.InstructionCacheFailRunning || this.thread1Status == ThreadStatus.Waiting) {
                            this.thread1Status = ThreadStatus.Waiting;
                            this.thread2Status = ThreadStatus.Running;
                        }
                    } else {
                        this.thread2Status = ThreadStatus.Waiting;
                        while (this.thread2Status != ThreadStatus.Running && this.thread2Status != ThreadStatus.DataCacheFailRunning && this.thread2Status != ThreadStatus.InstructionCacheFailRunning) {
                            super.nextCycle();
                        }
                    }
                }
            }
        }
    }

    /**
     * Manages the end of the quantum of a thread
     *
     * @param isThread1 If the thread is thread 1
     */
    private void manageQuantumEnd(boolean isThread1){
        if (isThread1){
            this.thread1Context.setPc(this.thread1Context.getPc() - 4);
            super.simulation.addContext(this.thread1Context);
            this.thread1Context = super.simulation.getNextContext();
            if (this.thread1Context.getStartingCycle() == -1){
                this.thread1Context.setStartingCycle(super.getClock());
            }
            this.thread1Context.setRemainingQuantum(super.quantum);
        } else {
            this.thread2Context.setPc(this.thread2Context.getPc() - 4);
            super.simulation.addContext(this.thread2Context);
            this.thread2Context = super.simulation.getNextContext();
            if (this.thread2Context.getStartingCycle() == -1){
                this.thread2Context.setStartingCycle(super.getClock());
            }
            this.thread2Context.setRemainingQuantum(super.quantum);
        }
    }

    /**
     * Gets an instruction based on the given PC
     *
     * @param pc PC of the thread.
     * @param isThread1 If the thread is thread 1
     * @return Null | Instruction
     */
    private Instruction getInstruction(int pc, boolean isThread1){
        Instruction instruction = null;
        InstructionBlock block;
        int blockLabel = this.simulation.getMainMemory().getBlockLabelByAddress(pc);
        int blockWord = this.simulation.getMainMemory().getBlockWordByAddress(pc);
        int cacheIndex = this.instructionCache.calculateIndexByLabel(blockLabel);
        if (this.instructionCache.getLock(cacheIndex).tryLock())
        {
            if (!this.instructionCache.hasBlock(blockLabel))
            { //InstructionCache Fail
                if (this.reserveInstructionPosition(this.instructionCache.calculateIndexByLabel(blockLabel), isThread1))
                {
                    if (this.simulation.getInstructionsBus().tryLock())
                    {
                        this.manageInstructionCacheFail(isThread1, blockLabel);
                        this.copyFromMemoryToInstructionCache(blockLabel);
                        this.instructionCache.getLock(cacheIndex).unlock();
                        this.simulation.getInstructionsBus().unlock();
                        this.checkIfCanContinueInstruction(isThread1);
                        block = this.instructionCache.getBlock(cacheIndex);
                        instruction = block.getValue(blockWord);
                    }
                    else
                    {
                        this.instructionCache.getLock(cacheIndex).unlock();
                        this.nextCycle();
                    }
                }
                else
                {
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
     * Tries to reserve a position in the instruction cache
     *
     * @param blockIndex Index of the block to reserve
     * @param isThread1 If the thread is thread 1
     * @return True if was reserved
     */
    private boolean reserveInstructionPosition (int blockIndex, boolean isThread1){
        boolean reserved = false;
        if (isThread1 && this.thread2Status != ThreadStatus.InstructionCacheFail && this.thread2Status != ThreadStatus.InstructionCacheFailRunning){
            this.thread1Status = ThreadStatus.InstructionCacheFailRunning;
            this.instructionBusReserved = 1;
            this.thread1InstructionReservedPosition = blockIndex;
            reserved = true;
        } else if (!isThread1 && this.thread1Status != ThreadStatus.InstructionCacheFail && this.thread1Status != ThreadStatus.InstructionCacheFailRunning) {
            this.thread2Status = ThreadStatus.InstructionCacheFailRunning;
            this.thread2InstructionReservedPosition = blockIndex;
            this.instructionBusReserved = 1;
            reserved = true;
        }
        return reserved;
    }

    /**
     * Manages a Instruction cache fail
     *
     * @param isThread1 If the thread is thread 1
     * @param blockLabel Label of the block
     */
    private void manageInstructionCacheFail(boolean isThread1, int blockLabel){
        if (isThread1){
            this.thread1Status = ThreadStatus.InstructionCacheFail;
            if (this.thread2Status !=   ThreadStatus.Finished) {
                if (this.thread2Status == ThreadStatus.Null) {
                    this.thread2Status = ThreadStatus.Running;
                } else if (this.thread2Status == ThreadStatus.Waiting) {
                    this.thread2Status = ThreadStatus.Running;
                }
            }
        } else {
            this.thread2Status = ThreadStatus.InstructionCacheFail;
            if (this.thread1Status !=   ThreadStatus.Finished) {
                if (this.thread1Status == ThreadStatus.Waiting) {
                    this.thread1Status = ThreadStatus.Running;
                }
            }
        }
        for (int i = 0; i < 40; ++i){
            super.nextCycle();
        }
    }

    /**
     * Checks if a thread can continue executing after solving a Instruction cache fail
     *
     * @param isThread1 If the thread is thread 1
     */
    private void checkIfCanContinueInstruction(boolean isThread1){
        if (isThread1){
            if (this.thread2Status == ThreadStatus.Finished){
                this.thread1Status = ThreadStatus.Running;
                this.thread1InstructionReservedPosition = -1;
                this.instructionBusReserved = 0;
            } else {
                this.thread1InstructionReservedPosition = -1;
                this.dataBusReserved = 0;
                if (this.thread2Status == ThreadStatus.DataCacheFail || this.thread2Status == ThreadStatus.InstructionCacheFail) {
                    this.thread1Status = ThreadStatus.Running;
                } else {
                    if (this.oldestThread == 1) {
                        if (this.thread2Status == ThreadStatus.Running || this.thread2Status == ThreadStatus.DataCacheFailRunning || this.thread2Status == ThreadStatus.InstructionCacheFailRunning || this.thread2Status == ThreadStatus.Waiting) {
                            this.thread2Status = ThreadStatus.Waiting;
                            this.thread1Status = ThreadStatus.Running;
                        }
                    } else {
                        this.thread1Status = ThreadStatus.Waiting;
                        while (this.thread1Status != ThreadStatus.Running && this.thread1Status != ThreadStatus.DataCacheFailRunning && this.thread1Status != ThreadStatus.InstructionCacheFailRunning) {
                            super.nextCycle();
                        }
                    }
                }
            }
        } else {
            if (this.thread1Status == ThreadStatus.Finished){
                this.thread2Status = ThreadStatus.Running;
                this.thread2InstructionReservedPosition = -1;
                this.dataBusReserved = 0;
            } else {
                this.thread2InstructionReservedPosition = -1;
                this.instructionBusReserved = 0;
                if (this.thread1Status == ThreadStatus.DataCacheFail || this.thread1Status == ThreadStatus.InstructionCacheFail) {
                    this.thread2Status = ThreadStatus.Running;
                } else {
                    if (this.oldestThread == 2) {
                        if (this.thread1Status == ThreadStatus.Running || this.thread1Status == ThreadStatus.DataCacheFailRunning || this.thread1Status == ThreadStatus.InstructionCacheFailRunning || this.thread1Status == ThreadStatus.Waiting) {
                            this.thread1Status = ThreadStatus.Waiting;
                            this.thread2Status = ThreadStatus.Running;
                        }
                    } else {
                        this.thread2Status = ThreadStatus.Waiting;
                        while (this.thread2Status != ThreadStatus.Running && this.thread2Status != ThreadStatus.DataCacheFailRunning && this.thread2Status != ThreadStatus.InstructionCacheFailRunning) {
                            super.nextCycle();
                        }
                    }
                }
            }
        }
    }
}
