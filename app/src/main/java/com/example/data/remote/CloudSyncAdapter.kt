package com.example.data.remote

import com.example.data.entity.Attendance
import com.example.data.entity.BalanceLedger
import com.example.data.entity.Payment
import kotlinx.coroutines.flow.StateFlow

/**
 * Backend-neutral contract for the cloud sync relay.
 *
 * The ViewModel depends only on this interface; swapping Firebase for
 * Supabase, AWS AppSync, or a self-hosted backend means writing one new
 * implementation of this contract — no changes above the data layer.
 */
interface CloudSyncAdapter {
    val syncState: StateFlow<CloudSyncState>

    /** True when the backend SDK is initialized and usable on this device. */
    fun isBackendConfigured(): Boolean

    /** Starts listening for remote changes and pushes the local snapshot. */
    fun enableCloudSync(clubId: String)

    /** Detaches all remote listeners. */
    fun disableCloudSync()

    /** Pushes the full local snapshot to the currently active club. */
    suspend fun pushAllLocalToCloud()

    suspend fun pushAttendanceToCloud(attendance: Attendance)

    suspend fun pushPaymentToCloud(payment: Payment)

    suspend fun pushLedgerEntryToCloud(entry: BalanceLedger)
}
