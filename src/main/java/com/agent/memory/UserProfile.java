package com.agent.memory;

import java.time.Instant;
import java.util.List;

public record UserProfile(String preferredLanguage, String codingStyle, List<String> techStack, String projectBudget, String role, Instant updatedAt) {
    public static UserProfile empty() { return new UserProfile("", "", List.of(), "", "", null); }
}
