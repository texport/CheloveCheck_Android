package com.chelovecheck.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureSecretsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getOpenAiApiKey(): String = prefs.getString(KEY_OPENAI_API_KEY, "").orEmpty()

    fun setOpenAiApiKey(value: String) {
        prefs.edit().putString(KEY_OPENAI_API_KEY, value).apply()
    }

    fun getGeminiApiKey(): String = prefs.getString(KEY_GEMINI_API_KEY, "").orEmpty()

    fun setGeminiApiKey(value: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()
    }

    private companion object {
        private const val FILE_NAME = "secure_settings"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }
}
