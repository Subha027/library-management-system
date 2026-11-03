package com.subhasree.library.model;

/**
 * Base class for any person who interacts with the library.
 * Demonstrates abstraction: subclasses decide their own role.
 */
public abstract class Person {

    private final int id;
    private final String name;
    private final String email;

    protected Person(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public abstract String getRole();

    @Override
    public String toString() {
        return String.format("[%d] %s (%s) - %s", id, name, email, getRole());
    }
}
