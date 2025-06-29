package com.http200ok.finbuddy.member.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequest {
    private String name;
    private String email;
    private String password;
    private LocalDate birthDate;
    private String sex;
    private String job;
    private String income;
}
