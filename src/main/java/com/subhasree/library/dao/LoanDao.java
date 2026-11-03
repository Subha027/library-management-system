package com.subhasree.library.dao;

import com.subhasree.library.model.Loan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** All SQL for the loans table. */
public class LoanDao {

    private final Connection connection;

    public LoanDao(Connection connection) {
        this.connection = connection;
    }

    public int insert(int bookId, int memberId, LocalDate issued, LocalDate due) throws SQLException {
        String sql = "INSERT INTO loans (book_id, member_id, issue_date, due_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, bookId);
            ps.setInt(2, memberId);
            ps.setString(3, issued.toString());
            ps.setString(4, due.toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public Optional<Loan> findOpenLoan(int bookId, int memberId) throws SQLException {
        String sql = "SELECT * FROM loans WHERE book_id = ? AND member_id = ? AND return_date IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            ps.setInt(2, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public int countOpenLoans(int memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE member_id = ? AND return_date IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Loans that are not returned and whose due date is before {@code today}. */
    public List<Loan> findOverdue(LocalDate today) throws SQLException {
        String sql = "SELECT * FROM loans WHERE return_date IS NULL AND due_date < ? ORDER BY due_date";
        List<Loan> loans = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, today.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    loans.add(map(rs));
                }
            }
        }
        return loans;
    }

    public void markReturned(int loanId, LocalDate returned, double fine) throws SQLException {
        String sql = "UPDATE loans SET return_date = ?, fine = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, returned.toString());
            ps.setDouble(2, fine);
            ps.setInt(3, loanId);
            ps.executeUpdate();
        }
    }

    private Loan map(ResultSet rs) throws SQLException {
        String returned = rs.getString("return_date");
        return new Loan(rs.getInt("id"), rs.getInt("book_id"), rs.getInt("member_id"),
                LocalDate.parse(rs.getString("issue_date")), LocalDate.parse(rs.getString("due_date")),
                returned == null ? null : LocalDate.parse(returned), rs.getDouble("fine"));
    }
}
