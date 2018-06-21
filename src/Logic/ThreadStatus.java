package Logic;

public enum ThreadStatus {
    Running,
    Waiting,
    DataCacheFail,
    DataCacheFailRunning,
    InstructionCacheFail,
    InstructionCacheFailRunning,
    Null,
    Finished
}
