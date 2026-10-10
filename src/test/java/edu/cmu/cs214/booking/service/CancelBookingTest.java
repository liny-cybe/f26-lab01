package edu.cmu.cs214.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import edu.cmu.cs214.booking.domain.Booking;
import edu.cmu.cs214.booking.domain.Room;
import edu.cmu.cs214.booking.domain.TimeInterval;
import edu.cmu.cs214.booking.domain.User;
import edu.cmu.cs214.booking.repo.InMemoryBookingStore;
import org.junit.jupiter.api.Test;

class CancelBookingTest {

    private final Room roomA = new Room("A", "Alpha", 10);
    private final Room roomB = new Room("B", "Beta", 4);
    private final User alice = new User("u1", "Alice");
    private final User bob = new User("u2", "Bob");

    private InMemoryBookingStore store;
    private BookingService svc;

    private BookingService newService() {
        store = new InMemoryBookingStore();
        svc = new BookingService(store);
        return svc;
    }

    @Test
    void cancelRemovesTheBooking() {
        newService();
        BookingResult r = svc.book(roomA, alice, new TimeInterval(600, 660));
        Booking booking = assertInstanceOf(BookingResult.Confirmed.class, r).booking();

        svc.cancelBooking(booking.id());

        assertEquals(0, svc.listBookings(roomA).size());
    }

    @Test
    void cancelUnknownIdDoesNothing() {
        newService();
        svc.book(roomA, alice, new TimeInterval(600, 660));
        svc.book(roomA, bob, new TimeInterval(630, 700));

        svc.cancelBooking("nonexistent");

        assertEquals(1, svc.listBookings(roomA).size());
        assertEquals(1, store.waitlistForRoom(roomA).size());
    }
}
