package com.cyberassessment.service;

import com.cyberassessment.dto.QuestionnaireTemplateDto;
import com.cyberassessment.dto.QuestionnaireTemplateItemDto;
import com.cyberassessment.entity.*;
import com.cyberassessment.repository.QuestionControlMappingRepository;
import com.cyberassessment.repository.QuestionnaireTemplateItemRepository;
import com.cyberassessment.repository.QuestionnaireTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionnaireTemplateService {
    private final QuestionnaireTemplateRepository questionnaireTemplateRepository;
    private final QuestionnaireTemplateItemRepository questionnaireTemplateItemRepository;
    private final QuestionControlMappingRepository questionControlMappingRepository;
    private final CurrentUserService currentUserService;

    public static QuestionnaireTemplateDto toDto(QuestionnaireTemplate t) {
        User createdBy = t.getCreatedBy();
        User publishedBy = t.getPublishedBy();
        return QuestionnaireTemplateDto.builder()
                .id(t.getId())
                .versionNo(t.getVersionNo())
                .status(t.getStatus())
                .notes(t.getNotes())
                .createdAt(t.getCreatedAt())
                .publishedAt(t.getPublishedAt())
                .createdByUserId(createdBy != null ? createdBy.getId() : null)
                .createdByEmail(createdBy != null ? createdBy.getEmail() : null)
                .publishedByUserId(publishedBy != null ? publishedBy.getId() : null)
                .publishedByEmail(publishedBy != null ? publishedBy.getEmail() : null)
                .itemCount(t.getItems() != null ? t.getItems().size() : 0)
                .build();
    }

    public static QuestionnaireTemplateItemDto toItemDto(QuestionnaireTemplateItem i) {
        return QuestionnaireTemplateItemDto.builder()
                .id(i.getId())
                .templateId(i.getTemplate().getId())
                .questionId(i.getQuestion().getId())
                .controlId(i.getControl().getId())
                .controlControlId(i.getControl().getControlId())
                .controlName(i.getControl().getName())
                .questionText(i.getQuestionText())
                .helpText(i.getHelpText())
                .displayOrder(i.getDisplayOrder())
                .askOwner(i.getAskOwner())
                .mappingRationale(i.getMappingRationale())
                .mappingWeight(i.getMappingWeight())
                .effectiveFrom(i.getEffectiveFrom())
                .effectiveTo(i.getEffectiveTo())
                .build();
    }

    @Transactional(readOnly = true)
    public List<QuestionnaireTemplateDto> listTemplates() {
        return questionnaireTemplateRepository.findAllByOrderByVersionNoDesc().stream().map(QuestionnaireTemplateService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionnaireTemplateItemDto> listItems(Long templateId) {
        return questionnaireTemplateItemRepository.findByTemplateIdOrderByDisplayOrderAscControlControlIdAsc(templateId).stream()
                .map(QuestionnaireTemplateService::toItemDto)
                .toList();
    }

    @Transactional
    public QuestionnaireTemplateDto createDraftFromCurrent(String notes) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can create questionnaire templates");
        }
        int nextVersion = questionnaireTemplateRepository.findTopByOrderByVersionNoDesc().map(t -> t.getVersionNo() + 1).orElse(1);
        QuestionnaireTemplate template = QuestionnaireTemplate.builder()
                .versionNo(nextVersion)
                .status(QuestionnaireTemplateStatus.DRAFT)
                .notes(notes)
                .createdBy(currentUserService.getCurrentUser().orElse(null))
                .build();
        template = questionnaireTemplateRepository.save(template);
        copyCurrentMappingsIntoTemplate(template);
        return toDto(template);
    }

    @Transactional
    public QuestionnaireTemplateDto bootstrapInitialFromCurrent(String notes) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can initialize questionnaire templates");
        }
        if (questionnaireTemplateRepository.count() > 0) {
            throw new IllegalArgumentException("Templates already exist");
        }
        QuestionnaireTemplate template = QuestionnaireTemplate.builder()
                .versionNo(1)
                .status(QuestionnaireTemplateStatus.PUBLISHED)
                .notes((notes == null || notes.isBlank()) ? "Initial baseline snapshot from live mappings." : notes)
                .createdBy(currentUserService.getCurrentUser().orElse(null))
                .publishedBy(currentUserService.getCurrentUser().orElse(null))
                .publishedAt(Instant.now())
                .build();
        template = questionnaireTemplateRepository.save(template);
        copyCurrentMappingsIntoTemplate(template);
        return toDto(template);
    }

    @Transactional
    public QuestionnaireTemplateDto publish(Long templateId) {
        if (!currentUserService.isAdmin()) {
            throw new IllegalArgumentException("Only admins can publish templates");
        }
        QuestionnaireTemplate template = questionnaireTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        questionnaireTemplateRepository.findTopByStatusOrderByVersionNoDesc(QuestionnaireTemplateStatus.PUBLISHED)
                .ifPresent(existing -> {
                    existing.setStatus(QuestionnaireTemplateStatus.ARCHIVED);
                    questionnaireTemplateRepository.save(existing);
                });
        template.setStatus(QuestionnaireTemplateStatus.PUBLISHED);
        template.setPublishedAt(Instant.now());
        template.setPublishedBy(currentUserService.getCurrentUser().orElse(null));
        template = questionnaireTemplateRepository.save(template);
        return toDto(template);
    }

    @Transactional(readOnly = true)
    public QuestionnaireTemplate findLatestPublishedEntity() {
        return questionnaireTemplateRepository.findTopByStatusOrderByVersionNoDesc(QuestionnaireTemplateStatus.PUBLISHED).orElse(null);
    }

    private void copyCurrentMappingsIntoTemplate(QuestionnaireTemplate template) {
        List<QuestionControlMapping> mappings = questionControlMappingRepository.findAll();
        for (QuestionControlMapping m : mappings) {
            QuestionnaireTemplateItem item = QuestionnaireTemplateItem.builder()
                    .template(template)
                    .question(m.getQuestion())
                    .control(m.getControl())
                    .questionText(m.getQuestion().getQuestionText())
                    .helpText(m.getQuestion().getHelpText())
                    .displayOrder(m.getQuestion().getDisplayOrder())
                    .askOwner(Boolean.TRUE.equals(m.getQuestion().getAskOwner()))
                    .mappingRationale(m.getMappingRationale())
                    .mappingWeight(m.getMappingWeight())
                    .effectiveFrom(m.getEffectiveFrom())
                    .effectiveTo(m.getEffectiveTo())
                    .build();
            questionnaireTemplateItemRepository.save(item);
        }
    }
}
