import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class LamportClockTest {
    private LamportClock clock;

    // Create a new LamportClock instance before each test
    @Before
    public void setUp() {
        clock = new LamportClock();
    }

    // Test the increment method, which should increase the clock's value by 1.
    @Test
    public void testIncrement() {
        // Initially, the clock's value should be 0
        assertEquals(0, clock.getValue());

        // After incrementing, the value should be 1
        clock.increment();
        assertEquals(1, clock.getValue());

        // Increment multiple times
        clock.increment();
        clock.increment();
        assertEquals(3, clock.getValue());
    }

    // Test the update method, which updates the clock based on a received value.
    @Test
    public void testUpdate() {
        // Initially, the clock's value should be 0
        assertEquals(0, clock.getValue());

        // Update with a value less than the current value (should not change the value)
        clock.update(0);
        assertEquals(1, clock.getValue());

        // Update with a value greater than the current value
        clock.update(2);
        assertEquals(3, clock.getValue());

        // Update with a value equal to the current value
        clock.update(3);
        assertEquals(4, clock.getValue());
    }
}
