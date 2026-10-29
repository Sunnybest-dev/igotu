package com.igotu.voice

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class VoiceProfileVerifier @Inject constructor(
    private val profileStore: VoiceProfileStore
) {
    /**
     * Returns a similarity score 0..1 between live audio and the stored profile.
     * Returns null if no profile is enrolled.
     *
     * NOTE: This is a cosine-similarity baseline. Replace with a proper
     * speaker-embedding model (TFLite) for production-quality verification.
     * Voice verification is advisory — it reduces false positives but is NOT
     * a guaranteed identity mechanism.
     */
    fun verify(liveEmbedding: FloatArray): Float? {
        val profile = profileStore.load() ?: return null
        return cosineSimilarity(liveEmbedding, profile.embedding)
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val len = minOf(a.size, b.size)
        var dot = 0.0; var normA = 0.0; var normB = 0.0
        for (i in 0 until len) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0.0) 0f else (dot / denom).toFloat()
    }
}
