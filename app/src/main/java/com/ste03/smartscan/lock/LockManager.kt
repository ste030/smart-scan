package com.ste03.smartscan.lock

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * App lock: PIN/passcode stored in EncryptedSharedPreferences (Keystore-backed),
 * with optional biometric unlock.
 */
class LockManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "smartscan_lock",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_PASSCODE = "passcode_hash"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_LAST_UNLOCK = "last_unlock_ms"
    }

    private fun hash(passcode: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(passcode.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun isLockEnabled(): Boolean = prefs.getString(KEY_PASSCODE, null) != null

    fun setPasscode(passcode: String) {
        prefs.edit().putString(KEY_PASSCODE, hash(passcode)).apply()
    }

    fun clearLock() {
        prefs.edit().remove(KEY_PASSCODE).remove(KEY_BIOMETRIC).apply()
    }

    fun verify(passcode: String): Boolean =
        prefs.getString(KEY_PASSCODE, null) == hash(passcode)

    fun biometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    /** Grace window so the lock doesn't re-prompt on every quick background/foreground. */
    fun markUnlocked() {
        prefs.edit().putLong(KEY_LAST_UNLOCK, System.currentTimeMillis()).apply()
    }

    fun isUnlocked(): Boolean {
        val last = prefs.getLong(KEY_LAST_UNLOCK, 0L)
        return System.currentTimeMillis() - last < 60_000L
    }
}
