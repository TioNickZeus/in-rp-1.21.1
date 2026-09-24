package com.tio.inrp.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InRPConfigTest {

    @Test
    @DisplayName("InRPConfig.SPEC.correct handles empty config without throwing NullPointerException")
    void testSpecCorrectWithEmptyConfig() {
        CommentedConfig config = CommentedConfig.inMemory();

        // This simulates createDefaultConfig on dedicated server startup,
        // which previously crashed when livesAction used List.of and tested null.
        assertDoesNotThrow(() -> InRPConfig.SPEC.correct(config),
                "InRPConfig.SPEC.correct must not throw NullPointerException on empty/uninitialized config");

        assertEquals("spectator", config.get("lives.livesAction"),
                "Default livesAction should be 'spectator'");
        assertTrue(InRPConfig.SPEC.isCorrect(config),
                "Config should be considered correct after correction");
    }

    @Test
    @DisplayName("livesAction accepts valid values and corrects invalid ones")
    void testLivesActionValidation() {
        CommentedConfig config = CommentedConfig.inMemory();

        // Test valid: spectator
        config.set("lives.livesAction", "spectator");
        InRPConfig.SPEC.correct(config);
        assertEquals("spectator", config.get("lives.livesAction"));

        // Test valid: kick
        config.set("lives.livesAction", "kick");
        InRPConfig.SPEC.correct(config);
        assertEquals("kick", config.get("lives.livesAction"));

        // Test invalid: unknown action should revert to default ("spectator")
        config.set("lives.livesAction", "banned_permanently");
        InRPConfig.SPEC.correct(config);
        assertEquals("spectator", config.get("lives.livesAction"),
                "Invalid action should be reverted to default 'spectator'");
    }
}
