package com.tofiq.peekdetector.feature.detection

import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Property-based tests for Smart Detection feature.
 */
class SmartDetectionPropertyTest {

    /**
     * Simple state machine that models the smart detection behavior.
     */
    class SmartDetectionStateMachine {
        var smartDetectionEnabled: Boolean = false
            private set
        var isScreenOn: Boolean = true
            private set
        var isDetectionActive: Boolean = true
            private set
        var receiverRegistered: Boolean = false
            private set
        
        fun setSmartDetectionEnabled(enabled: Boolean) {
            smartDetectionEnabled = enabled
            if (enabled) {
                receiverRegistered = true
                isDetectionActive = isScreenOn
            } else {
                receiverRegistered = false
                isDetectionActive = true
            }
        }
        
        fun setScreenState(screenOn: Boolean) {
            isScreenOn = screenOn
            if (smartDetectionEnabled) {
                isDetectionActive = screenOn
            }
        }
        
        fun reset() {
            smartDetectionEnabled = false
            isScreenOn = true
            isDetectionActive = true
            receiverRegistered = false
        }
    }

    private lateinit var stateMachine: SmartDetectionStateMachine

    @BeforeEach
    fun setup() {
        stateMachine = SmartDetectionStateMachine()
    }

    @Test
    fun `Property 6 - When smart detection enabled, detection follows screen state`() = runBlocking {
        checkAll(100, Arb.boolean()) { screenOn ->
            stateMachine.reset()
            stateMachine.setSmartDetectionEnabled(true)
            stateMachine.setScreenState(screenOn)
            
            assertEquals(
                screenOn,
                stateMachine.isDetectionActive,
                "When smart detection is enabled and screen is ${if (screenOn) "ON" else "OFF"}, " +
                "detection should be ${if (screenOn) "active" else "paused"}"
            )
        }
    }

    @Test
    fun `Property 6 - When smart detection disabled, detection always active`() = runBlocking {
        checkAll(100, Arb.boolean()) { screenOn ->
            stateMachine.reset()
            stateMachine.setSmartDetectionEnabled(false)
            stateMachine.setScreenState(screenOn)
            
            assertTrue(
                stateMachine.isDetectionActive,
                "When smart detection is disabled, detection should always be active"
            )
        }
    }

    @Test
    fun `Property 6 - Receiver registration follows smart detection setting`() = runBlocking {
        checkAll(100, Arb.boolean()) { enabled ->
            stateMachine.reset()
            stateMachine.setSmartDetectionEnabled(enabled)
            
            assertEquals(
                enabled,
                stateMachine.receiverRegistered,
                "Receiver should be ${if (enabled) "registered" else "unregistered"}"
            )
        }
    }

    @Test
    fun `Property 6 - Sequence of screen changes results in correct final state`() = runBlocking {
        checkAll(100, Arb.list(Arb.boolean(), 1..20)) { screenStates ->
            stateMachine.reset()
            stateMachine.setSmartDetectionEnabled(true)
            
            screenStates.forEach { screenOn ->
                stateMachine.setScreenState(screenOn)
            }
            
            val finalScreenState = screenStates.last()
            assertEquals(
                finalScreenState,
                stateMachine.isDetectionActive,
                "After sequence of screen changes, detection should match final screen state"
            )
        }
    }

    @Test
    fun `Property 6 - Toggling smart detection updates detection state correctly`() = runBlocking {
        checkAll(100, Arb.boolean(), Arb.boolean()) { initialEnabled, screenOn ->
            stateMachine.reset()
            stateMachine.setSmartDetectionEnabled(initialEnabled)
            stateMachine.setScreenState(screenOn)
            
            stateMachine.setSmartDetectionEnabled(!initialEnabled)
            
            if (!initialEnabled) {
                assertEquals(
                    screenOn,
                    stateMachine.isDetectionActive,
                    "After enabling smart detection, detection should follow screen state"
                )
            } else {
                assertTrue(
                    stateMachine.isDetectionActive,
                    "After disabling smart detection, detection should be active"
                )
            }
        }
    }

    @Test
    fun `Property 6 - Screen off then on resumes detection (round trip)`() = runBlocking {
        stateMachine.reset()
        stateMachine.setSmartDetectionEnabled(true)
        
        assertTrue(stateMachine.isDetectionActive, "Initially detection should be active")
        
        stateMachine.setScreenState(false)
        assertFalse(stateMachine.isDetectionActive, "Detection should pause when screen off")
        
        stateMachine.setScreenState(true)
        assertTrue(stateMachine.isDetectionActive, "Detection should resume when screen on")
    }
}
