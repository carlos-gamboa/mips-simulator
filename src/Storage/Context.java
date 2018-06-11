package Storage;

public class Context {

    private int[] registers;
    private int pc;

    public Context (){
        this.pc = 0;
        this.registers = new int[32];
    }

    public int getPc() {
        return this.pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public int getRegister(int i) {
        return this.registers[i];
    }

    public void setRegister(int i, int value) {
        this.registers[i] = value;
    }
}
