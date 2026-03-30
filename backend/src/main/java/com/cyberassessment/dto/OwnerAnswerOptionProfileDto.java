package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerAnswerOptionProfileDto {
    private Long id;
    private String code;
    private String displayName;
    private String fieldLabel;
    private String fieldHint;
    /** Raw JSON array of {value,label}; editable in admin UI. */
    private String optionsJson;
}
