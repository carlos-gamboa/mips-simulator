package Logic;

import Storage.Context;
import Storage.Instruction;

public class SimpleCoreThread implements Runnable {

    private volatile SimpleCore core;
    private volatile Context context;

    public SimpleCoreThread (SimpleCore core){
        this.core = core;
        this.context = this.core.getCurrentThread();
    }

    public void run(){
        while (this.core.isRunning()){
            Instruction instruction = this.core.getInstruction(this.context.getPc());
            this.context.setPc(this.context.getPc() + 4);
            this.manageInstruction(instruction);
        }
    }


    private void manageInstruction(Instruction instruction){
        switch (instruction.getOperationCode()){
            case 8:
                this.core.manageDADDI(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 32:
                this.core.manageDADD(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 34:
                this.core.manageDSUB(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 12:
                this.core.manageDMUL(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 14:
                this.core.manageDDIV(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 4:
                this.core.manageBEQZ(this.context, instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 5:
                this.core.manageBNEZ(this.context, instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 3:
                this.core.manageJAL(this.context, instruction.getImmediate());
                break;
            case 2:
                this.core.manageJR(this.context, instruction.getSourceRegister());
                break;
            case 35:
                this.core.manageLoadWord(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 43:
                this.core.manageStoreWord(this.context, instruction.getDestinyRegister(), instruction.getSourceRegister(), instruction.getImmediate());
                break;
            case 63:
                this.core.manageFIN();
                break;
        }
    }

}
