package com.francescooddo.remindy.wear.protocol

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TagCompletionRequestTest {
    @Test
    fun `watch message has one stable versioned representation`() {
        val request = TagCompletionRequest(
            operationId = "scan-123",
            uid = "04A1B2C3D4E5F6",
        )

        val encoded = request.encode()

        assertContentEquals("1\nscan-123\n04A1B2C3D4E5F6".encodeToByteArray(), encoded)
        assertEquals(request, TagCompletionRequest.decode(encoded))
    }

    @Test
    fun `phone rejects malformed or non hexadecimal tag messages`() {
        assertNull(TagCompletionRequest.decode("1\nscan-123\nnot-a-uid".encodeToByteArray()))
        assertNull(TagCompletionRequest.decode("2\nscan-123\n04A1B2C3D4E5F6".encodeToByteArray()))
        assertNull(TagCompletionRequest.decode(byteArrayOf()))
    }
}
