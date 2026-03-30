package com.cyberassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Resolved labels and dropdown entries for a question's owner-response profile. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolvedOwnerAnswerUi {
    private Long profileId;
    private String code;
    private String fieldLabel;
    private String fieldHint;
    private List<OwnerAnswerOptionEntryDto> options;
}
