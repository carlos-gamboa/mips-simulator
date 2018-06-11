package Storage;

public class Instruction {
    private int operationCode;
    private int sourceRegister;
    private int destinyRegister;
    private int immediate;

    public Instruction(int operationCode, int sourceRegister, int destinyRegister, int immediate) {
        this.operationCode = operationCode;
        this.sourceRegister = sourceRegister;
        this.destinyRegister = destinyRegister;
        this.immediate = immediate;
    }

    public Instruction() {
    }

    public int getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(int operationCode) {
        this.operationCode = operationCode;
    }

    public int getSourceRegister() {
        return sourceRegister;
    }

    public void setSourceRegister(int sourceRegister) {
        this.sourceRegister = sourceRegister;
    }

    public int getDestinyRegister() {
        return destinyRegister;
    }

    public void setDestinyRegister(int destinyRegister) {
        this.destinyRegister = destinyRegister;
    }

    public int getImmediate() {
        return immediate;
    }

    public void setImmediate(int immediate) {
        this.immediate = immediate;
    }
}
