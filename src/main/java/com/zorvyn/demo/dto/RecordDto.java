package com.zorvyn.demo.dto;

import com.zorvyn.demo.entity.RecordType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RecordDto {
    private Long id;
    private BigDecimal amount;
    private RecordType type;
    private String category;
    private LocalDate date;
    private String notes;
    private String createdBy;    // user email
    private Long userId;         // id of user the record belongs to
    private String userName;     // name or email of user the record belongs to
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
