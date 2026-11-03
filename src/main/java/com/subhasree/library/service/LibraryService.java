package com.subhasree.library.service;

import com.subhasree.library.dao.BookDao;
import com.subhasree.library.dao.LoanDao;
import com.subhasree.library.dao.MemberDao;
import com.subhasree.library.db.Database;
import com.subhasree.library.exception.LibraryException;
import com.subhasree.library.model.Book;
import com.subhasree.library.model.Loan;
import com.subhasree.library.model.Member;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Business rules of the library. The menu (Main) only talks to this class,
 * and this class is the only one that talks to the DAOs.
 */
public class LibraryService {

    public static final int LOAN_DAYS = 14;
    public static final int MAX_LOANS_PER_MEMBER = 3;
    public static final double FINE_PER_DAY = 2.0;

    private final Connection connection;
    private final BookDao bookDao;
    private final MemberDao memberDao;
    private final LoanDao loanDao;
    private final Clock clock;

    public LibraryService(Database database, Clock clock) {
        this.connection = database.getConnection();
        this.bookDao = new BookDao(connection);
        this.memberDao = new MemberDao(connection);
        this.loanDao = new LoanDao(connection);
        this.clock = clock;
    }

    public int addBook(String title, String author, String isbn, int copies) throws LibraryException {
        if (title.isBlank() || author.isBlank() || isbn.isBlank()) {
            throw new LibraryException("Title, author and ISBN cannot be empty.");
        }
        if (copies < 1) {
            throw new LibraryException("A book needs at least one copy.");
        }
        try {
            return bookDao.insert(title, author, isbn, copies);
        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                throw new LibraryException("A book with ISBN " + isbn + " already exists.");
            }
            throw new LibraryException("Could not add the book.", e);
        }
    }

    public int registerMember(String name, String email) throws LibraryException {
        if (name.isBlank()) {
            throw new LibraryException("Name cannot be empty.");
        }
        if (!email.contains("@")) {
            throw new LibraryException("Please enter a valid email address.");
        }
        try {
            return memberDao.insert(name, email, LocalDate.now(clock));
        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                throw new LibraryException("A member with email " + email + " already exists.");
            }
            throw new LibraryException("Could not register the member.", e);
        }
    }

    /**
     * Issues a book. The loan row and the copy count are updated in one
     * database transaction, so either both change or neither does.
     */
    public Loan issueBook(int bookId, int memberId) throws LibraryException {
        try {
            connection.setAutoCommit(false);
            Book book = bookDao.findById(bookId)
                    .orElseThrow(() -> new LibraryException("No book with id " + bookId + "."));
            Member member = memberDao.findById(memberId)
                    .orElseThrow(() -> new LibraryException("No member with id " + memberId + "."));

            if (book.getAvailableCopies() < 1) {
                throw new LibraryException("No copies of \"" + book.getTitle() + "\" are available.");
            }
            if (loanDao.findOpenLoan(bookId, memberId).isPresent()) {
                throw new LibraryException(member.getName() + " already has this book.");
            }
            if (loanDao.countOpenLoans(memberId) >= MAX_LOANS_PER_MEMBER) {
                throw new LibraryException(member.getName() + " has reached the limit of "
                        + MAX_LOANS_PER_MEMBER + " books.");
            }

            LocalDate today = LocalDate.now(clock);
            LocalDate due = today.plusDays(LOAN_DAYS);
            int loanId = loanDao.insert(bookId, memberId, today, due);
            bookDao.adjustAvailableCopies(bookId, -1);
            connection.commit();
            return new Loan(loanId, bookId, memberId, today, due, null, 0);
        } catch (LibraryException e) {
            rollbackQuietly();
            throw e;
        } catch (SQLException e) {
            rollbackQuietly();
            throw new LibraryException("Could not issue the book.", e);
        } finally {
            restoreAutoCommit();
        }
    }

    /** Returns a book and calculates any late fine. */
    public Loan returnBook(int bookId, int memberId) throws LibraryException {
        try {
            connection.setAutoCommit(false);
            Loan loan = loanDao.findOpenLoan(bookId, memberId)
                    .orElseThrow(() -> new LibraryException("No open loan found for this book and member."));

            LocalDate today = LocalDate.now(clock);
            double fine = calculateFine(loan.getDueDate(), today);
            loanDao.markReturned(loan.getId(), today, fine);
            bookDao.adjustAvailableCopies(bookId, 1);
            connection.commit();
            return new Loan(loan.getId(), bookId, memberId, loan.getIssueDate(), loan.getDueDate(), today, fine);
        } catch (LibraryException e) {
            rollbackQuietly();
            throw e;
        } catch (SQLException e) {
            rollbackQuietly();
            throw new LibraryException("Could not return the book.", e);
        } finally {
            restoreAutoCommit();
        }
    }

    /** Fine is FINE_PER_DAY for every day after the due date; zero if returned on time. */
    public static double calculateFine(LocalDate dueDate, LocalDate returnDate) {
        long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate);
        return daysLate > 0 ? daysLate * FINE_PER_DAY : 0.0;
    }

    public List<Book> listBooks() throws LibraryException {
        try {
            return bookDao.findAll();
        } catch (SQLException e) {
            throw new LibraryException("Could not read books.", e);
        }
    }

    public List<Book> searchBooks(String keyword) throws LibraryException {
        try {
            return bookDao.search(keyword);
        } catch (SQLException e) {
            throw new LibraryException("Could not search books.", e);
        }
    }

    public List<Member> listMembers() throws LibraryException {
        try {
            return memberDao.findAll();
        } catch (SQLException e) {
            throw new LibraryException("Could not read members.", e);
        }
    }

    public List<Loan> listOverdueLoans() throws LibraryException {
        try {
            return loanDao.findOverdue(LocalDate.now(clock));
        } catch (SQLException e) {
            throw new LibraryException("Could not read overdue loans.", e);
        }
    }

    private boolean isUniqueViolation(SQLException e) {
        return e.getMessage() != null && e.getMessage().contains("UNIQUE");
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // nothing more we can do; the original error is reported to the user
        }
    }

    private void restoreAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // connection is closed or broken; nothing to restore
        }
    }
}
