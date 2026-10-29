package com.igotu.sos

import android.content.Context
import com.igotu.messaging.MessagingOrchestrator
import com.igotu.recording.RecordingManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class SosEngineTest {

    private lateinit var engine: SosEngine
    private val context: Context = mock()
    private val recordingManager: RecordingManager = mock()
    private val messagingOrchestrator: MessagingOrchestrator = mock()
    private val auditLogger: SosAuditLogger = mock()

    @Before
    fun setup() {
        engine = SosEngine(context, recordingManager, messagingOrchestrator, auditLogger)
    }

    @Test
    fun `activate sets state to ACTIVE`() = runTest {
        engine.activate(TriggerMethod.BUTTON)
        assertEquals(SosState.ACTIVE, engine.state.value?.state)
        assertEquals(TriggerMethod.BUTTON, engine.state.value?.triggerMethod)
    }

    @Test
    fun `voice trigger stores confidence`() = runTest {
        engine.activate(TriggerMethod.VOICE, voiceConfidence = 0.87f)
        assertEquals(0.87f, engine.state.value?.voiceConfidence)
    }

    @Test
    fun `duplicate activate while active is ignored`() = runTest {
        engine.activate(TriggerMethod.BUTTON)
        val firstId = engine.state.value?.id
        engine.activate(TriggerMethod.VOICE)
        assertEquals(firstId, engine.state.value?.id)
    }

    @Test
    fun `resolve transitions to RESOLVED`() = runTest {
        engine.activate(TriggerMethod.BUTTON)
        val id = engine.state.value!!.id
        engine.resolve(id)
        assertEquals(SosState.RESOLVED, engine.state.value?.state)
        assertNotNull(engine.state.value?.resolvedAt)
    }

    @Test
    fun `cancel transitions to CANCELLED`() = runTest {
        engine.activate(TriggerMethod.BUTTON)
        val id = engine.state.value!!.id
        engine.cancel(id)
        assertEquals(SosState.CANCELLED, engine.state.value?.state)
    }

    @Test
    fun `recording failure does not cancel SOS`() = runTest {
        whenever(recordingManager.startEmergencyRecording(any())).thenThrow(RuntimeException("Camera unavailable"))
        engine.activate(TriggerMethod.BUTTON)
        // SOS must still be ACTIVE despite recording failure
        assertEquals(SosState.ACTIVE, engine.state.value?.state)
    }

    @Test
    fun `messaging failure does not cancel SOS`() = runTest {
        whenever(messagingOrchestrator.notifyContacts(any())).thenThrow(RuntimeException("Network unavailable"))
        engine.activate(TriggerMethod.BUTTON)
        assertEquals(SosState.ACTIVE, engine.state.value?.state)
    }
}
