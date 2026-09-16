package com.mnmyounus.yala.domain.model

/** The four custom authentication mechanisms. No OS biometrics, no system keyguard. */
enum class LockType { PIN, PASSWORD, PATTERN, IMAGE_SEQUENCE }

enum class LockMode { GLOBAL, INDIVIDUAL }

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean
)

/** Per-app lock configuration used in INDIVIDUAL mode. */
data class LockedApp(
    val packageName: String,
    val lockType: LockType,
    val hint: String = "",
    val enabled: Boolean = true
)

/** The globally applied lock configuration used in GLOBAL mode. */
data class GlobalLockConfig(
    val lockType: LockType = LockType.PIN,
    val hint: String = ""
)

/** Stored credential material. Values are salted-hashed before persistence. */
data class Credential(
    val lockType: LockType,
    val hash: String,
    val salt: String
)

enum class CaptureOutcome { SUCCESS, FAILURE }

data class IntruderShot(
    val id: Long,
    val filePath: String,
    val packageName: String,
    val timestampMillis: Long,
    val outcome: CaptureOutcome
)

data class OnboardingState(
    val completed: Boolean = false,
    val accessibilityGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val deviceAdminGranted: Boolean = false,
    val cameraGranted: Boolean = false,
    val disclaimerAccepted: Boolean = false
)

/** Result of an unlock attempt, consumed by the intruder-capture pipeline. */
sealed interface UnlockResult {
    data object Success : UnlockResult
    data class Failure(val attemptsRemaining: Int) : UnlockResult
    data object RecoveredWithKey : UnlockResult
}
