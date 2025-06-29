package com.http200ok.finbuddy.member.domain;

import com.http200ok.finbuddy.account.domain.Account;
import com.http200ok.finbuddy.auth.domain.RefreshToken;
import com.http200ok.finbuddy.budget.domain.Budget;
import com.http200ok.finbuddy.notification.domain.Notification;
import com.http200ok.finbuddy.totp.domain.Otp;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 10)
    private String sex;

    @Column(nullable = false, length = 20)
    private String job;

    @Column(nullable = false, length = 20)
    private String income;

    @Column
    private String phone;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Account> accounts = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Budget> budgets = new ArrayList<>();

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private RefreshToken refreshToken;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private Otp otp;

    @Builder
    public Member(String name, String email, String password, LocalDate birthDate, String sex, String job, String income) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.birthDate = birthDate;
        this.sex = sex;
        this.job = job;
        this.income = income;
    }
}
