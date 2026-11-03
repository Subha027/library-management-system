package com.subhasree.library.model;

import java.time.LocalDate;

/** Records one book issued to one member. */
public class Loan {

    private final int id;
    private final int bookId;
    private final int memberId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final LocalDate returnDate; // null while the book is still out
    private final double fine;

    public Loan(int id, int bookId, int memberId, LocalDate issueDate,
                LocalDate dueDate, LocalDate returnDate, double fine) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fine = fine;
    }

    public int getId() {
        return id;
    }

    public int getBookId() {
        return bookId;
    }

    public int getMemberId() {
        return memberId;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public double getFine() {
        return fine;
    }

    public boolean isReturned() {
        return returnDate != null;
    }

    @Override
    public String toString() {
        return String.format("Loan #%d | book %d -> member %d | issued %s | due %s | %s",
                id, bookId, memberId, issueDate, dueDate,
                isReturned() ? "returned " + returnDate + ", fine Rs " + fine : "not returned");
    }
}
