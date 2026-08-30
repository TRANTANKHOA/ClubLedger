package com.example.data.model

import com.example.data.entity.Team
import com.example.data.entity.TeamMembership
import com.example.data.entity.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the superset permission hierarchy documented in the README:
 * Global Club Admin ⊃ Team Admin ⊃ Team Captain / Coach ⊃ Team Member.
 */
class PermissionEngineTest {

    private val globalAdmin = User(id = 1, name = "Marcus", email = "marcus@club.test", role = "ADMIN")
    private val plainMember = User(id = 2, name = "Alex", email = "alex@club.test", role = "MEMBER")

    private val team1 = 10L
    private val team2 = 20L

    private fun membership(userId: Long, teamId: Long, role: String) =
        TeamMembership(userId = userId, teamId = teamId, roleInTeam = role)

    // --- Global admin detection ---

    @Test
    fun `global admin is detected case-insensitively`() {
        assertTrue(PermissionEngine.isGlobalAdmin(globalAdmin))
        assertTrue(PermissionEngine.isGlobalAdmin(globalAdmin.copy(role = "admin")))
        assertFalse(PermissionEngine.isGlobalAdmin(plainMember))
        assertFalse("Null user must never be an admin", PermissionEngine.isGlobalAdmin(null))
    }

    // --- Superset rule: Global Admin ⊃ Team Admin ---

    @Test
    fun `global admin has team admin access on every team without membership`() {
        assertTrue(PermissionEngine.hasTeamAdminAccess(globalAdmin, team1, emptyList()))
        assertTrue(PermissionEngine.hasTeamAdminAccess(globalAdmin, team2, emptyList()))
    }

    @Test
    fun `team admin access is granted only on the led team`() {
        val memberships = listOf(membership(plainMember.id, team1, "ADMIN"))
        assertTrue(PermissionEngine.hasTeamAdminAccess(plainMember, team1, memberships))
        assertFalse("Team Admin of Team 1 must not administer Team 2", PermissionEngine.hasTeamAdminAccess(plainMember, team2, memberships))
    }

    @Test
    fun `captains and members are not team admins`() {
        assertFalse(PermissionEngine.hasTeamAdminAccess(plainMember, team1, listOf(membership(plainMember.id, team1, "CAPTAIN"))))
        assertFalse(PermissionEngine.hasTeamAdminAccess(plainMember, team1, listOf(membership(plainMember.id, team1, "MEMBER"))))
        assertFalse(PermissionEngine.hasTeamAdminAccess(plainMember, team1, emptyList()))
        assertFalse(PermissionEngine.hasTeamAdminAccess(null, team1, emptyList()))
    }

    // --- Superset rule: Team Admin ⊃ Team Captain ---

    @Test
    fun `team admin inherits all captain capabilities`() {
        val memberships = listOf(membership(plainMember.id, team1, "ADMIN"))
        assertTrue(PermissionEngine.hasTeamCaptainAccess(plainMember, team1, memberships))
    }

    @Test
    fun `captain coach and member captain access`() {
        assertTrue(PermissionEngine.hasTeamCaptainAccess(plainMember, team1, listOf(membership(plainMember.id, team1, "CAPTAIN"))))
        assertTrue(PermissionEngine.hasTeamCaptainAccess(plainMember, team1, listOf(membership(plainMember.id, team1, "COACH"))))
        assertFalse("Plain members must not gain captain tools", PermissionEngine.hasTeamCaptainAccess(plainMember, team1, listOf(membership(plainMember.id, team1, "MEMBER"))))
        assertFalse("Captain of Team 1 has no captain access on Team 2", PermissionEngine.hasTeamCaptainAccess(plainMember, team2, listOf(membership(plainMember.id, team1, "CAPTAIN"))))
    }

    // --- Leadership detection ---

    @Test
    fun `any leadership role across teams is detected`() {
        val captainOfTeam2 = listOf(membership(plainMember.id, team2, "CAPTAIN"))
        assertTrue(PermissionEngine.hasAnyLeadershipRole(plainMember, captainOfTeam2))
        assertFalse(PermissionEngine.hasAnyLeadershipRole(plainMember, listOf(membership(plainMember.id, team1, "MEMBER"))))
        assertTrue(PermissionEngine.hasAnyLeadershipRole(globalAdmin, emptyList()))
        assertFalse(PermissionEngine.hasAnyLeadershipRole(null, emptyList()))
    }

    // --- Led teams ---

    @Test
    fun `global admin leads every team in the club`() {
        val allTeams = listOf(team1, team2, 30L)
        assertEquals(allTeams, PermissionEngine.getLedTeamIds(globalAdmin, emptyList(), allTeams))
    }

    @Test
    fun `captain leads only the teams they captain`() {
        val memberships = listOf(
            membership(plainMember.id, team1, "CAPTAIN"),
            membership(plainMember.id, team2, "MEMBER")
        )
        assertEquals(listOf(team1), PermissionEngine.getLedTeamIds(plainMember, memberships, listOf(team1, team2)))
    }

    // --- Badge formatting ---

    @Test
    fun `badge for global admin without captaincies`() {
        assertEquals(
            "Global Club Admin 👑",
            PermissionEngine.formatMultiRoleBadge(globalAdmin, emptyList(), emptyMap())
        )
    }

    @Test
    fun `badge for global admin also captaining a team`() {
        val teams = mapOf(team1 to Team(id = team1, name = "Metro Lions", sportType = "Basketball"))
        val memberships = listOf(membership(globalAdmin.id, team1, "CAPTAIN"))
        assertEquals(
            "Club Admin 👑 (Captain: Metro Lions)",
            PermissionEngine.formatMultiRoleBadge(globalAdmin, memberships, teams)
        )
    }

    @Test
    fun `badge for multi-role member lists each leadership post`() {
        val teams = mapOf(
            team1 to Team(id = team1, name = "Metro Lions", sportType = "Basketball"),
            team2 to Team(id = team2, name = "Riverside FC", sportType = "Soccer")
        )
        val memberships = listOf(
            membership(plainMember.id, team1, "CAPTAIN"),
            membership(plainMember.id, team2, "MEMBER")
        )
        assertEquals(
            "Captain (Metro Lions)",
            PermissionEngine.formatMultiRoleBadge(plainMember, memberships, teams)
        )
    }

    @Test
    fun `badge for ordinary player and member with no teams`() {
        assertEquals(
            "Team Player ⚽",
            PermissionEngine.formatMultiRoleBadge(plainMember, listOf(membership(plainMember.id, team1, "MEMBER")), emptyMap())
        )
        assertEquals(
            "Member",
            PermissionEngine.formatMultiRoleBadge(plainMember, emptyList(), emptyMap())
        )
    }
}
