public class LamportClock {
    private int value = 0;

    /**
     * Increments the Lamport clock value by 1.
     */
    public synchronized void increment() {
        value++;
    }

    /**
     * Updates the Lamport clock value based on the received value.
     *
     * @param receivedValue The received Lamport clock value.
     */
    public synchronized void update(int receivedValue) {
        value = Math.max(value, receivedValue + 1);
    }

    /**
     * Retrieves the current Lamport clock value.
     *
     * @return The current Lamport clock value.
     */
    public synchronized int getValue() {
        return value;
    }
}