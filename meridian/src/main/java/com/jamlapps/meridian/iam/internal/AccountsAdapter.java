package com.jamlapps.meridian.iam.internal;

import com.jamlapps.meridian.iam.Account;
import com.jamlapps.meridian.iam.Accounts;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The read-only view of accounts that other modules hold. */
@Component
class AccountsAdapter implements Accounts {

    private final AccountRepository repository;

    AccountsAdapter(AccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findById(UUID accountId) {
        return repository.findById(accountId).map(AccountEntity::toAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID accountId) {
        return repository.existsById(accountId);
    }
}
