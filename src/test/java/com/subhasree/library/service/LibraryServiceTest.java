package com.subhasree.library.service;

import com.subhasree.library.db.Database;
import com.subhasree.library.exception.LibraryException;
import com.subhasree.library.model.Loan;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryServiceTest {

    /** A clock we can move forward, so tests can simulate late returns. */
    private static class MutableClock extends Clock {
        private LocalDate date;

        MutableClock(LocalDate date) {
            this.date = date;
        }

        void setDate(LocalDate date) {
            this.date = date;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return date.atStartOfDay(ZoneOffset.UTC).toInstant();
        }
    }

    private Database database;
    private MutableClock clock;
    private LibraryService service;

    @BeforeEach
    void setUp() throws SQLException {
        database = new Database("jdbc:sqlite::memory:");
        clock = new MutableClock(LocalDate.of(2026, 1, 1));
        service = new LibraryService(database, clock);
    }

    @AfterEach
    void tearDown() throws SQLException {
        database.close();
    }

    @Test
    void issuingABookReducesAvailableCopiesAndSetsDueDate() throws LibraryException {
        int bookId = service.addBook("Clean Code", "Robert Martin", "111", 2);
        int memberId = service.registerMember("Asha", "asha@example.com");

        Loan loan = service.issueBook(bookId, memberId);

        assertEquals(LocalDate.of(2026, 1, 15), loan.getDueDate());
        assertEquals(1, service.listBooks().get(0).getAvailableCopies());
    }

    @Test
    void cannotIssueWhenNoCopiesAreLeft() throws LibraryException {
        int bookId = service.addBook("Effective Java", "Joshua Bloch", "222", 1);
        int first = service.registerMember("Asha", "asha@example.com");
        int second = service.registerMember("Ravi", "ravi@example.com");

        service.issueBook(bookId, first);

        assertThrows(LibraryException.class, () -> service.issueBook(bookId, second));
    }

    @Test
    void memberCannotBorrowMoreThanTheLimit() throws LibraryException {
        int memberId = service.registerMember("Asha", "asha@example.com");
        for (int i = 1; i <= LibraryService.MAX_LOANS_PER_MEMBER; i++) {
            int bookId = service.addBook("Book " + i, "Author", "isbn-" + i, 1);
            service.issueBook(bookId, memberId);
        }
        int extraBook = service.addBook("One too many", "Author", "isbn-extra", 1);

        assertThrows(LibraryException.class, () -> service.issueBook(extraBook, memberId));
    }

    @Test
    void lateReturnChargesFinePerDay() throws LibraryException {
        int bookId = service.addBook("Head First Java", "Kathy Sierra", "333", 1);
        int memberId = service.registerMember("Asha", "asha@example.com");
        service.issueBook(bookId, memberId);

        clock.setDate(LocalDate.of(2026, 1, 20)); // due 15 Jan, so 5 days late
        Loan returned = service.returnBook(bookId, memberId);

        assertEquals(5 * LibraryService.FINE_PER_DAY, returned.getFine());
        assertEquals(1, service.listBooks().get(0).getAvailableCopies());
    }

    @Test
    void returningOnTimeHasNoFine() {
        assertEquals(0.0, LibraryService.calculateFine(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15)));
        assertEquals(0.0, LibraryService.calculateFine(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 10)));
    }

    @Test
    void overdueListShowsOnlyLateUnreturnedLoans() throws LibraryException {
        int bookId = service.addBook("Algorithms", "Sedgewick", "444", 1);
        int memberId = service.registerMember("Asha", "asha@example.com");
        service.issueBook(bookId, memberId);

        assertTrue(service.listOverdueLoans().isEmpty());

        clock.setDate(LocalDate.of(2026, 2, 1));
        assertEquals(1, service.listOverdueLoans().size());
    }

    @Test
    void duplicateIsbnIsRejected() throws LibraryException {
        service.addBook("Book A", "Author", "555", 1);

        assertThrows(LibraryException.class, () -> service.addBook("Book B", "Author", "555", 1));
    }
}
