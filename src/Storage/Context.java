package Storage;

public class Context {

    private int[] registers;
    private int pc;
    private int startingCycle;
    private int finishingCycle;
    private int remainingQuantum;
    private String threadName;

    public Context (){
        this.startingCycle = -1;
        this.pc = 0;
        this.registers = new int[32];
        for (int i = 0; i < this.registers.length; ++i){
            this.registers[i] = 0;
        }
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

    public int getStartingCycle() {
        return startingCycle;
    }

    public void setStartingCycle(int startingCycle) {
        this.startingCycle = startingCycle;
    }

    public int getRemainingQuantum() {
        return remainingQuantum;
    }

    public void setRemainingQuantum(int remainingQuantum) {
        this.remainingQuantum = remainingQuantum;
    }

    public int getFinishingCycle() {
        return finishingCycle;
    }

    public void setFinishingCycle(int finishingCycle) {
        this.finishingCycle = finishingCycle;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String toString(){
        String context = "--- Contexto hilo " + this.threadName + " ---\n";
        for (int i = 0; i < this.registers.length; ++i){
            context += this.registers[i] + " ";
        }
        context += "\nDuracion: " + (this.finishingCycle - this.startingCycle) + "\n";
        context += "--- Fin de contexto hilo " + this.threadName + " ---\n";
        return context;
    }
}
