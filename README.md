# Library Management System

A console-based library management application written in **Java**. It keeps books, members and loans in a **SQLite** database through **JDBC**, and applies real library rules such as due dates, a loan limit and late fines.

## Features

- Add books (with number of copies) and register members
- List all books or search by title / author
- Issue a book to a member (14-day loan period)
- Return a book and calculate the late fine automatically
- View all overdue loans
- Rules enforced: no issue when no copies are left, maximum 3 books per member, no duplicate ISBN or email, no issuing the same book twice to one member

## Concepts used

| Area | Where to look |
| --- | --- |
| OOP: abstraction and inheritance | `Person` (abstract) and `Member` (extends `Person`) |
| OOP: encapsulation | Private fields with getters in the `model` package |
| Custom exceptions | `LibraryException` |
| Layered design (model, DAO, service, UI) | One package per layer |
| JDBC with `PreparedStatement` (prevents SQL injection) | `dao` package |
| SQL: tables, foreign keys, constraints, joins of ideas via ids | `Database.java` |
| Database transactions (commit / rollback) | `issueBook` and `returnBook` in `LibraryService` |
| Unit testing with JUnit 5 and a controllable clock | `LibraryServiceTest` |

## Project structure

```
src/main/java/com/subhasree/library/
├── Main.java                 console menu
├── model/                    Person, Member, Book, Loan
├── dao/                      BookDao, MemberDao, LoanDao (all SQL)
├── db/Database.java          connection and table creation
├── service/LibraryService.java   business rules
└── exception/LibraryException.java
src/test/java/.../LibraryServiceTest.java
```

## Requirements

- JDK 17 or newer
- Maven 3.8 or newer

## Build and run

```bash
mvn clean package
java -jar target/library-management-system-1.0.0.jar
```

The first run creates a `library.db` file in the current folder. To use a different file, pass its name:

```bash
java -jar target/library-management-system-1.0.0.jar mylibrary.db
```

## Run the tests

```bash
mvn test
```

## Menu

```
1. Add book        5. Issue book
2. Register member 6. Return book
3. List books      7. Overdue loans
4. Search books    8. List members
0. Exit
```

## Rules at a glance

- Loan period: 14 days
- Maximum books per member: 3
- Fine: Rs 2 per day after the due date

These values are constants at the top of `LibraryService.java`.

## Possible improvements

- Reserve a book that has no copies available
- Delete or update books and members
- Replace the console menu with a REST API (Spring Boot) or a web page
- Add more tests for the DAO classes
