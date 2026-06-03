package com.nihaltp.smartringtone.data

object ContactSearchHelper {
    /**
     * Filters a list of Contacts based on a search query.
     * The search query is split into space-separated terms.
     * A contact matches if all terms in the query are found in either
     * the contact's name or the contact's phone number (case-insensitive).
     */
    fun filterContacts(
        contacts: List<Contact>,
        searchQuery: String,
    ): List<Contact> {
        val query = searchQuery.trim()
        if (query.isEmpty()) return contacts

        // Split by one or more whitespace characters
        val terms = query.split(Regex("\\s+"))

        return contacts.filter { contact ->
            terms.all { term ->
                contact.name.contains(term, ignoreCase = true) ||
                    contact.phone.contains(term, ignoreCase = true)
            }
        }
    }
}
