package com.example.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class OrgSettings(
    val orgName: String = "",
    val squadronNumbers: List<String> = emptyList(),
    val homeLocation: String = "",
    val defaultTrainingNightDay: String = "Tuesday",
    val defaultTrainingNightTime: String = "18:30 - 21:00",
    val defaultBandPracticeDay: String = "Saturday",
    val defaultBandPracticeTime: String = "09:00 - 12:00",
    val attendanceThreshold: Int = 75,
    val trainingYear: String = "2026-2027",
    val googleSheetId: String = "",
    val googleSheetUrl: String = "",
    val googleSheetLastSynced: Long = 0L
)

@IgnoreExtraProperties
data class StaffMember(
    @DocumentId
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val rank: String = "",
    val role: String = "",
    val authorizedSquadrons: List<String> = emptyList(),
    val isAdmin: Boolean = false,
    val isAuthorized: Boolean = false,
    val canSyncSheets: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class Cadet(
    @DocumentId
    val id: String = "",
    val lastName: String = "",
    val firstName: String = "",
    val rank: String = "Cdt",
    val phone: String = "",
    val email: String = "",
    val squadron: String = "",
    val flight: String = "",
    val appointment: String = "",
    val instrument: String = "",
    val status: String = STATUS_ACTIVE, // Active, Released, Transferred
    val parentName: String = "",
    val parentPhone: String = "",
    val parentEmail: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    @get:Exclude
    val fullName: String
        get() = if (rank.isNotBlank()) "$rank $lastName, $firstName" else "$lastName, $firstName"

    @get:Exclude
    val displayName: String
        get() = "$firstName $lastName"

    companion object {
        const val STATUS_ACTIVE = "Active"
        const val STATUS_RELEASED = "Released"
        const val STATUS_TRANSFERRED = "Transferred"
    }
}

@IgnoreExtraProperties
data class BandEvent(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val date: String = "", // YYYY-MM-DD
    val startTime: String = "18:30",
    val endTime: String = "21:00",
    val squadron: String = "All", // "All" or specific squadron number
    val type: String = TYPE_TRAINING_NIGHT, // Training Night, Band Practice, Parade, Special Event
    val location: String = "",
    val isCancelled: Boolean = false,
    val cancellationReason: String = "",
    val notes: String = ""
) {
    companion object {
        const val TYPE_TRAINING_NIGHT = "Training Night"
        const val TYPE_BAND_PRACTICE = "Band Practice"
        const val TYPE_PARADE = "Parade"
        const val TYPE_SPECIAL = "Special Event"
    }
}

@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId
    val id: String = "",
    val cadetId: String = "",
    val eventId: String = "",
    val status: String = STATUS_UNMARKED, // Present, Absent, Absent-Notified, Late, Excused
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val markedBy: String = ""
) {
    companion object {
        const val STATUS_UNMARKED = "Unmarked"
        const val STATUS_PRESENT = "Present"
        const val STATUS_ABSENT = "Absent"
        const val STATUS_ABSENT_NOTIFIED = "Absent-Notified"
        const val STATUS_LATE = "Late"
        const val STATUS_EXCUSED = "Excused"
    }
}

data class CadetAttendanceStats(
    val cadet: Cadet,
    val totalEvents: Int,
    val attendedCount: Int, // Present + Late
    val excusedCount: Int,
    val absentCount: Int,
    val absentNotifiedCount: Int,
    val percentage: Float, // 0 - 100
    val isBelowThreshold: Boolean,
    val records: List<Pair<BandEvent, AttendanceRecord>> = emptyList()
)

data class SquadronReport(
    val squadron: String,
    val activeCadetsCount: Int,
    val averageAttendanceRate: Float,
    val totalEventsHeld: Int,
    val flaggedCadetsCount: Int
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "gemini"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val blocks: List<ChatBlock> = emptyList(),
    val tableHeaders: List<String> = emptyList(),
    val tableRows: List<List<String>> = emptyList()
)

data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "New Conversation",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList()
)
