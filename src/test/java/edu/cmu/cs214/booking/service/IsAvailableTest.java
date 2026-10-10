package edu.cmu.cs214.booking.service;

import edu.cmu.cs214.booking.domain.Room;
import edu.cmu.cs214.booking.domain.TimeInterval;
import edu.cmu.cs214.booking.domain.User;
import edu.cmu.cs214.booking.repo.InMemoryBookingStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class IsAvailableTest {

    private final Room roomA = new Room("A", "Room A", 10);
    private final User alice = new User("u1", "Alice");

    @Test
    void notAvailableWhenAnEarlierBookingRunsIntoTheRequest() {
        BookingService svc = new BookingService(new InMemoryBookingStore());
        svc.book(roomA, alice, new TimeInterval(570, 630));
        assertFalse(svc.isAvailable(roomA, new TimeInterval(600, 660)),
            "isAvailable 应该识别出 [570,630) 与 [600,660) 冲突");
    }

    @Test
    void notAvailableWhenAnEarlierBookingContainsTheRequest() {
        BookingService svc = new BookingService(new InMemoryBookingStore());
        svc.book(roomA, alice, new TimeInterval(540, 720));
        assertFalse(svc.isAvailable(roomA, new TimeInterval(600, 660)),
            "isAvailable 应该识别出 [540,720) 与 [600,660) 冲突");
    }
}