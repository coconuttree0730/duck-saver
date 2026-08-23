package com.duck.saver.account.service;

import com.duck.saver.account.dto.AccountResponse;
import com.duck.saver.account.dto.CreateAccountRequest;
import com.duck.saver.account.dto.TransactionItemRequest;
import com.duck.saver.account.dto.UpdateAccountRequest;

public interface AccountService {

	AccountResponse findByName(String accountName);

	AccountResponse create(CreateAccountRequest request);

	void update(String name, UpdateAccountRequest request);

	void delete(String name);

	void addItem(String name, TransactionItemRequest request);

	void deleteItem(String name, String itemId);

	AccountResponse demo();
}
