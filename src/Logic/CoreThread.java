package Logic;

import Storage.Context;
import Storage.Instruction;

public class CoreThread implements Runnable {

    private DualCore dualCore;
    private boolean isThread1;
    private Context context;

    public CoreThread (DualCore dualCore, boolean isThread1, Context context){
        this.dualCore = dualCore;
        this.isThread1 = isThread1;
        this.context = context;
    }

    public void run(){
        if (isThread1){
            this.runThread1();
        } else {
            this.runThread2();
        }
    }

    private void runThread1(){
        if (this.dualCore.getThread1Status() == ThreadStatus.Running){
            this.manageInstruction(this.dualCore.getInstruction(this.context.getPc()));
        }
    }

    private void runThread2(){
        if (this.dualCore.getThread2Status() == ThreadStatus.Running){
            this.manageInstruction(this.dualCore.getInstruction(this.context.getPc()));
        }
    }

    private void manageInstruction(Instruction instruction){
        switch (instruction.getOperationCode()){
            case 8:
                this.dualCore.manageDADDI(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 32:
                this.dualCore.manageDADD(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 34:
                this.dualCore.manageDSUB(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 12:
                this.dualCore.manageDMUL(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 14:
                this.dualCore.manageDDIV(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 4:
                this.dualCore.manageBEQZ(this.context, instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 5:
                this.dualCore.manageBNEZ(this.context, instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 3:
                this.dualCore.manageJAL(this.context, instruction.getImmediate());
                break;
            case 2:
                this.dualCore.manageJR(this.context, instruction.getSourceRegister());
                break;
            case 35:
                this.dualCore.manageLoadWord(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 43:
                this.dualCore.manageStoreWord(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 63:
                this.dualCore.manageFIN();
                break;
        }
    }

}
