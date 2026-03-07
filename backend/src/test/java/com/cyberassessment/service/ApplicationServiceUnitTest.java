package com.cyberassessment.service;

import com.cyberassessment.dto.ApplicationDto;
import com.cyberassessment.entity.Application;
import com.cyberassessment.entity.User;
import com.cyberassessment.repository.ApplicationRepository;
import com.cyberassessment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceUnitTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void createAndFindByIdWork() {
        User owner = User.builder().id(2L).email("owner@test.com").displayName("Owner").build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> {
            Application a = i.getArgument(0);
            a.setId(9L);
            return a;
        });
        when(applicationRepository.findById(9L)).thenReturn(Optional.of(
                Application.builder().id(9L).name("CRM").description("desc").owner(owner).build()
        ));

        ApplicationDto created = applicationService.create("CRM", "desc", 2L);
        ApplicationDto found = applicationService.findById(9L);

        assertThat(created.getId()).isEqualTo(9L);
        assertThat(created.getOwnerId()).isEqualTo(2L);
        assertThat(found.getName()).isEqualTo("CRM");
    }

    @Test
    void updateAndDeleteHandleBranches() {
        Application app = Application.builder().id(1L).name("Old").description("x").build();
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
        when(applicationRepository.existsById(1L)).thenReturn(true);
        when(applicationRepository.existsById(404L)).thenReturn(false);
        when(applicationRepository.findAll()).thenReturn(List.of(app));

        ApplicationDto updated = applicationService.update(1L, "New", null, null);
        List<ApplicationDto> all = applicationService.findAll();
        applicationService.deleteById(1L);

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(all).hasSize(1);
        verify(applicationRepository).deleteById(1L);
        assertThatThrownBy(() -> applicationService.deleteById(404L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
