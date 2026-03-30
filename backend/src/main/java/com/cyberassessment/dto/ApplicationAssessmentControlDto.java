package com.cyberassessment.dto;

import com.cyberassessment.entity.ControlFramework;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationAssessmentControlDto {
    private Long id;
    private String controlId;
    private String name;
    private ControlFramework framework;
    private String category;
    @Builder.Default
    private List<String> regulatoryScopes = new ArrayList<>();
    private boolean includedUnderCurrentFilter;
}
