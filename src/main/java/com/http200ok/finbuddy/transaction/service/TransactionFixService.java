package com.http200ok.finbuddy.transaction.service;

import com.http200ok.finbuddy.account.domain.Account;

public interface TransactionFixService {
    void processAllAccounts();
    void processAccountTransactions(Account account);
    void processSpecialCaseAccount(Account account);
}
