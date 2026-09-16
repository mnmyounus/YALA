package com.mnmyounus.yala.core.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Offline-only hashing helpers. Credentials are never stored in plain text;
 * only salted SHA-256 digests are written to EncryptedSharedPreferences.
 */
object CryptoUtils {

    private const val RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val RECOVERY_LENGTH = 12
    private val random = SecureRandom()

    fun newSalt(bytes: Int = 16): String {
        val buf = ByteArray(bytes)
        random.nextBytes(buf)
        return Base64.encodeToString(buf, Base64.NO_WRAP)
    }

    fun hash(secret: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray())
        // Key stretching: cheap but meaningful on-device iteration count.
        var out = digest.digest(secret.toByteArray())
        repeat(20_000) {
            val d = MessageDigest.getInstance("SHA-256")
            d.update(salt.toByteArray())
            out = d.digest(out)
        }
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun verify(secret: String, salt: String, expectedHash: String): Boolean =
        constantTimeEquals(hash(secret, salt), expectedHash)

    /** 12-character alphanumeric emergency recovery key. */
    fun generateRecoveryKey(): String = buildString {
        repeat(RECOVERY_LENGTH) { append(RECOVERY_ALPHABET[random.nextInt(RECOVERY_ALPHABET.length)]) }
    }

    /** Pattern dots are normalised to "0-4-8-7" style before hashing. */
    fun encodePattern(dots: List<Int>): String = dots.joinToString("-")

    /** Image sequence is encoded from the stable local image ids the user picked. */
    fun encodeImageSequence(imageIds: List<String>): String = imageIds.joinToString("|")

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }
}
