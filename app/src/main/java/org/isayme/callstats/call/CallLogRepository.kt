package org.isayme.callstats.call

import android.content.ContentResolver
import android.provider.CallLog
import android.util.Log

class CallLogRepository(private val contentResolver: ContentResolver) {

    /**
     * OEM call logs may store numbers as "+86 176 2120 3857" or with other
     * separators, so an SQL `NUMBER IN (...)` lookup is not reliable. Load the
     * (personal, local) log and match normalized values in memory instead.
     */
    fun loadMatchingCalls(number: String, mode: NumberMatchMode): List<CallLogEntry> {
        if (PhoneNumberMatcher.digitsOnly(number).isEmpty()) return emptyList()

        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
        )

        return try {
            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC",
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durationIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

                var scanned = 0
                val entries = buildList {
                    while (cursor.moveToNext()) {
                        scanned++
                        val storedNumber = cursor.getString(numberIndex).orEmpty()
                        if (!PhoneNumberMatcher.matches(storedNumber, number, mode)) continue
                        add(
                            CallLogEntry(
                                number = storedNumber,
                                type = cursor.getInt(typeIndex),
                                dateMillis = cursor.getLong(dateIndex),
                                durationSeconds = cursor.getLong(durationIndex),
                            ),
                        )
                    }
                }
                Log.d(TAG, "Scanned $scanned call log rows, matched ${entries.size}")
                entries
            }.orEmpty()
        } catch (error: SecurityException) {
            Log.w(TAG, "Call log permission is missing", error)
            emptyList()
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Unable to query call log", error)
            emptyList()
        }
    }

    private companion object {
        const val TAG = "CallLogRepository"
    }
}
