package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegulationDto {
    private Long id;
    private String code;
    private String name;
    private String version;
    private String description;
    private Boolean active;
}
