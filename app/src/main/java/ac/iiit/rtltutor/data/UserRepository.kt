package ac.iiit.rtltutor.data

import android.content.Context
import android.util.Log
import ac.iiit.rtltutor.models.User
import ac.iiit.rtltutor.models.UserRole
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID

/**
 * UserRepository — multi-user auth with persistent encrypted storage.
 *
 * Security:
 *   - Passwords hashed with BCrypt (cost 12) — irreversible, salted automatically
 *   - All user records stored via SecureUserStore (EncryptedSharedPreferences / AES-256-GCM)
 *   - Only the currently logged-in user's data is exposed in [currentUser]
 *   - Admin role can call [getAllStudents] and [getUserDataForAdmin]
 *
 * Admin bootstrap:
 *   - username: admin, password: admin123
 *   - Created automatically on first launch if no admin exists
 */
object UserRepository {

    private const val TAG = "UserRepository"
    const val ADMIN_USERNAME = "admin"
    private const val ADMIN_PASSWORD = "admin123"

    private var appContext: Context? = null
    private var _currentUser: User? = null

    /** Currently logged-in user. Null if nobody is logged in. */
    val currentUser: User? get() = _currentUser

    // ─────────────────────────────────────────────────────────────────────────
    // Initialization (call once from Application.onCreate)
    // ─────────────────────────────────────────────────────────────────────────

    fun init(context: Context) {
        appContext = context.applicationContext
        bootstrapAdmin()
    }

    private fun bootstrapAdmin() {
        val ctx = appContext ?: return
        if (!SecureUserStore.userExists(ctx, ADMIN_USERNAME)) {
            val hash = BCrypt.hashpw(ADMIN_PASSWORD, BCrypt.gensalt(12))
            val record = SecureUserStore.UserRecord(
                id           = "admin-0",
                username     = ADMIN_USERNAME,
                displayName  = "Admin",
                role         = "ADMIN",
                passwordHash = hash,
                createdAt    = System.currentTimeMillis()
            )
            SecureUserStore.saveUser(ctx, record)
            Log.d(TAG, "Admin account bootstrapped")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Authentication
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attempt login.
     * @return [User] on success, null if credentials are wrong.
     */
    fun login(username: String, password: String): User? {
        val ctx = appContext ?: return null
        val record = SecureUserStore.getUser(ctx, username) ?: return null

        // BCrypt verify — checks hash without storing password anywhere
        if (!BCrypt.checkpw(password, record.passwordHash)) return null

        val user = record.toUser()
        _currentUser = user
        Log.d(TAG, "Login OK: ${user.username} (${user.role})")
        return user
    }

    /**
     * Register a new student account.
     * @return [User] on success, null if username taken or validation fails.
     */
    fun createUser(
        username: String,
        displayName: String,
        password: String,
        role: UserRole = UserRole.STUDENT
    ): User? {
        val ctx = appContext ?: return null

        if (username.isBlank() || password.length < 6) return null
        if (SecureUserStore.userExists(ctx, username)) return null

        val hash = BCrypt.hashpw(password, BCrypt.gensalt(12))
        val name = displayName.trim().ifBlank { username.trim() }
        val record = SecureUserStore.UserRecord(
            id           = UUID.randomUUID().toString(),
            username     = username.trim().lowercase(),
            displayName  = name,
            role         = role.name,
            passwordHash = hash,
            createdAt    = System.currentTimeMillis()
        )
        SecureUserStore.saveUser(ctx, record)

        val user = record.toUser()
        _currentUser = user   // auto-login after registration
        Log.d(TAG, "Created user: ${user.username}")
        return user
    }

    /** Log out the current user. */
    fun logout() {
        Log.d(TAG, "Logout: ${_currentUser?.username}")
        _currentUser = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin-only queries (enforce role check in ViewModel before calling)
    // ─────────────────────────────────────────────────────────────────────────

    fun getAllUsers(): List<User> {
        val ctx = appContext ?: return emptyList()
        return SecureUserStore.getAllUsers(ctx).map { it.toUser() }
    }

    fun getAllStudents(): List<User> = getAllUsers().filter { it.role == UserRole.STUDENT }

    /**
     * Admin: read a specific user's session count and streak.
     * Returns Pair(sessionCount, streak).
     */
    fun getUserStats(userId: String): Pair<Int, Int> {
        val ctx = appContext ?: return 0 to 0
        return SecureUserStore.getSessionCount(ctx, userId) to
               SecureUserStore.getStreak(ctx, userId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User-self data (only current user can call these)
    // ─────────────────────────────────────────────────────────────────────────

    fun incrementMySessionCount() {
        val ctx = appContext ?: return
        val uid = _currentUser?.id ?: return
        SecureUserStore.incrementSessionCount(ctx, uid)
    }

    fun getMySessionCount(): Int {
        val ctx = appContext ?: return 0
        val uid = _currentUser?.id ?: return 0
        return SecureUserStore.getSessionCount(ctx, uid)
    }

    fun getMyStreak(): Int {
        val ctx = appContext ?: return 0
        val uid = _currentUser?.id ?: return 0
        return SecureUserStore.getStreak(ctx, uid)
    }

    fun setMyStreak(streak: Int) {
        val ctx = appContext ?: return
        val uid = _currentUser?.id ?: return
        SecureUserStore.setStreak(ctx, uid, streak)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun SecureUserStore.UserRecord.toUser() = User(
        id           = id,
        username     = username,
        displayName  = displayName,
        role         = if (role == "ADMIN") UserRole.ADMIN else UserRole.STUDENT,
        passwordHash = passwordHash,
        keySalt      = ByteArray(0),  // BCrypt embeds salt in hash; keySalt unused
        createdAt    = createdAt
    )
}
