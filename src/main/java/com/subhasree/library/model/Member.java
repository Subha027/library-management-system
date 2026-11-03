package com.subhasree.library.model;

import java.time.LocalDate;

/** A registered library member (inherits from {@link Person}). */
public class Member extends Person {

    private final LocalDate membershipDate;

    public Member(int id, String name, String email, LocalDate membershipDate) {
        super(id, name, email);
        this.membershipDate = membershipDate;
    }

    public LocalDate getMembershipDate() {
        return membershipDate;
    }

    @Override
    public String getRole() {
        return "Member";
    }
}
