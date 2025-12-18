package in.bored.api.service;

import in.bored.api.model.HealthWellnessCategory;
import in.bored.api.model.HealthWellnessContent;
import in.bored.api.model.UserHealthWellnessView;
import in.bored.api.repo.HealthWellnessCategoryRepository;
import in.bored.api.repo.HealthWellnessContentRepository;
import in.bored.api.repo.UserHealthWellnessViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class HealthWellnessServiceTest {
    @Mock
    HealthWellnessContentRepository contentRepository;
    @Mock
    HealthWellnessCategoryRepository categoryRepository;
    @Mock
    UserHealthWellnessViewRepository viewRepository;
    @Mock
    GeminiService geminiService;

    HealthWellnessService service;

    @BeforeEach
    void setUp() {
        service = new HealthWellnessService(contentRepository, categoryRepository, viewRepository, geminiService);
    }

    @Test
    void getNextTip_returnsExistingContent_andMarksViewed() {
        Long userId = 1L;
        String topic = "Fitness";
        UUID catId = UUID.randomUUID();
        HealthWellnessCategory category = new HealthWellnessCategory();
        category.setId(catId);
        category.setName(topic);

        HealthWellnessContent content = new HealthWellnessContent();
        content.setId(100L);
        content.setCategory(category);
        content.setTip("Drink water");

        when(categoryRepository.findByName(topic)).thenReturn(Optional.of(category));
        when(contentRepository.findRandomUnseen(userId, catId)).thenReturn(Optional.of(content));

        HealthWellnessContent result = service.getNextTip(userId, topic);

        assertNotNull(result);
        assertEquals("Drink water", result.getTip());

        // Verify it was marked as viewed
        verify(viewRepository).save(any(UserHealthWellnessView.class));
    }

    @Test
    void getNextTip_generatesContent_andMarksViewed_whenNoExistingContent() {
        Long userId = 1L;
        String topic = "Mindfulness";
        UUID catId = UUID.randomUUID();
        HealthWellnessCategory category = new HealthWellnessCategory();
        category.setId(catId);
        category.setName(topic);

        when(categoryRepository.findByName(topic)).thenReturn(Optional.of(category));

        HealthWellnessContent savedContent = new HealthWellnessContent();
        savedContent.setId(200L);
        savedContent.setTip("Breathe deeply");
        savedContent.setCategory(category);

        // 1. Initial check returns empty
        // 2. Refetch after generation returns content
        when(contentRepository.findRandomUnseen(userId, catId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(savedContent));

        when(geminiService.generateHealthWellnessTip(eq(topic), anyInt()))
                .thenReturn(Collections.singletonList("Breathe deeply"));

        HealthWellnessContent result = service.getNextTip(userId, topic);

        assertNotNull(result);
        assertEquals("Breathe deeply", result.getTip());

        // Verify save was called for the new content
        verify(contentRepository).save(any(HealthWellnessContent.class));

        // Verify it was marked as viewed
        verify(viewRepository).save(any(UserHealthWellnessView.class));
    }

    @Test
    void getNextTip_deduplicatesGeneratedContent() {
        Long userId = 1L;
        String topic = "Yoga";
        UUID catId = UUID.randomUUID();
        HealthWellnessCategory category = new HealthWellnessCategory();
        category.setId(catId);
        category.setName(topic);

        when(categoryRepository.findByName(topic)).thenReturn(Optional.of(category));

        // Mock Gemini response with duplicate content
        when(geminiService.generateHealthWellnessTip(eq(topic), anyInt()))
                .thenReturn(Collections.singletonList("Do a headstand"));

        // Mock that "Do a headstand" ALREADY exists in DB
        HealthWellnessContent existingContent = new HealthWellnessContent();
        existingContent.setId(300L);
        existingContent.setTip("Do a headstand");
        when(contentRepository.findByTip("Do a headstand")).thenReturn(Optional.of(existingContent));

        // findRandomUnseen initially empty, then returns existing
        when(contentRepository.findRandomUnseen(userId, catId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingContent));

        HealthWellnessContent result = service.getNextTip(userId, topic);

        assertNotNull(result);
        assertEquals("Do a headstand", result.getTip());

        // Verify save was NOT called for new content (because it was a duplicate)
        verify(contentRepository, never()).save(any(HealthWellnessContent.class));

        // Verify it was marked as viewed
        verify(viewRepository).save(any(UserHealthWellnessView.class));
    }

    @Test
    void markAsViewed_doesNothing_ifAlreadyViewed() {
        Long userId = 1L;
        Long contentId = 100L;

        when(viewRepository.existsByUserProfileIdAndHealthWellnessContentId(userId, contentId)).thenReturn(true);

        service.markAsViewed(userId, contentId, true);

        verify(viewRepository, never()).save(any(UserHealthWellnessView.class));
    }
}
