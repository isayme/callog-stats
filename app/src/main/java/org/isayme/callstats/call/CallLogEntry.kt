package org.isayme.callstats.call

data class CallLogEntry(
    val number: String,
    val type: Int,
    val dateMillis: Long,
    val durationSeconds: Long,
)

object CallTypes {
    // Values mirror android.provider.CallLog.Calls so the domain layer can be unit tested.
    const val INCOMING = 1
    const val OUTGOING = 2
    const val MISSED = 3
    const val VOICEMAIL = 4
    const val REJECTED = 5
    const val BLOCKED = 6
    const val ANSWERED_EXTERNALLY = 7
}
