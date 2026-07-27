package com.sharecutter.backend.exception;

import java.util.UUID;

public class TransactionNotFoundException
        extends RuntimeException {

    public TransactionNotFoundException(
            UUID transactionId
    ) {
        super(
                "Transaction not found with id: "
                        + transactionId
        );
    }
}