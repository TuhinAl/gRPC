package com.altuhin.grpc.sec06.requestHandler;

import com.altuhin.grpc.sec06.repository.AccountRepository;
import com.altuhin.models.sec06.AccountBalance;
import com.altuhin.models.sec06.TransferRequest;
import com.altuhin.models.sec06.TransferResponse;
import com.altuhin.models.sec06.TransferStatus;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferRequestHandler implements StreamObserver<TransferRequest> {

    private static final Logger log = LoggerFactory.getLogger(TransferRequestHandler.class);
    private final StreamObserver<TransferResponse> responseObserver;

    public TransferRequestHandler(StreamObserver<TransferResponse> responseObserver) {
        this.responseObserver = responseObserver;
    }

    @Override
    public void onNext(TransferRequest transferRequest) {
        var status = this.transfer(transferRequest);
        var response = TransferResponse.newBuilder()
                .setFromAccount(this.toAccountBalance(transferRequest.getFromAccount()))
                .setToAccount(this.toAccountBalance(transferRequest.getToAccount()))
                .setStatus(status)
                .build();
        this.responseObserver.onNext(response);

    }

    @Override
    public void onError(Throwable throwable) {
        log.info("client error. {}", throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        log.info("transfer request stream completed.");
        this.responseObserver.onCompleted();
    }

    private TransferStatus transfer(TransferRequest transferRequest) {
        var amount = transferRequest.getAmount();
        var fromAccount = transferRequest.getFromAccount();
        var toAccount = transferRequest.getToAccount();
        var status = TransferStatus.REJECTED;
        if (AccountRepository.getBalance(fromAccount) >= amount && (fromAccount != toAccount)) {
            AccountRepository.deductAmount(fromAccount, amount);
            AccountRepository.addAmount(toAccount, amount);
            status = TransferStatus.COMPLETED;
        }
        return status;
    }

    private AccountBalance toAccountBalance(int accountNumber) {
        return AccountBalance.newBuilder()
                .setAccountNumber(accountNumber)
                .setBalance(AccountRepository.getBalance(accountNumber))
                .build();
    }
}
