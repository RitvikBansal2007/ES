package ac.iiit.rtltutor.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * SecureUserStore — encrypted persistent user data using Android Keystore + AES-256-GCM.
 *
 * Storage layout (all files in app private storage, encrypted):
 *
 *   users_registry.enc          → SharedPrefs: key="user_{username}" → UserRecord JSON
 *   user_data_{userId}.enc      → SharedPrefs: key="profile" → UserLearningProfile JSON
 *                                              key="session_count" → Int
 *                                              key="streak" → Int
 *
 * Security model:
 *   - Encrypted with Android Keystore-backed MasterKey (AES256-GCM)
 *   - Files are app-private (other apps cannot read them)
 *   - Admin reads all user files (same app, same Keystore access)
 *   - User A cannot see User B's data in the UI (enforced by Repository)
 *   - On-disk files are ciphertext even with root access
 */
object SecureUserStore {

    private const val TAG = "SecureUserStore"
    private const val REGISTRY_FILE = "users_registry"

    private val gson = Gson()

    // ── Internal representation stored in encrypted prefs ──────────────────

    data class UserRecord(
        val id: String,
        val username: String,
        val displayName: String,
        val role: String,          // "STUDENT" | "ADMIN"
        val passwordHash: String,  // BCrypt hash
        val createdAt: Long
    )

    // ── MasterKey ──────────────────────────────────────────────────────────

    private fun buildMasterKey(context: Context): MasterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    // ── EncryptedSharedPreferences factory ─────────────────────────────────

    private fun openEncryptedPrefs(context: Context, fileName: String): SharedPreferences =
        EncryptedSharedPreferences.create(
            context.applicationContext,
            fileName,
            buildMasterKey(context.applicationContext),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    // ─────────────────────────────────────────────────────────────────────────
    // User Registry (all users → encrypted)
    // ─────────────────────────────────────────────────────────────────────────

    fun saveUser(context: Context, record: UserRecord) {
        try {
            val prefs = openEncryptedPrefs(context, REGISTRY_FILE)
            prefs.edit().putString("user_${record.username.lowercase()}", gson.toJson(record)).apply()
            Log.d(TAG, "Saved user: ${record.username}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user: ${e.message}")
        }
    }

    fun getUser(context: Context, username: String): UserRecord? {
        return try {
            val prefs = openEncryptedPrefs(context, REGISTRY_FILE)
            val json = prefs.getString("user_${username.lowercase()}", null) ?: return null
            gson.fromJson(json, UserRecord::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user: ${e.message}")
            null
        }
    }

    fun getAllUsers(context: Context): List<UserRecord> {
        return try {
            val prefs = openEncryptedPrefs(context, REGISTRY_FILE)
            prefs.all.values
                .filterIsInstance<String>()
                .mapNotNull { json ->
                    try { gson.fromJson(json, UserRecord::class.java) } catch (e: Exception) { null }
                }
                .sortedBy { it.createdAt }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all users: ${e.message}")
            emptyList()
        }
    }

    fun userExists(context: Context, username: String): Boolean =
        getUser(context, username) != null

    // ─────────────────────────────────────────────────────────────────────────
    // Per-User Data (learning profile, sessions, streak — encrypted separately)
    // ─────────────────────────────────────────────────────────────────────────

    private fun userDataFile(userId: String) = "user_data_$userId"

    fun getSessionCount(context: Context, userId: String): Int {
        return try {
            openEncryptedPrefs(context, userDataFile(userId))
                .getInt("session_count", 0)
        } catch (e: Exception) { 0 }
    }

    fun incrementSessionCount(context: Context, userId: String) {
        try {
            val prefs = openEncryptedPrefs(context, userDataFile(userId))
            val count = prefs.getInt("session_count", 0)
            prefs.edit().putInt("session_count", count + 1).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to increment session count: ${e.message}")
        }
    }

    fun getStreak(context: Context, userId: String): Int {
        return try {
            openEncryptedPrefs(context, userDataFile(userId))
                .getInt("streak", 0)
        } catch (e: Exception) { 0 }
    }

    fun setStreak(context: Context, userId: String, streak: Int) {
        try {
            openEncryptedPrefs(context, userDataFile(userId))
                .edit().putInt("streak", streak).apply()
        } catch (e: Exception) { }
    }

    /**
     * Save an arbitrary JSON blob for a user (e.g., UserLearningProfile, quiz results).
     * [key] must be unique per data type, e.g. "learning_profile", "quiz_results".
     */
    fun saveUserData(context: Context, userId: String, key: String, jsonValue: String) {
        try {
            openEncryptedPrefs(context, userDataFile(userId))
                .edit().putString(key, jsonValue).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user data [$key]: ${e.message}")
        }
    }

    fun getUserData(context: Context, userId: String, key: String): String? {
        return try {
            openEncryptedPrefs(context, userDataFile(userId))
                .getString(key, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user data [$key]: ${e.message}")
            null
        }
    }

    /** Admin: get ALL keys+values for a user (returns map of key→value JSON strings). */
    fun getAllUserData(context: Context, userId: String): Map<String, String> {
        return try {
            openEncryptedPrefs(context, userDataFile(userId))
                .all
                .filterValues { it is String }
                .mapValues { (_, v) -> v as String }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all data for $userId: ${e.message}")
            emptyMap()
        }
    }
}
