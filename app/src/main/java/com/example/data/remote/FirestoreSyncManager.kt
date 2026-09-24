package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.dao.ClubDao
import com.example.data.entity.*
import com.example.util.SecurityDefenseHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class CloudSyncState {
    object Disabled : CloudSyncState()
    object Connecting : CloudSyncState()
    data class Active(val clubId: String, val lastSyncTime: Long) : CloudSyncState()
    data class Syncing(val message: String) : CloudSyncState()
    data class Error(val message: String) : CloudSyncState()
}

class FirestoreSyncManager(
    private val context: Context,
    private val dao: ClubDao,
    private val scope: CoroutineScope
) {
    private val TAG = "FirestoreSyncManager"

    private val _syncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Disabled)
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var activeClubId: String = "sports-club-demo"
    private val listeners = mutableListOf<ListenerRegistration>()

    /**
     * Denial-of-Wallet guard: refuses to push documents whose serialized payload
     * exceeds the safe byte cap, protecting cloud storage from bloat floods.
     */
    private fun isDocumentSafe(payload: Any): Boolean {
        val safe = SecurityDefenseHelper.isPayloadSizeSafe(payload.toString())
        if (!safe) {
            Log.w(TAG, "Blocked cloud push of oversized payload (${payload::class.simpleName}) exceeding Denial-of-Wallet byte cap")
        }
        return safe
    }

    init {
        checkFirebaseAvailability()
    }

    fun isFirebaseConfigured(): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun checkFirebaseAvailability() {
        try {
            if (isFirebaseConfigured()) {
                firestore = FirebaseFirestore.getInstance()
                auth = FirebaseAuth.getInstance()
            } else {
                Log.d(TAG, "Firebase is not configured with google-services.json yet.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization error: ${e.message}")
        }
    }

    fun enableCloudSync(clubId: String = "sports-club-demo") {
        activeClubId = clubId.trim().ifEmpty { "sports-club-demo" }
        _syncState.value = CloudSyncState.Connecting

        scope.launch(Dispatchers.IO) {
            try {
                if (firestore == null) {
                    checkFirebaseAvailability()
                }

                val db = firestore
                if (db == null) {
                    _syncState.value = CloudSyncState.Error(
                        "Firebase not initialized on device. Add google-services.json to sync across live devices."
                    )
                    return@launch
                }

                // Attach real-time listeners for multi-user remote updates
                attachRemoteListeners(db, activeClubId)

                // Push initial local state to cloud & update status
                pushAllLocalToCloud(activeClubId)
                _syncState.value = CloudSyncState.Active(activeClubId, System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable cloud sync", e)
                _syncState.value = CloudSyncState.Error(e.localizedMessage ?: "Sync connection failed")
            }
        }
    }

    fun disableCloudSync() {
        listeners.forEach { it.remove() }
        listeners.clear()
        _syncState.value = CloudSyncState.Disabled
    }

    private fun attachRemoteListeners(db: FirebaseFirestore, clubId: String) {
        listeners.forEach { it.remove() }
        listeners.clear()

        // 1. Listen for new/updated Attendances from remote members
        val attendanceListener = db.collection("clubs").document(clubId)
            .collection("attendances")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Attendance listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.getLong("id") ?: continue
                                val userId = doc.getLong("userId") ?: continue
                                val teamId = doc.getLong("teamId") ?: continue
                                val sessionDate = doc.getLong("sessionDate") ?: System.currentTimeMillis()
                                val sessionType = doc.getString("sessionType") ?: "TRAINING"
                                val status = doc.getString("status") ?: "PENDING"
                                val reviewNotes = doc.getString("reviewNotes")
                                val notes = doc.getString("notes") ?: ""
                                val submittedAt = doc.getLong("submittedAt") ?: System.currentTimeMillis()

                                val att = Attendance(
                                    id = id,
                                    userId = userId,
                                    teamId = teamId,
                                    sessionDate = sessionDate,
                                    sessionType = sessionType,
                                    status = status,
                                    reviewNotes = reviewNotes,
                                    notes = notes,
                                    submittedAt = submittedAt
                                )
                                dao.insertAttendance(att)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing remote attendance", e)
                            }
                        }
                    }
                }
            }
        listeners.add(attendanceListener)

        // 2. Listen for Payments from remote members / treasurers
        val paymentListener = db.collection("clubs").document(clubId)
            .collection("payments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && !snapshot.isEmpty) {
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.getLong("id") ?: continue
                                val userId = doc.getLong("userId") ?: continue
                                val amount = doc.getDouble("amount") ?: continue
                                val paymentDate = doc.getLong("paymentDate") ?: System.currentTimeMillis()
                                val paymentMethod = doc.getString("paymentMethod") ?: "CASH"
                                val referenceNote = doc.getString("referenceNote") ?: ""
                                val receiptNote = doc.getString("receiptNote")
                                val status = doc.getString("status") ?: "PENDING"
                                val submittedAt = doc.getLong("submittedAt") ?: System.currentTimeMillis()

                                val p = Payment(
                                    id = id,
                                    userId = userId,
                                    amount = amount,
                                    paymentDate = paymentDate,
                                    paymentMethod = paymentMethod,
                                    referenceNote = referenceNote,
                                    receiptNote = receiptNote,
                                    status = status,
                                    submittedAt = submittedAt
                                )
                                dao.insertPayment(p)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing remote payment", e)
                            }
                        }
                    }
                }
            }
        listeners.add(paymentListener)
    }

    suspend fun pushAllLocalToCloud(clubId: String = activeClubId) {
        val db = firestore ?: return
        _syncState.value = CloudSyncState.Syncing("Pushing local data to cloud...")

        try {
            val users = dao.getAllUsersOnce()
            val teams = dao.getAllTeamsOnce()
            val attendances = dao.getAllAttendancesOnce()
            val payments = dao.getAllPaymentsOnce()
            val budgets = dao.getAllBudgetsOnce()
            val ledgerEntries = dao.getAllLedgerEntriesOnce()

            val clubDoc = db.collection("clubs").document(clubId)
            clubDoc.set(mapOf("lastSyncedAt" to System.currentTimeMillis(), "clubId" to clubId), SetOptions.merge())

            // Sync users
            for (u in users) {
                if (!isDocumentSafe(u)) continue
                clubDoc.collection("users").document(u.id.toString()).set(u).await()
            }
            // Sync teams
            for (t in teams) {
                if (!isDocumentSafe(t)) continue
                clubDoc.collection("teams").document(t.id.toString()).set(t).await()
            }
            // Sync attendances with deterministic idempotency compound key
            for (a in attendances) {
                if (!isDocumentSafe(a)) continue
                val docId = SecurityDefenseHelper.createIdempotentAttendanceKey(
                    userId = a.userId,
                    teamId = a.teamId,
                    sessionDate = a.sessionDate,
                    sessionType = a.sessionType
                )
                clubDoc.collection("attendances").document(docId).set(a).await()
            }
            // Sync payments with deterministic idempotency compound key
            for (p in payments) {
                if (!isDocumentSafe(p)) continue
                val docId = SecurityDefenseHelper.createIdempotentPaymentKey(
                    userId = p.userId,
                    amount = p.amount,
                    paymentDate = p.paymentDate,
                    paymentMethod = p.paymentMethod
                )
                clubDoc.collection("payments").document(docId).set(p).await()
            }
            // Sync budgets
            for (b in budgets) {
                if (!isDocumentSafe(b)) continue
                clubDoc.collection("budgets").document(b.id.toString()).set(b).await()
            }
            // Sync ledger
            for (l in ledgerEntries) {
                if (!isDocumentSafe(l)) continue
                clubDoc.collection("ledger").document(l.id.toString()).set(l).await()
            }

            _syncState.value = CloudSyncState.Active(clubId, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing to cloud", e)
            _syncState.value = CloudSyncState.Error("Sync push failed: ${e.message}")
        }
    }

    suspend fun pushAttendanceToCloud(attendance: Attendance) {
        val db = firestore ?: return
        if (!isDocumentSafe(attendance)) return
        try {
            val docId = SecurityDefenseHelper.createIdempotentAttendanceKey(
                userId = attendance.userId,
                teamId = attendance.teamId,
                sessionDate = attendance.sessionDate,
                sessionType = attendance.sessionType
            )
            db.collection("clubs").document(activeClubId)
                .collection("attendances").document(docId)
                .set(attendance).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing attendance to cloud", e)
        }
    }

    suspend fun pushPaymentToCloud(payment: Payment) {
        val db = firestore ?: return
        if (!isDocumentSafe(payment)) return
        try {
            val docId = SecurityDefenseHelper.createIdempotentPaymentKey(
                userId = payment.userId,
                amount = payment.amount,
                paymentDate = payment.paymentDate,
                paymentMethod = payment.paymentMethod
            )
            db.collection("clubs").document(activeClubId)
                .collection("payments").document(docId)
                .set(payment).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing payment to cloud", e)
        }
    }

    suspend fun pushLedgerEntryToCloud(entry: BalanceLedger) {
        val db = firestore ?: return
        if (!isDocumentSafe(entry)) return
        try {
            db.collection("clubs").document(activeClubId)
                .collection("ledger").document(entry.id.toString())
                .set(entry).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing ledger to cloud", e)
        }
    }
}
