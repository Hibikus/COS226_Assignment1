import java.util.concurrent.atomic.AtomicBoolean;

public class TTASLock implements Lock{
    
    private final AtomicBoolean state = new AtomicBoolean(false);

    @Override 
    public void lock() {
        while (true) {
            while (state.get()){
                //wait
            }

            //try acquire
            if (!state.getAndSet(true)) {
                return;
            }
        }
    }

    @Override 
    public void unlock() {
        state.set(false);
    }
};
