// SPDX-License-Identifier: GPL-3.0-or-later

package com.screengarden.app.data.backup

/** Fixed backup columns mirror the seven user-data tables; never accept document-supplied SQL. */
internal object BackupSchema {
    val tables: Map<String, BackupTable> = linkedMapOf(
        "daily_stats" to BackupTable(
            listOf(
                BackupColumn("dayStartUtcMillis", "integer", max = Long.MAX_VALUE),
                BackupColumn("focusMinutes", "integer"),
                BackupColumn("screenMinutes", "integer"),
                BackupColumn("sessionsCompleted", "integer"),
                BackupColumn("plantsGrown", "integer"),
                BackupColumn("updatedAtUtcMillis", "integer", max = Long.MAX_VALUE),
                BackupColumn("score", "integer", nullable = true, max = 100),
                BackupColumn("pickups", "integer", nullable = true),
                BackupColumn("unlocks", "integer", nullable = true),
                BackupColumn("usageIsBounded", "boolean"),
                BackupColumn("gardenApplied", "boolean"),
                BackupColumn("bedtimeMinutes", "integer", nullable = true),
                BackupColumn("isCompassionDay", "boolean")
            ),
            listOf("dayStartUtcMillis")
        ),
        "app_usage_daily" to BackupTable(
            listOf(
                BackupColumn("dayStartUtcMillis", "integer", max = Long.MAX_VALUE),
                BackupColumn("packageName", "text", max = 200),
                BackupColumn("usageMillis", "integer", max = Long.MAX_VALUE),
                BackupColumn("launchCount", "integer"),
                BackupColumn("updatedAtUtcMillis", "integer", max = Long.MAX_VALUE)
            ),
            listOf("dayStartUtcMillis", "packageName")
        ),
        "app_limits" to BackupTable(
            listOf(
                BackupColumn("packageName", "text", max = 200),
                BackupColumn("dailyLimitMinutes", "integer", min = 1, max = 1440),
                BackupColumn("isEnabled", "boolean"),
                BackupColumn("updatedAtUtcMillis", "integer", max = Long.MAX_VALUE)
            ),
            listOf("packageName")
        ),
        "plants" to BackupTable(
            listOf(
                BackupColumn("plantId", "text", max = 200),
                BackupColumn("species", "text", max = 200),
                BackupColumn("stage", "integer", max = 3),
                BackupColumn("health", "integer", max = 100),
                BackupColumn("plantedAtUtcMillis", "integer", max = Long.MAX_VALUE),
                BackupColumn("lastFocusAtUtcMillis", "integer", nullable = true, max = Long.MAX_VALUE),
                BackupColumn("growthPoints", "integer"),
                BackupColumn("condition", "text", max = 200, options = setOf("GROWING", "WILTING", "RECOVERING"))
            ),
            listOf("plantId")
        ),
        "focus_sessions" to BackupTable(
            listOf(
                BackupColumn("sessionId", "text", max = 200),
                BackupColumn("dayStartUtcMillis", "integer", max = Long.MAX_VALUE),
                BackupColumn("startedAtUtcMillis", "integer", max = Long.MAX_VALUE),
                BackupColumn("endedAtUtcMillis", "integer", nullable = true, max = Long.MAX_VALUE),
                BackupColumn("plannedMinutes", "integer", min = 1, max = 240),
                BackupColumn("wasCompleted", "boolean"),
                BackupColumn("plantId", "text", nullable = true, max = 200),
                BackupColumn("tag", "text", max = 200, options = setOf("STUDY", "WORK", "READING", "OTHER")),
                BackupColumn(
                    "status",
                    "text",
                    max = 200,
                    options = setOf("RUNNING", "COMPLETED", "FAILED_SOFT", "CANCELLED")
                ),
                BackupColumn("actualDurationMs", "integer", max = Long.MAX_VALUE),
                BackupColumn("updatedAtUtcMillis", "integer", max = Long.MAX_VALUE),
                BackupColumn("bootCount", "integer", min = -1)
            ),
            listOf("sessionId")
        ),
        "badges" to BackupTable(
            listOf(
                BackupColumn("badgeKey", "text", max = 200),
                BackupColumn("unlockedAtUtcMillis", "integer", max = Long.MAX_VALUE)
            ),
            listOf("badgeKey")
        ),
        "garden_history" to BackupTable(
            listOf(
                BackupColumn("dayStartUtcMillis", "integer", max = Long.MAX_VALUE),
                BackupColumn("capturedAtUtcMillis", "integer", max = Long.MAX_VALUE),
                BackupColumn("score", "integer", max = 100),
                BackupColumn("plantsJson", "text", max = 1_000_000)
            ),
            listOf("dayStartUtcMillis")
        )
    )
}
