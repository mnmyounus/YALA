package com.mnmyounus.yala.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mnmyounus.yala.core.util.CryptoUtils
import com.mnmyounus.yala.domain.model.Credential
import com.mnmyounus.yala.domain.model.GlobalLockConfig
import com.mnmyounus.yala.domain.model.LockMode
import com.mnmyounus.yala.domain.model.LockType
import com.mnmyounus.yala.domain.model.LockedApp
import com.mnmyounus.yala.domain.model.ThemeMode
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single encrypted store for credentials, recovery key, locked-app table and settings.
 * Falls back to a plain SharedPreferences file only if the keystore is unavailable
 * (some API 21-22 OEM builds), so the app never hard-crashes on old devices.
 */
@Singleton
class SecurePrefs @Inject constructor(context: Context) {

    private val prefs: SharedPreferences = runCatching {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "yala_secure",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as SharedPreferences
    }.getOrElse {
        context.getSharedPreferences("yala_fallback", Context.MODE_PRIVATE)
    }

    // ---- Credentials -------------------------------------------------------

    fun saveCredential(type: LockType, rawSecret: String) {
        val salt = CryptoUtils.newSalt()
        prefs.edit()
            .putString("cred_${type.name}_salt", salt)
            .putString("cred_${type.name}_hash", CryptoUtils.hash(rawSecret, salt))
            .apply()
    }

    fun credential(type: LockType): Credential? {
        val salt = prefs.getString("cred_${type.name}_salt", null) ?: return null
        val hash = prefs.getString("cred_${type.name}_hash", null) ?: return null
        return Credential(type, hash, salt)
    }

    fun verify(type: LockType, rawSecret: String): Boolean {
        val c = credential(type) ?: return false
        return CryptoUtils.verify(rawSecret, c.salt, c.hash)
    }

    // ---- Recovery key ------------------------------------------------------

    /** Created once during onboarding; the plaintext is shown to the user exactly once. */
    fun createRecoveryKey(): String {
        val key = CryptoUtils.generateRecoveryKey()
        val salt = CryptoUtils.newSalt()
        prefs.edit()
            .putString("recovery_salt", salt)
            .putString("recovery_hash", CryptoUtils.hash(key, salt))
            .putString("recovery_plain_once", key)
            .apply()
        return key
    }

    fun peekRecoveryKeyOnce(): String? = prefs.getString("recovery_plain_once", null)

    fun clearRecoveryPlaintext() = prefs.edit().remove("recovery_plain_once").apply()

    fun verifyRecoveryKey(input: String): Boolean {
        val salt = prefs.getString("recovery_salt", null) ?: return false
        val hash = prefs.getString("recovery_hash", null) ?: return false
        return CryptoUtils.verify(input.trim().uppercase(), salt, hash)
    }

    // ---- Lock mode / global config ----------------------------------------

    var lockMode: LockMode
        get() = LockMode.valueOf(prefs.getString("lock_mode", LockMode.GLOBAL.name)!!)
        set(v) = prefs.edit().putString("lock_mode", v.name).apply()

    var globalConfig: GlobalLockConfig
        get() = GlobalLockConfig(
            LockType.valueOf(prefs.getString("global_type", LockType.PIN.name)!!),
            prefs.getString("global_hint", "") ?: ""
        )
        set(v) = prefs.edit()
            .putString("global_type", v.lockType.name)
            .putString("global_hint", v.hint)
            .apply()

    var themeMode: ThemeMode
        get() = ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name)!!)
        set(v) = prefs.edit().putString("theme", v.name).apply()

    var captureOnSuccess: Boolean
        get() = prefs.getBoolean("cap_success", true)
        set(v) = prefs.edit().putBoolean("cap_success", v).apply()

    var captureOnFailure: Boolean
        get() = prefs.getBoolean("cap_failure", true)
        set(v) = prefs.edit().putBoolean("cap_failure", v).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(v) = prefs.edit().putBoolean("onboarded", v).apply()

    /** Stable ids of the 5 images uploaded for the image-sequence lock. */
    var imagePoolIds: List<String>
        get() = prefs.getString("image_pool", "")!!.split("|").filter { it.isNotBlank() }
        set(v) = prefs.edit().putString("image_pool", v.joinToString("|")).apply()

    // ---- Locked apps -------------------------------------------------------

    fun lockedApps(): List<LockedApp> {
        val raw = prefs.getString("locked_apps", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            LockedApp(
                packageName = o.getString("pkg"),
                lockType = LockType.valueOf(o.optString("type", LockType.PIN.name)),
                hint = o.optString("hint", ""),
                enabled = o.optBoolean("enabled", true)
            )
        }
    }

    fun saveLockedApps(apps: List<LockedApp>) {
        val arr = JSONArray()
        apps.forEach { a ->
            arr.put(
                JSONObject()
                    .put("pkg", a.packageName)
                    .put("type", a.lockType.name)
                    .put("hint", a.hint)
                    .put("enabled", a.enabled)
            )
        }
        prefs.edit().putString("locked_apps", arr.toString()).apply()
    }
}
