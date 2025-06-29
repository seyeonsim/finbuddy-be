package com.http200ok.finbuddy.mail.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailRequestDto {
    private String mail;
    private String code;
}