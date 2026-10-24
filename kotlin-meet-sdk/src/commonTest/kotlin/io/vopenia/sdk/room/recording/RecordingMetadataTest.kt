package io.vopenia.sdk.room.recording

import kotlin.test.Test
import kotlin.test.assertEquals

class RecordingMetadataTest {

    @Test
    fun readsModeAndStatus_asTheMeetBackendWritesThem() {
        assertEquals(
            RecordingMetadata(RecordingMode.Transcript, RecordingMetadata.Status.Starting),
            RecordingMetadata.parse("""{"recording_mode": "transcript", "recording_status": "starting"}"""),
        )
        assertEquals(
            RecordingMetadata(RecordingMode.ScreenRecording, RecordingMetadata.Status.Saving),
            RecordingMetadata.parse("""{"other": 1, "recording_mode": "screen_recording", "recording_status": "saving"}"""),
        )
    }

    @Test
    fun unknownValuesAreNull() {
        assertEquals(
            RecordingMetadata(null, RecordingMetadata.Status.Started),
            RecordingMetadata.parse("""{"recording_mode": "podcast", "recording_status": "started"}"""),
        )
    }

    @Test
    fun noUsableMetadataIsNone() {
        listOf(null, "", "  ", "not json", "[1, 2]", "{}", """{"recording_mode": 3}""").forEach {
            assertEquals(RecordingMetadata.None, RecordingMetadata.parse(it), "for $it")
        }
    }
}
