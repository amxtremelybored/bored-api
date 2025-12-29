package in.bored.api.dto;

import java.util.UUID;

public record AdRequest(
                String name,
                String adType,
                String imageUrl,
                String videoUrl,
                String textContent,
                String ctaText,
                String ctaUrl,
                boolean isActive,
                int priority,
                Integer durationSeconds) {
}
