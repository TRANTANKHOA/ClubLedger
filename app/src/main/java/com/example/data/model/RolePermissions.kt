package com.example.data.model

import com.example.data.entity.TeamMembership
import com.example.data.entity.User

object RoleConstants {
    const val GLOBAL_ADMIN = "ADMIN"
    const val GLOBAL_MEMBER = "MEMBER"

    const val TEAM_ADMIN = "ADMIN"
    const val TEAM_CAPTAIN = "CAPTAIN"
    const val TEAM_COACH = "COACH"
    const val TEAM_MEMBER = "MEMBER"
}

/**
 * Summary of user permissions across the club and individual teams
 */
data class UserRoleProfile(
    val user: User,
    val isGlobalAdmin: Boolean,
    val teamRoles: Map<Long, String>, // teamId -> roleInTeam ("ADMIN", "CAPTAIN", "COACH", "MEMBER")
    val summaryBadge: String
)

/**
 * Strict Superset Permission Engine:
 * Global Club Admin ⊃ Team Admin ⊃ Team Captain / Coach ⊃ Team Member
 */
object PermissionEngine {

    /**
     * Checks if user has Global Club Admin privileges.
     */
    fun isGlobalAdmin(user: User?): Boolean {
        return user?.role.equals(RoleConstants.GLOBAL_ADMIN, ignoreCase = true)
    }

    /**
     * Checks if user has Team Admin privileges for [teamId]
     * (True if Global Club Admin OR assigned as Team Admin for that team).
     * Superset Rule: Global Club Admin is a superset of all Team Admins.
     */
    fun hasTeamAdminAccess(user: User?, teamId: Long, memberships: List<TeamMembership>): Boolean {
        if (user == null) return false
        if (isGlobalAdmin(user)) return true
        val roleInTeam = memberships.firstOrNull { it.userId == user.id && it.teamId == teamId }?.roleInTeam
        return roleInTeam.equals(RoleConstants.TEAM_ADMIN, ignoreCase = true)
    }

    /**
     * Checks if user has Team Captain privileges for [teamId]
     * (True if Global Club Admin OR Team Admin on that team OR Team Captain / Coach on that team).
     * Superset Rule: Team Admin automatically has ALL Team Captain capabilities!
     */
    fun hasTeamCaptainAccess(user: User?, teamId: Long, memberships: List<TeamMembership>): Boolean {
        if (user == null) return false
        if (isGlobalAdmin(user)) return true
        val roleInTeam = memberships.firstOrNull { it.userId == user.id && it.teamId == teamId }?.roleInTeam ?: return false
        return roleInTeam.equals(RoleConstants.TEAM_ADMIN, ignoreCase = true) ||
               roleInTeam.equals(RoleConstants.TEAM_CAPTAIN, ignoreCase = true) ||
               roleInTeam.equals(RoleConstants.TEAM_COACH, ignoreCase = true)
    }

    /**
     * Checks if user holds Captain, Team Admin, or Global Admin leadership on AT LEAST one team.
     */
    fun hasAnyLeadershipRole(user: User?, memberships: List<TeamMembership>): Boolean {
        if (user == null) return false
        if (isGlobalAdmin(user)) return true
        return memberships.any {
            it.userId == user.id && (
                it.roleInTeam.equals(RoleConstants.TEAM_ADMIN, ignoreCase = true) ||
                it.roleInTeam.equals(RoleConstants.TEAM_CAPTAIN, ignoreCase = true) ||
                it.roleInTeam.equals(RoleConstants.TEAM_COACH, ignoreCase = true)
            )
        }
    }

    /**
     * Return all team IDs where this user has Team Captain or Team Admin permissions.
     */
    fun getLedTeamIds(user: User?, memberships: List<TeamMembership>, allTeamIds: List<Long>): List<Long> {
        if (user == null) return emptyList()
        if (isGlobalAdmin(user)) return allTeamIds
        return memberships
            .filter {
                it.userId == user.id && (
                    it.roleInTeam.equals(RoleConstants.TEAM_ADMIN, ignoreCase = true) ||
                    it.roleInTeam.equals(RoleConstants.TEAM_CAPTAIN, ignoreCase = true) ||
                    it.roleInTeam.equals(RoleConstants.TEAM_COACH, ignoreCase = true)
                )
            }
            .map { it.teamId }
    }

    /**
     * Formats a clear multi-role description string for a user across all teams.
     */
    fun formatMultiRoleBadge(user: User, memberships: List<TeamMembership>, teamsMap: Map<Long, com.example.data.entity.Team>): String {
        if (isGlobalAdmin(user)) {
            val captainTeams = memberships
                .filter { it.userId == user.id && (it.roleInTeam.equals(RoleConstants.TEAM_CAPTAIN, ignoreCase = true) || it.roleInTeam.equals(RoleConstants.TEAM_ADMIN, ignoreCase = true)) }
                .mapNotNull { teamsMap[it.teamId]?.name }
            return if (captainTeams.isNotEmpty()) {
                "Club Admin 👑 (Captain: ${captainTeams.joinToString()})"
            } else {
                "Global Club Admin 👑"
            }
        }

        val userMemberships = memberships.filter { it.userId == user.id }
        if (userMemberships.isEmpty()) return "Member"

        val leadership = userMemberships.filter {
            it.roleInTeam.equals(RoleConstants.TEAM_ADMIN, ignoreCase = true) ||
            it.roleInTeam.equals(RoleConstants.TEAM_CAPTAIN, ignoreCase = true) ||
            it.roleInTeam.equals(RoleConstants.TEAM_COACH, ignoreCase = true)
        }

        if (leadership.isNotEmpty()) {
            val parts = leadership.map { mem ->
                val teamName = teamsMap[mem.teamId]?.name ?: "Team #${mem.teamId}"
                val roleTitle = when (mem.roleInTeam.uppercase()) {
                    "ADMIN" -> "Team Admin"
                    "CAPTAIN" -> "Captain"
                    "COACH" -> "Coach"
                    else -> mem.roleInTeam
                }
                "$roleTitle ($teamName)"
            }
            return parts.joinToString(" • ")
        }

        return "Team Player ⚽"
    }
}
