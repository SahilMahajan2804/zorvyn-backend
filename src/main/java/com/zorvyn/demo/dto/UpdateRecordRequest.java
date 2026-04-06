package com.zorvyn.demo.dto;

import com.zorvyn.demo.entity.RecordType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateRecordRequest {

    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Digits(integer = 15, fraction = 2, message = "Amount format invalid")
    private BigDecimal amount;

    private RecordType type;

    @Size(max = 100, message = "Category must be 100 characters or less")
    private String category;

    private LocalDate date;

    @Size(max = 1000, message = "Notes must be 1000 characters or less")
    private String notes;
}
