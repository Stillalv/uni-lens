package com.unilens.app.domain.stabilizer

import com.unilens.app.data.model.DetectedEntity

/**
 * TemporalStabilizer prevents visual flickering by requiring detected entities
 * to appear consistently across multiple frames before being confirmed to the UI.
 * Also handles deduplication and automatic expiration when out of frame.
 */
class TemporalStabilizer(
    private val requiredHits: Int = 2,
    private val expiryDurationMs: Long = 1500L
) {
    private data class TrackedCandidate(
        var hitCount: Int,
        val firstSeenAt: Long,
        var lastSeenAt: Long,
        var entity: DetectedEntity
    )

    private val trackedCandidates = mutableMapOf<String, TrackedCandidate>()

    /**
     * Updates stabilizer with raw detections from the current frame.
     * Returns the list of confirmed, stable, deduplicated entities.
     */
    @Synchronized
    fun update(
        currentFrameEntities: List<DetectedEntity>,
        currentTimeMs: Long = System.currentTimeMillis()
    ): List<DetectedEntity> {
        val seenInThisFrame = mutableSetOf<String>()

        for (item in currentFrameEntities) {
            val key = item.uniqueKey
            seenInThisFrame.add(key)

            val existing = trackedCandidates[key]
            if (existing != null) {
                existing.hitCount += 1
                existing.lastSeenAt = currentTimeMs
                existing.entity = item.copy(boundingBox = item.boundingBox ?: existing.entity.boundingBox)
            } else {
                trackedCandidates[key] = TrackedCandidate(
                    hitCount = 1,
                    firstSeenAt = currentTimeMs,
                    lastSeenAt = currentTimeMs,
                    entity = item
                )
            }
        }

        // Decay/prune entries not seen recently
        val iterator = trackedCandidates.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val candidate = entry.value
            if (currentTimeMs - candidate.lastSeenAt > expiryDurationMs) {
                iterator.remove()
            }
        }

        // Filter for confirmed stable entities
        return trackedCandidates.values
            .filter { it.hitCount >= requiredHits }
            .map { it.entity }
            .sortedByDescending { it.detectedAt }
    }

    @Synchronized
    fun clear() {
        trackedCandidates.clear()
    }
}
