package com.cyberassessment.dto;

import com.cyberassessment.entity.ControlFramework;
import com.cyberassessment.model.ControlResponderAudience;
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
public class ControlDto {
    private Long id;
    private String controlId;
    private String name;
    private String description;
    private ControlFramework framework;
    private Boolean enabled;
    private String category;
    /** Derived from linked questions' ask_owner flags. */
    private ControlResponderAudience responderAudience;
    @Builder.Default
    private List<QuestionDto> questions = new ArrayList<>();
}
