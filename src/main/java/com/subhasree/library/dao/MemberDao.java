package com.subhasree.library.dao;

import com.subhasree.library.model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** All SQL for the members table. */
public class MemberDao {

    private final Connection connection;

    public MemberDao(Connection connection) {
        this.connection = connection;
    }

    public int insert(String name, String email, LocalDate joined) throws SQLException {
        String sql = "INSERT INTO members (name, email, membership_date) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, joined.toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public Optional<Member> findById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM members WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Member> findAll() throws SQLException {
        List<Member> members = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM members ORDER BY name")) {
            while (rs.next()) {
                members.add(map(rs));
            }
        }
        return members;
    }

    private Member map(ResultSet rs) throws SQLException {
        return new Member(rs.getInt("id"), rs.getString("name"), rs.getString("email"),
                LocalDate.parse(rs.getString("membership_date")));
    }
}
