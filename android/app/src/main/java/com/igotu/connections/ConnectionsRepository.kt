package com.igotu.connections

import com.igotu.data.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionsRepository @Inject constructor(
    private val api: ApiService
) {
    suspend fun getConnections(): Result<List<IgotuConnection>> = runCatching {
        api.getConnections()
    }

    suspend fun sendRequest(toUserId: String): Result<ConnectionRequest> = runCatching {
        api.sendConnectionRequest(mapOf("toUserId" to toUserId))
    }

    suspend fun acceptRequest(requestId: String): Result<IgotuConnection> = runCatching {
        api.acceptConnectionRequest(requestId)
    }

    suspend fun rejectRequest(requestId: String): Result<Unit> = runCatching {
        api.rejectConnectionRequest(requestId)
    }

    suspend fun removeConnection(connectionId: String): Result<Unit> = runCatching {
        api.removeConnection(connectionId)
    }

    suspend fun updatePermissions(
        connectionId: String,
        permissions: ConnectionPermissions
    ): Result<IgotuConnection> = runCatching {
        api.updateConnectionPermissions(connectionId, permissions)
    }

    suspend fun blockUser(connectionId: String): Result<Unit> = runCatching {
        api.blockConnection(connectionId)
    }

    suspend fun createInvitation(): Result<ConnectionInvitation> = runCatching {
        api.createConnectionInvitation()
    }
}
