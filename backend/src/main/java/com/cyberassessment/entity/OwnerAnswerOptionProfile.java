package com.cyberassessment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "owner_answer_option_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerAnswerOptionProfile {

    public static final String DEFAULT_CODE = "DEFAULT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "field_label", length = 500)
    private String fieldLabel;

    @Column(name = "field_hint", length = 2000)
    private String fieldHint;

    @Lob
    @Column(name = "options_json", nullable = false)
    private String optionsJson;
}
