package com.nihaltp.smartringtone.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactSearchHelperTest {
    private val contacts =
        listOf(
            Contact("1", "Karthik RIT Hostel", "5616465145", null, null, 0, null),
            Contact("2", "John Doe", "1234567890", null, null, 0, null),
            Contact("3", "Hostel Office", "5551234", null, null, 0, null),
            Contact("4", "Karthik Prasad", "88888888", null, null, 0, null),
        )

    @Test
    fun testEmptyQueryReturnsAllContacts() {
        val result = ContactSearchHelper.filterContacts(contacts, "")
        assertEquals(contacts, result)
    }

    @Test
    fun testBlankQueryReturnsAllContacts() {
        val result = ContactSearchHelper.filterContacts(contacts, "   ")
        assertEquals(contacts, result)
    }

    @Test
    fun testSingleTermMatch() {
        val result = ContactSearchHelper.filterContacts(contacts, "John")
        assertEquals(1, result.size)
        assertEquals("John Doe", result[0].name)
    }

    @Test
    fun testCaseInsensitiveMatch() {
        val result = ContactSearchHelper.filterContacts(contacts, "karthik")
        assertEquals(2, result.size)
        assertEquals("Karthik RIT Hostel", result[0].name)
        assertEquals("Karthik Prasad", result[1].name)
    }

    @Test
    fun testFlexibleMultiTermMatch() {
        // Karthik Hostel should match Karthik RIT Hostel
        val result = ContactSearchHelper.filterContacts(contacts, "Karthik Hostel")
        assertEquals(1, result.size)
        assertEquals("Karthik RIT Hostel", result[0].name)
    }

    @Test
    fun testMultiTermDifferentOrderMatch() {
        // Hostel Karthik should match Karthik RIT Hostel
        val result = ContactSearchHelper.filterContacts(contacts, "Hostel Karthik")
        assertEquals(1, result.size)
        assertEquals("Karthik RIT Hostel", result[0].name)
    }

    @Test
    fun testPhoneMatch() {
        val result = ContactSearchHelper.filterContacts(contacts, "123")
        assertEquals(2, result.size)
        assertEquals("John Doe", result[0].name) // has 1234567890
        assertEquals("Hostel Office", result[1].name) // has 5551234
    }

    @Test
    fun testCombinedNameAndPhoneMatch() {
        // One term matches name, another term matches phone
        val result = ContactSearchHelper.filterContacts(contacts, "Karthik 5616")
        assertEquals(1, result.size)
        assertEquals("Karthik RIT Hostel", result[0].name)
    }

    @Test
    fun testNoMatch() {
        val result = ContactSearchHelper.filterContacts(contacts, "Karthik Mess")
        assertEquals(0, result.size)
    }
}
