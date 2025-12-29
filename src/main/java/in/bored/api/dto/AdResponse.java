package in.bored.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdResponse(
                UUID id,
                String name,
                String adType,
                String imageUrl,
                String videoUrl,
                String textContent,
                String ctaText,
                String ctaUrl,
                Integer durationSeconds,
                boolean isActive,
                int priority,
                OffsetDateTime createdAt,
                OffsetDateTime updatedAt) {
}
