package com.cyberassessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionControlId implements Serializable {

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "control_id")
    private Long controlId;
}
