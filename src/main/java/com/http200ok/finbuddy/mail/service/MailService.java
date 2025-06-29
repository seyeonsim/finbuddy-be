package com.http200ok.finbuddy.mail.service;

public interface MailService {
    void sendMail(String mail);
    boolean verifyCode(String mail, String userCode);
}
