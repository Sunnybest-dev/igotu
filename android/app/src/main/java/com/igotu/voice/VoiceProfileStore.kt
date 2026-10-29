package com.igotu.voice

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice profile: stores a float embedding derived from enrollment recordings.
 * Raw audio is NOT persisted — only the derived embedding.
 * The embedding is stored in EncryptedSharedPreferences (AES-256-GCM).
 *
 * PRIVACY NOTE: Voice embeddings are treated as biometric-adjacent data.
 * They are never uploaded to the backend by default.
 * They are deleted when the user removes their voice profile.
 */
data class VoiceProfile(
    val id: String,
    val createdAt: Long,
    val embedding: FloatArray  // derived from enrollment samples
)

@Singleton
class VoiceProfileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "igotu_voice_profile",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(profile: VoiceProfile) {
        prefs.edit().putString(KEY_PROFILE, gson.toJson(profile)).apply()
    }

    fun load(): VoiceProfile? {
        val json = prefs.getString(KEY_PROFILE, null) ?: return null
        return gson.fromJson(json, VoiceProfile::class.java)
    }

    fun delete() {
        prefs.edit().remove(KEY_PROFILE).apply()
    }

    fun exists(): Boolean = prefs.contains(KEY_PROFILE)

    companion object { private const val KEY_PROFILE = "profile" }
}
