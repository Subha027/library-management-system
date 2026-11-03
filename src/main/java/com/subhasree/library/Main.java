package com.subhasree.library;

import com.subhasree.library.db.Database;
import com.subhasree.library.exception.LibraryException;
import com.subhasree.library.model.Book;
import com.subhasree.library.model.Loan;
import com.subhasree.library.service.LibraryService;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Clock;
import java.util.List;

/** Console menu for the Library Management System. */
public class Main {

    private final LibraryService service;
    private final BufferedReader in;

    private Main(LibraryService service, BufferedReader in) {
        this.service = service;
        this.in = in;
    }

    public static void main(String[] args) {
        String dbPath = args.length > 0 ? args[0] : "library.db";
        try (Database database = new Database("jdbc:sqlite:" + dbPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            new Main(new LibraryService(database, Clock.systemDefaultZone()), reader).run();
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
        }
    }

    private void run() throws IOException {
        System.out.println("=== Library Management System ===");
        while (true) {
            printMenu();
            try {
                String choice = ask("Choose an option: ");
                switch (choice) {
                    case "1" -> addBook();
                    case "2" -> registerMember();
                    case "3" -> printBooks(service.listBooks());
                    case "4" -> printBooks(service.searchBooks(ask("Search title or author: ")));
                    case "5" -> issueBook();
                    case "6" -> returnBook();
                    case "7" -> showOverdue();
                    case "8" -> service.listMembers().forEach(System.out::println);
                    case "0" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid option, please try again.");
                }
            } catch (EOFException e) {
                System.out.println("Goodbye!");
                return;
            } catch (LibraryException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1. Add book        5. Issue book");
        System.out.println("2. Register member 6. Return book");
        System.out.println("3. List books      7. Overdue loans");
        System.out.println("4. Search books    8. List members");
        System.out.println("0. Exit");
    }

    private void addBook() throws IOException, LibraryException {
        String title = ask("Title: ");
        String author = ask("Author: ");
        String isbn = ask("ISBN: ");
        int copies = askInt("Number of copies: ");
        int id = service.addBook(title, author, isbn, copies);
        System.out.println("Book added with id " + id + ".");
    }

    private void registerMember() throws IOException, LibraryException {
        String name = ask("Name: ");
        String email = ask("Email: ");
        int id = service.registerMember(name, email);
        System.out.println("Member registered with id " + id + ".");
    }

    private void issueBook() throws IOException, LibraryException {
        int bookId = askInt("Book id: ");
        int memberId = askInt("Member id: ");
        Loan loan = service.issueBook(bookId, memberId);
        System.out.println("Book issued. Due date: " + loan.getDueDate());
    }

    private void returnBook() throws IOException, LibraryException {
        int bookId = askInt("Book id: ");
        int memberId = askInt("Member id: ");
        Loan loan = service.returnBook(bookId, memberId);
        if (loan.getFine() > 0) {
            System.out.println("Book returned late. Fine: Rs " + loan.getFine());
        } else {
            System.out.println("Book returned on time. No fine.");
        }
    }

    private void showOverdue() throws LibraryException {
        List<Loan> overdue = service.listOverdueLoans();
        if (overdue.isEmpty()) {
            System.out.println("No overdue loans.");
        } else {
            overdue.forEach(System.out::println);
        }
    }

    private void printBooks(List<Book> books) {
        if (books.isEmpty()) {
            System.out.println("No books found.");
        } else {
            books.forEach(System.out::println);
        }
    }

    private String ask(String prompt) throws IOException {
        System.out.print(prompt);
        String line = in.readLine();
        if (line == null) {
            throw new EOFException();
        }
        return line.trim();
    }

    private int askInt(String prompt) throws IOException, LibraryException {
        String text = ask(prompt);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new LibraryException("\"" + text + "\" is not a valid number.");
        }
    }
}
