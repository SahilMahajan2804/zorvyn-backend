package com.zorvyn.demo.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryTotal {
    private String category;
    private String type;
    private BigDecimal total;
}
