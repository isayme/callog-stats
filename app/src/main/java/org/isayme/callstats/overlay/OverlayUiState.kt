package org.isayme.callstats.overlay

import org.isayme.callstats.call.CallStats

enum class CallPhase {
    INCOMING,
    OUTGOING,
    IN_CALL,
}

data class OverlayUiState(
    val number: String,
    val phase: CallPhase,
    val stats: CallStats? = null,
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val collapsed: Boolean = false,
    val preview: Boolean = false,
    val duplicateWindowMinutes: Int = 10,
)
