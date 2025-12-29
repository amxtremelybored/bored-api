package in.bored.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdTargetingRuleResponse(
        UUID id,
        UUID adId,
        Integer minAge,
        Integer maxAge,
        String targetState,
        String targetGender,
        String adCategory,
        OffsetDateTime createdAt) {
}
