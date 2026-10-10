package edu.cmu.cs214.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import edu.cmu.cs214.booking.domain.Booking;
import edu.cmu.cs214.booking.domain.Room;
import edu.cmu.cs214.booking.domain.TimeInterval;
import edu.cmu.cs214.booking.domain.User;
import edu.cmu.cs214.booking.repo.InMemoryBookingStore;
import java.util.List;
import org.junit.jupiter.api.Test;

class CancelBookingTest {

    private final Room roomA = new Room("A", "Alpha", 10);
    private final Room roomB = new Room("B", "Beta", 4);
    private final User alice = new User("u1", "Alice");
    private final User bob = new User("u2", "Bob");
    private final User carol = new User("u3", "Carol");
    private final User dave = new User("u4", "Dave");

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

    @Test
    void cancellingPromotesTheWaitlistedUser() {
        newService();
        BookingResult first = svc.book(roomA, alice, new TimeInterval(600, 660));
        Booking a = assertInstanceOf(BookingResult.Confirmed.class, first).booking();
        assertInstanceOf(BookingResult.Waitlisted.class,
                svc.book(roomA, bob, new TimeInterval(630, 700)));

        svc.cancelBooking(a.id());

        List<Booking> bookings = svc.listBookings(roomA);
        assertEquals(1, bookings.size());
        assertEquals(bob, bookings.get(0).user());
        assertEquals(new TimeInterval(630, 700), bookings.get(0).interval());
        assertEquals(0, store.waitlistForRoom(roomA).size());
    }

    // S4: FCFS + at most one promotion
    @Test
    void promoteAtMostOneWaiterByArrivalOrder() {
        newService();
        BookingResult first = svc.book(roomA, alice, new TimeInterval(600, 720));
        Booking a = assertInstanceOf(BookingResult.Confirmed.class, first).booking();
        assertInstanceOf(BookingResult.Waitlisted.class,
                svc.book(roomA, bob, new TimeInterval(600, 660)));
        assertInstanceOf(BookingResult.Waitlisted.class,
                svc.book(roomA, carol, new TimeInterval(660, 700)));

        svc.cancelBooking(a.id());

        List<Booking> bookings = svc.listBookings(roomA);
        assertEquals(1, bookings.size());
        assertEquals(bob, bookings.get(0).user());
        assertEquals(1, store.waitlistForRoom(roomA).size());
        assertEquals(carol, store.waitlistForRoom(roomA).get(0).user());
    }

    // S5: skip a waiter whose interval still conflicts
    @Test
    void skipConflictingWaiterAndPromoteNext() {
        newService();
        BookingResult r1 = svc.book(roomA, alice, new TimeInterval(600, 660));
        Booking a = assertInstanceOf(BookingResult.Confirmed.class, r1).booking();
        BookingResult r2 = svc.book(roomA, dave, new TimeInterval(700, 760));
        Booking d = assertInstanceOf(BookingResult.Confirmed.class, r2).booking();
        assertInstanceOf(BookingResult.Waitlisted.class,
                svc.book(roomA, bob, new TimeInterval(630, 730)));
        assertInstanceOf(BookingResult.Waitlisted.class,
                svc.book(roomA, carol, new TimeInterval(610, 650)));

        svc.cancelBooking(a.id());

        List<Booking> bookings = svc.listBookings(roomA);
        assertEquals(2, bookings.size());
        boolean carolPromoted = bookings.stream()
                .anyMatch(b -> b.user().equals(carol)
                        && b.interval().equals(new TimeInterval(610, 650)));
        assertEquals(true, carolPromoted);
        assertEquals(1, store.waitlistForRoom(roomA).size());
        assertEquals(bob, store.waitlistForRoom(roomA).get(0).user());
    }

    // S6: promotion does not cross rooms
    @Test
    void promotionStaysWithinTheSameRoom() {
        newService();
        BookingResult r1 = svc.book(roomA, alice, new TimeInterval(600, 660));
        Booking a = assertInstanceOf(BookingResult.Confirmed.class, r1).booking();
        svc.book(roomB, dave, new TimeInterval(600, 660));
        assertInstanceOf(BookingResult.Waitlisted.class,
                svc.book(roomB, bob, new TimeInterval(630, 700)));

        svc.cancelBooking(a.id());

        assertEquals(0, svc.listBookings(roomA).size());
        assertEquals(1, store.waitlistForRoom(roomB).size());
    }
}
