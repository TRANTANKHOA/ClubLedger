package com.example.util

import android.net.Uri

/**
 * Parses the app's share deep links (`clubledger://join?code=TEAM-1234`).
 * Extracted from MainActivity so the parsing rules are unit-testable.
 */
object DeepLinks {

    /** Returns the trimmed invite code, or null when the uri is absent or not a join link. */
    fun parseJoinCode(uri: Uri?): String? {
        val code = uri
            ?.takeIf {
                it.scheme?.equals("clubledger", ignoreCase = true) == true &&
                    it.host?.equals("join", ignoreCase = true) == true
            }
            ?.getQueryParameter("code")
            ?.trim()
        return code?.takeIf { it.isNotEmpty() }
    }
}
