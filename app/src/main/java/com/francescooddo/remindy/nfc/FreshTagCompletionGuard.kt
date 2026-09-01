package com.francescooddo.remindy.nfc

internal class FreshTagCompletionGuard(
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private data class PendingTag(val uid: String, val expiresAt: Long)

    private var pendingTag: PendingTag? = null

    @Synchronized
    fun recordLinked(uid: String) {
        pendingTag = PendingTag(uid = uid, expiresAt = nowMillis() + WRITE_ECHO_WINDOW_MILLIS)
    }

    @Synchronized
    fun shouldIgnore(uid: String): Boolean {
        val pending = pendingTag ?: return false
        if (nowMillis() > pending.expiresAt) {
            pendingTag = null
            return false
        }
        if (!pending.uid.equals(uid, ignoreCase = true)) return false
        pendingTag = null
        return true
    }

    private companion object {
        const val WRITE_ECHO_WINDOW_MILLIS = 5_000L
    }
}

internal object FreshTagCompletions {
    private val guard = FreshTagCompletionGuard()

    fun recordLinked(uid: String) {
        guard.recordLinked(uid)
    }

    fun shouldIgnore(uid: String): Boolean = guard.shouldIgnore(uid)
}
