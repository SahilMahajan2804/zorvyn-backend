package com.zorvyn.demo.service;

import com.zorvyn.demo.dto.*;
import com.zorvyn.demo.entity.*;
import com.zorvyn.demo.exception.ResourceNotFoundException;
import com.zorvyn.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final FinancialRecordRepository recordRepository;
    private final UserRepository            userRepository;

    // ── CREATE (ADMIN only) ───────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public RecordDto createRecord(CreateRecordRequest req, Authentication auth) {
        User creator = userRepository
                .findByEmailAndDeletedAtIsNull(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        User targetUser = creator;
        if (req.getUserId() != null) {
            targetUser = userRepository.findById(req.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));
        }

        FinancialRecord record = FinancialRecord.builder()
                .amount(req.getAmount())
                .type(req.getType())
                .category(req.getCategory())
                .date(req.getDate())
                .notes(req.getNotes())
                .createdBy(creator)
                .user(targetUser)
                .build();

        return toDto(recordRepository.save(record));
    }

    // ── READ ALL (all roles) — plain list with optional filters ───────────────

    public List<RecordDto> getAllRecords(RecordFilter filter, Authentication auth) {
        Long scopedUserId = resolveUserId(auth, filter.getUserId());
        String category = (filter.getCategory() != null && !filter.getCategory().isBlank())
                ? filter.getCategory().toLowerCase() : null;

        return recordRepository
                .findAllFiltered(scopedUserId, filter.getType(), category, filter.getFrom(), filter.getTo())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ── READ ONE (all roles — Viewer sees own only) ───────────────────────────

    public RecordDto getRecordById(Long id, Authentication auth) {
        FinancialRecord r = findActive(id);
        boolean isViewer = isViewer(auth);

        if (isViewer && r.getUser() != null) {
            User currentUser = userRepository.findByEmailAndDeletedAtIsNull(auth.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            if (!r.getUser().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You can only view your own records");
            }
        }
        return toDto(r);
    }

    // ── UPDATE (ADMIN only) ───────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public RecordDto updateRecord(Long id, UpdateRecordRequest req) {
        FinancialRecord record = findActive(id);

        if (req.getAmount()   != null) record.setAmount(req.getAmount());
        if (req.getType()     != null) record.setType(req.getType());
        if (req.getCategory() != null) record.setCategory(req.getCategory());
        if (req.getDate()     != null) record.setDate(req.getDate());
        if (req.getNotes()    != null) record.setNotes(req.getNotes());

        return toDto(recordRepository.save(record));
    }

    // ── DELETE (ADMIN only, soft) ─────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRecord(Long id) {
        FinancialRecord record = findActive(id);
        record.setDeletedAt(LocalDateTime.now());
        recordRepository.save(record);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Viewers are always locked to their own userId; others use the requested value. */
    Long resolveUserId(Authentication auth, Long requestedUserId) {
        if (auth != null && isViewer(auth)) {
            return userRepository.findByEmailAndDeletedAtIsNull(auth.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        return requestedUserId;
    }

    private boolean isViewer(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VIEWER"));
    }

    private FinancialRecord findActive(Long id) {
        return recordRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found with id: " + id));
    }

    public RecordDto toDto(FinancialRecord r) {
        return RecordDto.builder()
                .id(r.getId())
                .amount(r.getAmount())
                .type(r.getType())
                .category(r.getCategory())
                .date(r.getDate())
                .notes(r.getNotes())
                .createdBy(r.getCreatedBy() != null ? r.getCreatedBy().getEmail() : null)
                .userId(r.getUser() != null ? r.getUser().getId() : null)
                .userName(r.getUser() != null ? r.getUser().getName() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
