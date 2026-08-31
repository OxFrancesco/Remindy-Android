package com.francescooddo.remindy.wear.protocol

data class TagCompletionRequest(
    val operationId: String,
    val uid: String,
) {
    init {
        require(OPERATION_ID.matches(operationId)) { "Invalid scan operation ID" }
        require(uid.length % 2 == 0 && TAG_UID.matches(uid)) { "Invalid NFC tag UID" }
    }

    fun encode(): ByteArray = "$VERSION\n$operationId\n$uid".encodeToByteArray()

    companion object {
        const val PATH = "/remindy/tag-completion/v1"

        private const val VERSION = "1"
        private val OPERATION_ID = Regex("[A-Za-z0-9-]{1,64}")
        private val TAG_UID = Regex("[0-9A-F]{2,64}")

        fun decode(payload: ByteArray): TagCompletionRequest? {
            val parts = runCatching {
                payload.decodeToString(throwOnInvalidSequence = true).split('\n')
            }.getOrNull() ?: return null
            if (parts.size != 3 || parts[0] != VERSION) return null
            return runCatching {
                TagCompletionRequest(
                    operationId = parts[1],
                    uid = parts[2],
                )
            }.getOrNull()
        }
    }
}
