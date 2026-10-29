package com.igotu.sos

import android.util.Log
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SosAuditLogger @Inject constructor() {

    private val tag = "SosAudit"

    fun logTrigger(event: SosEvent) {
        log("TRIGGER id=${event.id} method=${event.triggerMethod} voiceConf=${event.voiceConfidence} at=${event.triggeredAt}")
    }

    fun logAction(type: SosAction.ActionType, status: SosAction.ActionStatus, error: String? = null) {
        val msg = "ACTION type=$type status=$status${error?.let { " error=$it" } ?: ""} at=${Instant.now()}"
        if (status == SosAction.ActionStatus.FAILED) Log.e(tag, msg) else log(msg)
    }

    fun logResolution(eventId: String) = log("RESOLVED id=$eventId at=${Instant.now()}")
    fun logCancellation(eventId: String) = log("CANCELLED id=$eventId at=${Instant.now()}")

    private fun log(msg: String) = Log.i(tag, msg)
    // TODO: persist audit entries to Room for backend sync
}
