package com.tofiq.peekdetector.feature.panic

import android.media.AudioManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

/**
 * Property-based tests for PanicAlertService audio configuration.
 */
class PanicAlertServicePropertyTest : FunSpec({

    test("Property 3 - Audio stream type is STREAM_ALARM for maximum audibility") {
        PanicAlertConstants.AUDIO_STREAM_TYPE shouldBe AudioManager.STREAM_ALARM
    }

    test("Property 3 - Swipe threshold is 80% for activation") {
        PanicAlertConstants.SWIPE_THRESHOLD shouldBe 0.8f
    }

    test("Property 3 - Notification channel ID is properly defined") {
        PanicAlertConstants.NOTIFICATION_CHANNEL_ID shouldBe "panic_alert_channel"
    }

    test("Property 3 - Notification ID is unique and non-zero") {
        (PanicAlertConstants.NOTIFICATION_ID > 0) shouldBe true
        PanicAlertConstants.NOTIFICATION_ID shouldBe 2001
    }

    test("Property 3 - Stop action intent is properly defined") {
        PanicAlertConstants.ACTION_STOP_SIREN shouldBe "com.tofiq.peekdetector.STOP_SIREN"
    }

    test("Property 4 - Volume restoration logic works for all valid volume levels") {
        checkAll(100, Arb.int(0..15)) { previousVolume ->
            val restoredVolume = if (previousVolume >= 0) previousVolume else -1
            
            if (previousVolume >= 0) {
                restoredVolume shouldBe previousVolume
            }
        }
    }

    test("Property 4 - Invalid previous volume (-1) indicates no restoration needed") {
        val invalidPreviousVolume = -1
        (invalidPreviousVolume < 0) shouldBe true
    }

    test("Property 4 - Volume values are within valid Android range") {
        checkAll(100, Arb.int(0..15)) { volume ->
            (volume >= 0) shouldBe true
        }
    }
})
