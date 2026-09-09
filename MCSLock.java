import java.util.concurrent.atomic.AtomicReference;

public class MCSLock implements Lock {
    class QNode {
        volatile boolean locked = false;
        QNode next = null;
    }

    AtomicReference<QNode> tail;
    ThreadLocal<QNode> myNode;

    public MCSLock() {
        tail = new AtomicReference<QNode>(null);
        myNode = new ThreadLocal<QNode>() {
            protected QNode initialValue() {
                return new QNode();
            }
        };
    }

    public void lock() {
        QNode qnode = myNode.get();
        QNode pred = tail.getAndSet(qnode);
        if (pred != null) {
            qnode.locked = true;
            pred.next = qnode;
            while (qnode.locked){} //wait until pred gives up lock
        }
    }

    public void unlock() {
        QNode qnode = myNode.get();
        if (qnode.next == null) {
            if (tail.compareAndSet(qnode, null)){
                return;
            }
            while (qnode.next == null) {} //wait until succ fills next field
        }
        qnode.next.locked = false;
        qnode.next = null;
    }

}
