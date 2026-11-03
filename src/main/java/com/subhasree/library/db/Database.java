package com.subhasree.library.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Opens the SQLite database and creates the tables if they do not exist yet. */
public class Database implements AutoCloseable {

    private final Connection connection;

    /** @param url a JDBC URL, e.g. "jdbc:sqlite:library.db" or "jdbc:sqlite::memory:" */
    public Database(String url) throws SQLException {
        this.connection = DriverManager.getConnection(url);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        createTables();
    }

    public Connection getConnection() {
        return connection;
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS books (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        author TEXT NOT NULL,
                        isbn TEXT NOT NULL UNIQUE,
                        total_copies INTEGER NOT NULL CHECK (total_copies >= 0),
                        available_copies INTEGER NOT NULL CHECK (available_copies >= 0)
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS members (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        email TEXT NOT NULL UNIQUE,
                        membership_date TEXT NOT NULL
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS loans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        book_id INTEGER NOT NULL REFERENCES books(id),
                        member_id INTEGER NOT NULL REFERENCES members(id),
                        issue_date TEXT NOT NULL,
                        due_date TEXT NOT NULL,
                        return_date TEXT,
                        fine REAL NOT NULL DEFAULT 0
                    )""");
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
