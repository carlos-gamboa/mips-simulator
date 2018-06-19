package Storage;

public class DataBlock extends Block {

    private int[] data;
    private CacheStatus blockStatus;

    public DataBlock() {
        this.blockStatus = CacheStatus.Invalid;
        this.data = new int[4];
        for (int i = 0; i < 4; ++i){
            this.data[i] = 1;
        }
    }

    public CacheStatus getBlockStatus() {
        return this.blockStatus;
    }

    public void setBlockStatus(CacheStatus blackStatus){
        this.blockStatus = blockStatus;
    }

    public int getData (int i){
        return this.data[i];
    }

    public void setData (int i, int value){
        this.data[i] = value;
    }

}
