package com.example.util

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the clubledger://join deep-link parsing rules that MainActivity
 * relies on to pre-open the join-team dialog with a prefilled code.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeepLinksTest {

    @Test
    fun `valid join link yields the invite code`() {
        assertEquals("RIVERSIDE-26", DeepLinks.parseJoinCode(Uri.parse("clubledger://join?code=RIVERSIDE-26")))
    }

    @Test
    fun `code is trimmed and url-encoded characters are decoded`() {
        assertEquals("TEAM-123", DeepLinks.parseJoinCode(Uri.parse("clubledger://join?code=TEAM-123")))
        assertEquals("TEAM 123", DeepLinks.parseJoinCode(Uri.parse("clubledger://join?code=TEAM%20123")))
    }

    @Test
    fun `blank or missing code yields null so the dialog stays closed`() {
        assertNull(DeepLinks.parseJoinCode(Uri.parse("clubledger://join?code=")))
        assertNull(DeepLinks.parseJoinCode(Uri.parse("clubledger://join?code=%20%20")))
        assertNull(DeepLinks.parseJoinCode(Uri.parse("clubledger://join")))
        assertNull(DeepLinks.parseJoinCode(null))
    }

    @Test
    fun `non-join uris are ignored`() {
        assertNull("https share links are not deep links", DeepLinks.parseJoinCode(Uri.parse("https://clubledger.app/join?team=X")))
        assertNull(DeepLinks.parseJoinCode(Uri.parse("clubledger://team?code=X")))
        assertNull(DeepLinks.parseJoinCode(Uri.parse("mailto:treasurer@club.test")))
    }

    @Test
    fun `scheme and host match case-insensitively`() {
        // Uri normalizes the scheme to lowercase; an uppercase host must still match.
        assertEquals("ALPHA-1", DeepLinks.parseJoinCode(Uri.parse("CLUBLEDGER://join?code=ALPHA-1")))
        assertEquals("ALPHA-2", DeepLinks.parseJoinCode(Uri.parse("clubledger://JOIN?code=ALPHA-2")))
    }
}
