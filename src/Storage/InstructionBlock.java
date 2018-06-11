package Storage;

public class InstructionBlock extends Block {

    private Instruction[] values;

    public InstructionBlock() {
        this.values = new Instruction[4];
    }

    public Instruction getValue (int i){
        return this.values[i];
    }

    public void setValue (int i, Instruction value){
        this.values[i] = value;
    }

}
