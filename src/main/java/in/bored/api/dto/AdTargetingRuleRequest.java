package in.bored.api.dto;

public record AdTargetingRuleRequest(
        Integer minAge,
        Integer maxAge,
        String targetState,
        String targetGender,
        String adCategory) {
}
