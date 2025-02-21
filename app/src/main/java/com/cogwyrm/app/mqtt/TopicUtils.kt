package com.cogwyrm.app.mqtt

object TopicUtils {
    private val INVALID_TOPIC_CHARS = charArrayOf('#', '+', '/')
    private const val SINGLE_LEVEL_WILDCARD = "+"
    private const val MULTI_LEVEL_WILDCARD = "#"

    /**
     * Validates if a topic string is valid according to MQTT spec:
     * - Single-level wildcard (+) can substitute for one topic level
     * - Multi-level wildcard (#) must be the last character and preceded by /
     * - Topic must not be empty
     * - Topic must not contain null characters
     */
    fun validateTopic(topic: String): Boolean {
        if (topic.isEmpty() || topic.contains('\u0000')) {
            return false
        }

        val levels = topic.split('/')

        // Check each level
        levels.forEachIndexed { index, level ->
            when {
                // Empty level (consecutive slashes)
                level.isEmpty() && index > 0 -> return false

                // Multi-level wildcard must be alone at last level
                level.contains(MULTI_LEVEL_WILDCARD) -> {
                    if (level != MULTI_LEVEL_WILDCARD || index != levels.lastIndex) {
                        return false
                    }
                }

                // Single-level wildcard must be alone in its level
                level.contains(SINGLE_LEVEL_WILDCARD) -> {
                    if (level != SINGLE_LEVEL_WILDCARD) {
                        return false
                    }
                }
            }
        }

        return true
    }

    /**
     * Checks if a topic matches a subscription pattern.
     * Examples:
     * - "home/+/temperature" matches "home/kitchen/temperature"
     * - "home/#" matches "home/kitchen/temperature"
     */
    fun topicMatchesPattern(pattern: String, topic: String): Boolean {
        if (!validateTopic(pattern) || !validateTopic(topic)) {
            return false
        }

        val patternLevels = pattern.split('/')
        val topicLevels = topic.split('/')

        return matchLevels(patternLevels, topicLevels)
    }

    private fun matchLevels(patternLevels: List<String>, topicLevels: List<String>): Boolean {
        var patternIndex = 0
        var topicIndex = 0

        while (patternIndex < patternLevels.size && topicIndex < topicLevels.size) {
            val patternLevel = patternLevels[patternIndex]
            val topicLevel = topicLevels[topicIndex]

            when {
                // Multi-level wildcard matches everything after
                patternLevel == MULTI_LEVEL_WILDCARD -> return true

                // Single-level wildcard matches just this level
                patternLevel == SINGLE_LEVEL_WILDCARD -> {
                    patternIndex++
                    topicIndex++
                }

                // Exact match required
                patternLevel == topicLevel -> {
                    patternIndex++
                    topicIndex++
                }

                // No match
                else -> return false
            }
        }

        // Both must reach the end for a match
        return patternIndex == patternLevels.size && topicIndex == topicLevels.size
    }

    /**
     * Returns a helpful description of wildcard usage
     */
    fun getWildcardHelp(): String = """
        Topic wildcards:
        + = single level (e.g., "home/+/temp" matches "home/kitchen/temp")
        # = multi level (e.g., "home/#" matches "home/kitchen/temp")

        Examples:
        home/+/temperature   - Matches any room's temperature
        home/kitchen/+      - Matches any kitchen sensor
        home/#             - Matches everything in home
    """.trimIndent()
}
