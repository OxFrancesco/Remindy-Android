package com.francescooddo.remindy.wear.protocol

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

data class WearReminder(
    val id: String,
    val title: String,
) {
    init {
        require(id.isNotBlank()) { "Reminder ID cannot be blank" }
        require(title.isNotBlank()) { "Reminder title cannot be blank" }
    }
}

data class ReminderSnapshot(
    val generatedAtMillis: Long,
    val reminders: List<WearReminder>,
) {
    init {
        require(reminders.size <= MAX_REMINDERS) { "Too many reminders" }
        require(reminders.map(WearReminder::id).distinct().size == reminders.size) {
            "Reminder IDs must be unique"
        }
    }

    fun encode(): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeLong(generatedAtMillis)
            output.writeInt(reminders.size)
            reminders.forEach { reminder ->
                output.writeBoundedString(reminder.id)
                output.writeBoundedString(reminder.title)
            }
        }
        bytes.toByteArray()
    }

    companion object {
        const val PATH = "/remindy/reminders/v1"

        private const val MAGIC = 0x524D4459
        private const val VERSION = 1
        private const val MAX_PAYLOAD_BYTES = 100 * 1024
        private const val MAX_REMINDERS = 512
        private const val MAX_STRING_BYTES = 4 * 1024

        fun decode(payload: ByteArray): ReminderSnapshot? {
            if (payload.isEmpty() || payload.size > MAX_PAYLOAD_BYTES) return null
            return runCatching {
                DataInputStream(ByteArrayInputStream(payload)).use { input ->
                    require(input.readInt() == MAGIC)
                    require(input.readInt() == VERSION)
                    val generatedAtMillis = input.readLong()
                    val count = input.readInt()
                    require(count in 0..MAX_REMINDERS)
                    val reminders = List(count) {
                        WearReminder(
                            id = input.readBoundedString(),
                            title = input.readBoundedString(),
                        )
                    }
                    require(input.available() == 0)
                    ReminderSnapshot(generatedAtMillis, reminders)
                }
            }.getOrNull()
        }

        private fun DataOutputStream.writeBoundedString(value: String) {
            val encoded = value.encodeToByteArray()
            require(encoded.size in 1..MAX_STRING_BYTES) { "Reminder value is too large" }
            writeInt(encoded.size)
            write(encoded)
        }

        private fun DataInputStream.readBoundedString(): String {
            val length = readInt()
            require(length in 1..MAX_STRING_BYTES)
            return ByteArray(length).also(::readFully).decodeToString(throwOnInvalidSequence = true)
        }
    }
}
