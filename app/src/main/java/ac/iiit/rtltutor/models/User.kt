package ac.iiit.rtltutor.models

data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val role: UserRole,
    val passwordHash: String,
    val keySalt: ByteArray,
    val createdAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as User
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
