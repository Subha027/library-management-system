package com.subhasree.library.model;

/** A book title held by the library, with the number of copies available. */
public class Book {

    private final int id;
    private final String title;
    private final String author;
    private final String isbn;
    private final int totalCopies;
    private final int availableCopies;

    public Book(int id, String title, String author, String isbn, int totalCopies, int availableCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s by %s | ISBN %s | available %d/%d",
                id, title, author, isbn, availableCopies, totalCopies);
    }
}
