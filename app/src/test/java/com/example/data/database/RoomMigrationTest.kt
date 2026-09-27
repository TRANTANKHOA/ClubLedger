package com.example.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.ClubDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the real v1 → v2 upgrade path against a hand-built v1 SQLite file.
 * The database is opened through Room with ONLY [ClubDatabase.MIGRATION_1_2]
 * wired — no destructive fallback — so Room's post-migration schema validation
 * acts as the correctness gate for both the migration and the expected v2 DDL.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "club-migration-test.db"
    private var room: ClubDatabase? = null

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        room?.close()
        context.deleteDatabase(dbName)
    }

    /** v1 schema: today's shapes for the 10 untouched tables, v1 shapes of team_budgets/invoices. */
    private fun createV1Database() {
        val db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        db.execSQL(
            "CREATE TABLE `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `email` TEXT NOT NULL, `phone` TEXT NOT NULL, " +
                "`role` TEXT NOT NULL, `avatarColorHex` INTEGER NOT NULL, " +
                "`joinedDate` INTEGER NOT NULL, `status` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE `teams` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `sportType` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                "`colorHex` INTEGER NOT NULL, `monthlyBudgetGoal` REAL NOT NULL, " +
                "`inviteCode` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE `team_memberships` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`userId` INTEGER NOT NULL, `teamId` INTEGER NOT NULL, " +
                "`roleInTeam` TEXT NOT NULL, `joinedDate` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE `attendances` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`userId` INTEGER NOT NULL, `teamId` INTEGER NOT NULL, `sessionDate` INTEGER NOT NULL, " +
                "`sessionType` TEXT NOT NULL, `notes` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                "`submittedAt` INTEGER NOT NULL, `reviewedAt` INTEGER, `reviewedByUserId` INTEGER, " +
                "`reviewNotes` TEXT)"
        )
        db.execSQL(
            "CREATE TABLE `payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`userId` INTEGER NOT NULL, `amount` REAL NOT NULL, `paymentDate` INTEGER NOT NULL, " +
                "`paymentMethod` TEXT NOT NULL, `referenceNote` TEXT NOT NULL, `receiptNote` TEXT, " +
                "`status` TEXT NOT NULL, `submittedAt` INTEGER NOT NULL, `reviewedAt` INTEGER, " +
                "`reviewedByUserId` INTEGER, `reviewNotes` TEXT)"
        )
        db.execSQL(
            "CREATE TABLE `team_budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`teamId` INTEGER NOT NULL, `periodMonth` INTEGER NOT NULL, `periodYear` INTEGER NOT NULL, " +
                "`totalAmount` REAL NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE `invoices` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`budgetId` INTEGER NOT NULL, `teamId` INTEGER NOT NULL, `invoiceNumber` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, `totalAmount` REAL NOT NULL, `periodMonth` INTEGER NOT NULL, " +
                "`periodYear` INTEGER NOT NULL, `status` TEXT NOT NULL, `issuedAt` INTEGER NOT NULL, " +
                "`totalApprovedSessions` INTEGER NOT NULL, `costPerSession` REAL NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE `invoice_allocations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`invoiceId` INTEGER NOT NULL, `userId` INTEGER NOT NULL, `teamId` INTEGER NOT NULL, " +
                "`approvedSessionsCount` INTEGER NOT NULL, `percentage` REAL NOT NULL, " +
                "`allocatedAmount` REAL NOT NULL, `status` TEXT NOT NULL, `calculatedAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE `balance_ledger` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`userId` INTEGER NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, " +
                "`runningBalanceAfter` REAL NOT NULL, `referenceType` TEXT NOT NULL, " +
                "`referenceId` INTEGER NOT NULL, `description` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `createdByUserId` INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE `audit_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`action` TEXT NOT NULL, `entityType` TEXT NOT NULL, `entityId` INTEGER NOT NULL, " +
                "`performedByUserId` INTEGER NOT NULL, `details` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE `disputes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`userId` INTEGER NOT NULL, `category` TEXT NOT NULL, `referenceType` TEXT NOT NULL, " +
                "`referenceId` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                "`requestedAdjustmentAmount` REAL NOT NULL, `status` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `resolvedAt` INTEGER, `resolvedByUserId` INTEGER, " +
                "`resolutionNotes` TEXT)"
        )
        db.execSQL(
            "CREATE TABLE `team_join_requests` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`teamId` INTEGER NOT NULL, `applicantName` TEXT NOT NULL, `applicantEmail` TEXT NOT NULL, " +
                "`applicantPhone` TEXT NOT NULL, `message` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `reviewedAt` INTEGER, `reviewedByUserId` INTEGER, " +
                "`reviewNotes` TEXT, `existingUserId` INTEGER)"
        )

        // Legacy rows written before the category/attachment columns existed.
        db.execSQL(
            "INSERT INTO `team_budgets` " +
                "(`id`,`teamId`,`periodMonth`,`periodYear`,`totalAmount`,`title`,`description`,`status`,`createdAt`) " +
                "VALUES (1, 1, 8, 2026, 600.0, 'August Pitch Rental', 'legacy description', 'APPROVED', 1754000000000)"
        )
        db.execSQL(
            "INSERT INTO `invoices` " +
                "(`id`,`budgetId`,`teamId`,`invoiceNumber`,`title`,`totalAmount`,`periodMonth`,`periodYear`,`status`,`issuedAt`,`totalApprovedSessions`,`costPerSession`) " +
                "VALUES (1, 1, 1, 'INV-2026-8-LEG1', 'Legacy Allocation', 600.0, 8, 2026, 'ISSUED', 1754000001000, 10, 60.0)"
        )

        db.version = 1
        db.close()
    }

    @Test
    fun `migration 1 to 2 preserves rows, applies category default, leaves new columns null`() = runTest {
        createV1Database()

        val database = Room.databaseBuilder(context, ClubDatabase::class.java, dbName)
            .addMigrations(ClubDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        room = database
        val dao: ClubDao = database.clubDao()

        val budget = dao.getBudgetById(1)
        assertEquals("August Pitch Rental", budget?.title)
        assertEquals("legacy description", budget?.description)
        assertEquals(600.0, budget?.totalAmount!!, 0.0)
        assertEquals(8, budget?.periodMonth)
        assertEquals(2026, budget?.periodYear)
        assertEquals("APPROVED", budget?.status)
        // Defaults supplied by the migration, mirroring the Kotlin signatures:
        assertEquals("COURT_RENTAL", budget?.category)
        // Nullable new columns are NULL for migrated (pre-v2) rows:
        assertNull(budget?.attachmentUrl)
        assertNull(budget?.attachmentType)
        assertNull(budget?.attachmentName)
        assertNull(budget?.declaredByUserId)

        val invoice = dao.getInvoiceById(1)
        assertEquals("INV-2026-8-LEG1", invoice?.invoiceNumber)
        assertEquals("Legacy Allocation", invoice?.title)
        assertEquals(600.0, invoice?.totalAmount!!, 0.0)
        assertEquals(10, invoice?.totalApprovedSessions)
        assertEquals(60.0, invoice?.costPerSession!!, 0.0)
        assertEquals("COURT_RENTAL", invoice?.category)
        assertNull(invoice?.attachmentUrl)
        assertNull(invoice?.attachmentType)
        assertNull(invoice?.attachmentName)

        val raw = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        assertEquals("Room must persist the upgraded version marker", 2, raw.version)
        raw.close()
    }
}
