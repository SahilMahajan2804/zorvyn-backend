package com.zorvyn.demo.controller;

import com.zorvyn.demo.dto.*;
import com.zorvyn.demo.service.RecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordsController {

    private final RecordService recordService;

    /**
     * POST /api/records
     * ADMIN only — create a new record. Set userId to assign to a specific user.
     */
    @PostMapping
    public ResponseEntity<RecordDto> create(
            @Valid @RequestBody CreateRecordRequest req,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordService.createRecord(req, auth));
    }

    /**
     * GET /api/records?type=&category=&from=&to=&userId=
     * All roles — filtered list. Viewer is automatically scoped to own records.
     */
    @GetMapping
    public ResponseEntity<List<RecordDto>> getAll(RecordFilter filter, Authentication auth) {
        return ResponseEntity.ok(recordService.getAllRecords(filter, auth));
    }

    /**
     * GET /api/records/{id}
     * All roles — single record. Viewer can only see own.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecordDto> getById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(recordService.getRecordById(id, auth));
    }

    /**
     * PATCH /api/records/{id}
     * ADMIN only — partial update.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<RecordDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecordRequest req) {
        return ResponseEntity.ok(recordService.updateRecord(id, req));
    }

    /**
     * DELETE /api/records/{id}
     * ADMIN only — soft delete.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}
