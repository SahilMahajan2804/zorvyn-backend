package com.zorvyn.demo.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MonthlyTrend {
    private int year;
    private int month;
    private String type;
    private BigDecimal total;
}
