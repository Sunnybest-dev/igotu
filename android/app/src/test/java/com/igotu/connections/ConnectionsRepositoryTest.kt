package com.igotu.connections

import com.igotu.data.ApiService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.util.UUID

class ConnectionsRepositoryTest {

    private val api: ApiService = mock()
    private val repository = ConnectionsRepository(api)

    private fun mockConnection(id: String = UUID.randomUUID().toString()) = IgotuConnection(
        connectionId = id,
        userId = "user1",
        connectedUserId = "user2",
        status = ConnectionStatus.CONNECTED,
        permissions = ConnectionPermissions(),
        createdAt = Instant.now()
    )

    @Test
    fun `getConnections returns list from api`() = runTest {
        val expected = listOf(mockConnection())
        whenever(api.getConnections()).thenReturn(expected)
        val result = repository.getConnections()
        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }

    @Test
    fun `sendRequest delegates to api`() = runTest {
        val request = ConnectionRequest(
            requestId = UUID.randomUUID().toString(),
            fromUserId = "user1",
            toUserId = "user2",
            invitationToken = "tok",
            createdAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(3600)
        )
        whenever(api.sendConnectionRequest(any())).thenReturn(request)
        val result = repository.sendRequest("user2")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `api failure returns failure result`() = runTest {
        whenever(api.getConnections()).thenThrow(RuntimeException("Network error"))
        val result = repository.getConnections()
        assertTrue(result.isFailure)
    }

    @Test
    fun `default permissions do not grant live location`() {
        val perms = ConnectionPermissions()
        assertFalse(perms.receiveLiveLocation)
        assertFalse(perms.receiveEmergencyRecordings)
        assertTrue(perms.receiveSosAlerts)
    }
}
