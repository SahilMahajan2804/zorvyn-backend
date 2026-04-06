package com.zorvyn.demo.dto;

import com.zorvyn.demo.entity.RecordType;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;


@Data @NoArgsConstructor @AllArgsConstructor
public class RecordFilter {

    private RecordType type;

    private String category;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    private Long userId;
}
