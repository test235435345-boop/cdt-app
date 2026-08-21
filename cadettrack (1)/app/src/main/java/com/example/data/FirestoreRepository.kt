package com.example.data

import android.util.Log
import com.example.model.AttendanceRecord
import com.example.model.BandEvent
import com.example.model.Cadet
import com.example.model.OrgSettings
import com.example.model.StaffMember
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // --- Org Settings ---
    private val orgSettingsDoc = firestore.collection("orgSettings").document("settings")

    fun observeOrgSettings(): Flow<OrgSettings?> = callbackFlow {
        val listener = orgSettingsDoc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreRepo", "observeOrgSettings error", error)
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(OrgSettings::class.java))
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveOrgSettings(settings: OrgSettings): Result<Unit> {
        return try {
            orgSettingsDoc.set(settings).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "saveOrgSettings error", e)
            Result.failure(e)
        }
    }

    suspend fun getOrgSettings(): OrgSettings? {
        return try {
            val doc = orgSettingsDoc.get().await()
            if (doc.exists()) doc.toObject(OrgSettings::class.java) else null
        } catch (e: Exception) {
            null
        }
    }

    // --- Staff Members ---
    private val staffCollection = firestore.collection("staffMembers")

    fun observeAllStaff(): Flow<List<StaffMember>> = callbackFlow {
        val listener = staffCollection.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "observeAllStaff error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val staffList = snapshot?.documents?.mapNotNull { it.toObject(StaffMember::class.java) } ?: emptyList()
                trySend(staffList)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateStaffMember(staff: StaffMember): Result<Unit> {
        return try {
            staffCollection.document(staff.uid).set(staff).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Cadets ---
    private val cadetsCollection = firestore.collection("cadets")

    fun observeCadets(): Flow<List<Cadet>> = callbackFlow {
        val listener = cadetsCollection.orderBy("lastName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "observeCadets error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val cadets = snapshot?.documents?.mapNotNull { it.toObject(Cadet::class.java) } ?: emptyList()
                trySend(cadets)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addCadet(cadet: Cadet): Result<String> {
        return try {
            val docRef = if (cadet.id.isBlank()) cadetsCollection.document() else cadetsCollection.document(cadet.id)
            val toSave = cadet.copy(id = docRef.id, updatedAt = System.currentTimeMillis())
            docRef.set(toSave).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCadet(cadet: Cadet): Result<Unit> {
        return try {
            cadetsCollection.document(cadet.id).set(cadet.copy(updatedAt = System.currentTimeMillis())).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveCadet(cadetId: String, newStatus: String): Result<Unit> {
        return try {
            cadetsCollection.document(cadetId).update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun batchImportCadets(cadets: List<Cadet>): Result<Int> {
        return try {
            val batch = firestore.batch()
            var count = 0
            for (cadet in cadets) {
                val docRef = if (cadet.id.isBlank()) cadetsCollection.document() else cadetsCollection.document(cadet.id)
                val c = cadet.copy(id = docRef.id, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
                batch.set(docRef, c)
                count++
            }
            batch.commit().await()
            Result.success(count)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "batchImportCadets error", e)
            Result.failure(e)
        }
    }

    // --- Events ---
    private val eventsCollection = firestore.collection("events")

    fun observeEvents(): Flow<List<BandEvent>> = callbackFlow {
        val listener = eventsCollection.orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "observeEvents error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.mapNotNull { it.toObject(BandEvent::class.java) } ?: emptyList()
                trySend(events)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createEvent(event: BandEvent): Result<String> {
        return try {
            val docRef = if (event.id.isBlank()) eventsCollection.document() else eventsCollection.document(event.id)
            val toSave = event.copy(id = docRef.id)
            docRef.set(toSave).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEvent(event: BandEvent): Result<Unit> {
        return try {
            eventsCollection.document(event.id).set(event).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelEvent(eventId: String, cancellationReason: String): Result<Unit> {
        return try {
            eventsCollection.document(eventId).update(
                mapOf(
                    "isCancelled" to true,
                    "cancellationReason" to cancellationReason
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Generates recurring empty event shells for the training year
    suspend fun generateYearEvents(
        startDateStr: String, // YYYY-MM-DD
        trainingDay: String, // e.g. "Tuesday"
        trainingTime: String, // "18:30 - 21:00"
        bandDay: String, // e.g. "Thursday"
        bandTime: String, // "18:30 - 21:00"
        location: String,
        squadrons: List<String>,
        weeksCount: Int = 38
    ): Result<Int> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val baseDate: Date = try {
                sdf.parse(startDateStr) ?: Date()
            } catch (e: Exception) {
                Date()
            }

            val cal = Calendar.getInstance()
            cal.time = baseDate

            fun parseTime(timeStr: String): Pair<String, String> {
                val parts = timeStr.split("-").map { it.trim() }
                return Pair(parts.getOrElse(0) { "18:30" }, parts.getOrElse(1) { "21:00" })
            }

            val (trainStart, trainEnd) = parseTime(trainingTime)
            val (bandStart, bandEnd) = parseTime(bandTime)

            val dayMap = mapOf(
                "Sunday" to Calendar.SUNDAY,
                "Monday" to Calendar.MONDAY,
                "Tuesday" to Calendar.TUESDAY,
                "Wednesday" to Calendar.WEDNESDAY,
                "Thursday" to Calendar.THURSDAY,
                "Friday" to Calendar.FRIDAY,
                "Saturday" to Calendar.SATURDAY
            )

            val trainDayCal = dayMap[trainingDay] ?: Calendar.TUESDAY
            val bandDayCal = dayMap[bandDay] ?: Calendar.THURSDAY

            val batch = firestore.batch()
            var createdCount = 0

            for (week in 0 until weeksCount) {
                val weekCal = Calendar.getInstance()
                weekCal.time = baseDate
                weekCal.add(Calendar.WEEK_OF_YEAR, week)

                // 1. Training Night event
                val tCal = Calendar.getInstance()
                tCal.time = weekCal.time
                tCal.set(Calendar.DAY_OF_WEEK, trainDayCal)
                val tDateStr = sdf.format(tCal.time)

                val tDoc = eventsCollection.document()
                val tEvent = BandEvent(
                    id = tDoc.id,
                    title = "Weekly Training Night",
                    date = tDateStr,
                    startTime = trainStart,
                    endTime = trainEnd,
                    squadron = "All",
                    type = BandEvent.TYPE_TRAINING_NIGHT,
                    location = location,
                    isCancelled = false
                )
                batch.set(tDoc, tEvent)
                createdCount++

                // 2. Band Practice event
                val bCal = Calendar.getInstance()
                bCal.time = weekCal.time
                bCal.set(Calendar.DAY_OF_WEEK, bandDayCal)
                val bDateStr = sdf.format(bCal.time)

                val bDoc = eventsCollection.document()
                val bEvent = BandEvent(
                    id = bDoc.id,
                    title = "Cadet Band Practice",
                    date = bDateStr,
                    startTime = bandStart,
                    endTime = bandEnd,
                    squadron = "All",
                    type = BandEvent.TYPE_BAND_PRACTICE,
                    location = location,
                    isCancelled = false
                )
                batch.set(bDoc, bEvent)
                createdCount++
            }

            batch.commit().await()
            Result.success(createdCount)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "generateYearEvents error", e)
            Result.failure(e)
        }
    }

    // --- Attendance Records ---
    private val attendanceCollection = firestore.collection("attendanceRecords")

    fun observeAttendanceForEvent(eventId: String): Flow<List<AttendanceRecord>> = callbackFlow {
        val listener = attendanceCollection.whereEqualTo("eventId", eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "observeAttendanceForEvent error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { it.toObject(AttendanceRecord::class.java) } ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    fun observeAllAttendance(): Flow<List<AttendanceRecord>> = callbackFlow {
        val listener = attendanceCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreRepo", "observeAllAttendance error", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val records = snapshot?.documents?.mapNotNull { it.toObject(AttendanceRecord::class.java) } ?: emptyList()
            trySend(records)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveAttendanceRecord(record: AttendanceRecord): Result<Unit> {
        return try {
            val docId = if (record.id.isNotBlank()) record.id else "${record.eventId}_${record.cadetId}"
            val toSave = record.copy(id = docId, timestamp = System.currentTimeMillis())
            attendanceCollection.document(docId).set(toSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun batchMarkAllPresent(
        eventId: String,
        activeCadetIds: List<String>,
        markedBy: String
    ): Result<List<AttendanceRecord>> {
        return try {
            val batch = firestore.batch()
            val records = mutableListOf<AttendanceRecord>()
            val now = System.currentTimeMillis()

            for (cadetId in activeCadetIds) {
                val docId = "${eventId}_${cadetId}"
                val docRef = attendanceCollection.document(docId)
                val record = AttendanceRecord(
                    id = docId,
                    cadetId = cadetId,
                    eventId = eventId,
                    status = AttendanceRecord.STATUS_PRESENT,
                    note = "",
                    timestamp = now,
                    markedBy = markedBy
                )
                batch.set(docRef, record)
                records.add(record)
            }
            batch.commit().await()
            Result.success(records)
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "batchMarkAllPresent error", e)
            Result.failure(e)
        }
    }

    suspend fun batchRevertAttendance(records: List<AttendanceRecord>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            for (record in records) {
                val docRef = attendanceCollection.document(record.id)
                batch.set(docRef, record)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
