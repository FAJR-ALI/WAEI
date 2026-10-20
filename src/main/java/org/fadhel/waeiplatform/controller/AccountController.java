package org.fadhel.waeiplatform.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fadhel.waeiplatform.api.ApiResponse;
import org.fadhel.waeiplatform.dto.in.AccountDTOIn;
import org.fadhel.waeiplatform.dto.in.AccountUpdateDTOIn;
import org.fadhel.waeiplatform.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // GET ALL ACCOUNTS
    @GetMapping("/get")
    public ResponseEntity<?> getAllAccounts() {

        return ResponseEntity.status(200).body(
                accountService.getAllAccounts());
    }

    // ADD ACCOUNT
    @PostMapping("/add")
    public ResponseEntity<?> addAccount(
            @Valid @RequestBody AccountDTOIn accountDTOIn) {

        accountService.addAccount(accountDTOIn);

        return ResponseEntity.status(200).body(
                new ApiResponse("Account added successfully"));
    }

    // UPDATE ACCOUNT
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateAccount(
            @PathVariable Integer id,
            @Valid @RequestBody AccountUpdateDTOIn accountUpdateDTOIn) {

        accountService.updateAccount(id, accountUpdateDTOIn);

        return ResponseEntity.status(200).body(
                new ApiResponse("Account updated successfully"));
    }

    // DELETE ACCOUNT
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteAccount(
            @PathVariable Integer id) {

        accountService.deleteAccount(id);

        return ResponseEntity.status(200).body(
                new ApiResponse("Account deleted successfully"));
    }
}
