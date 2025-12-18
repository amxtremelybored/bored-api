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
import java.util.List;
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
    void getNextTips_returnsExistingContent_andMarksViewed() {
        Long userId = 1L;
        String topic = "Fitness";
        int count = 2;
        UUID catId = UUID.randomUUID();
        HealthWellnessCategory category = new HealthWellnessCategory();
        category.setId(catId);
        category.setName(topic);

        HealthWellnessContent content1 = new HealthWellnessContent();
        content1.setId(100L);
        content1.setCategory(category);
        content1.setTip("Drink water");

        HealthWellnessContent content2 = new HealthWellnessContent();
        content2.setId(101L);
        content2.setCategory(category);
        content2.setTip("Walk daily");

        when(categoryRepository.findByName(topic)).thenReturn(Optional.of(category));
        when(contentRepository.findRandomUnseen(userId, catId, count)).thenReturn(List.of(content1, content2));

        List<HealthWellnessContent> result = service.getNextTips(userId, topic, count);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Drink water", result.get(0).getTip());

        // Verify it was marked as viewed
        verify(viewRepository, times(2)).save(any(UserHealthWellnessView.class));
    }

    @Test
    void getNextTips_generatesContent_andMarksViewed_whenNoExistingContent() {
        Long userId = 1L;
        String topic = "Mindfulness";
        int count = 1;
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
        when(contentRepository.findRandomUnseen(userId, catId, count))
                .thenReturn(List.of())
                .thenReturn(List.of(savedContent));

        when(geminiService.generateHealthWellnessTip(eq(topic), anyInt()))
                .thenReturn(Collections.singletonList("Breathe deeply"));

        List<HealthWellnessContent> result = service.getNextTips(userId, topic, count);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Breathe deeply", result.get(0).getTip());

        // Verify save was called for the new content
        verify(contentRepository).save(any(HealthWellnessContent.class));

        // Verify it was marked as viewed
        verify(viewRepository).save(any(UserHealthWellnessView.class));
    }

    @Test
    void getNextTips_deduplicatesGeneratedContent() {
        Long userId = 1L;
        String topic = "Yoga";
        int count = 1;
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
        when(contentRepository.findRandomUnseen(userId, catId, count))
                .thenReturn(List.of())
                .thenReturn(List.of(existingContent));

        List<HealthWellnessContent> result = service.getNextTips(userId, topic, count);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Do a headstand", result.get(0).getTip());

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
