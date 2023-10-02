public class LamportClock {
    private int value = 0;

    public synchronized void increment() {
        value++;
    }

    public synchronized void update(int receivedValue) {
        value = Math.max(value, receivedValue + 1);
    }

    public synchronized int getValue() {
        return value;
    }
}