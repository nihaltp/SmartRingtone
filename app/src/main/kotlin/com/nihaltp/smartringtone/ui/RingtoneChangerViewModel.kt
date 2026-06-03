package com.nihaltp.smartringtone.ui

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nihaltp.smartringtone.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RingtoneChangerViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context get() = getApplication()

    private val _ringtones = MutableStateFlow<List<Ringtone>>(emptyList())
    val ringtones: StateFlow<List<Ringtone>> = _ringtones

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    private val _callLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val callLogs: StateFlow<List<CallLogEntry>> = _callLogs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _playingUri = MutableStateFlow<String?>(null)
    val playingUri: StateFlow<String?> = _playingUri

    private var mediaPlayer: MediaPlayer? = null

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                // Read from helpers
                val loadedRingtones = PreferenceHelper.getRingtones(context)
                val loadedContacts = ContactHelper.getContacts(context)
                val loadedCallLogs = PreferenceHelper.getCallLogsHistory(context)

                _ringtones.value = loadedRingtones
                _contacts.value = loadedContacts
                _callLogs.value = loadedCallLogs
            }
            _isLoading.value = false
        }
    }

    fun syncCallLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                CallSyncHelper.syncCallLogs(context)
                val loadedContacts = ContactHelper.getContacts(context)
                val loadedCallLogs = PreferenceHelper.getCallLogsHistory(context)
                _contacts.value = loadedContacts
                _callLogs.value = loadedCallLogs
            }
            _isLoading.value = false
        }
    }

    fun addRingtone(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val added =
                withContext(Dispatchers.IO) {
                    RingtoneHelper.addRingtoneFromUri(context, uri)
                }
            if (added != null) {
                val currentList =
                    withContext(Dispatchers.IO) {
                        val list = PreferenceHelper.getRingtones(context).toMutableList()
                        list.add(added)
                        PreferenceHelper.saveRingtones(context, list)
                        list
                    }
                _ringtones.value = currentList
                _isLoading.value = false

                // Background update of contacts in system DB
                viewModelScope.launch(Dispatchers.IO) {
                    val contactsList = ContactHelper.getContacts(context)
                    ContactHelper.updateContactsRingtonesBasedOnScores(context, contactsList, currentList)
                    val loadedContacts = ContactHelper.getContacts(context)
                    _contacts.value = loadedContacts
                }
            } else {
                _isLoading.value = false
            }
        }
    }

    fun deleteRingtone(ringtoneId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentList =
                withContext(Dispatchers.IO) {
                    val list = PreferenceHelper.getRingtones(context).toMutableList()
                    list.removeAll { it.id == ringtoneId }
                    PreferenceHelper.saveRingtones(context, list)
                    list
                }
            _ringtones.value = currentList

            val updatedContacts =
                _contacts.value.map { contact ->
                    if (contact.score > 0) {
                        val idx = (contact.score - 1).coerceAtMost(currentList.size - 1)
                        val mappedName = if (currentList.isNotEmpty()) currentList[idx].name else "System Default"
                        contact.copy(mappedRingtoneName = mappedName)
                    } else {
                        contact
                    }
                }
            _contacts.value = updatedContacts
            _isLoading.value = false

            // Background update of contacts in system DB
            viewModelScope.launch(Dispatchers.IO) {
                val contactsList = ContactHelper.getContacts(context)
                ContactHelper.updateContactsRingtonesBasedOnScores(context, contactsList, currentList)
                val loadedContacts = ContactHelper.getContacts(context)
                _contacts.value = loadedContacts
            }
        }
    }

    fun moveRingtone(
        index: Int,
        up: Boolean,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentList =
                withContext(Dispatchers.IO) {
                    val list = PreferenceHelper.getRingtones(context).toMutableList()
                    val targetIndex = if (up) index - 1 else index + 1
                    if (targetIndex in 0 until list.size) {
                        val temp = list[index]
                        list[index] = list[targetIndex]
                        list[targetIndex] = temp
                        PreferenceHelper.saveRingtones(context, list)
                    }
                    list
                }

            _ringtones.value = currentList
            val updatedContacts =
                _contacts.value.map { contact ->
                    if (contact.score > 0 && currentList.isNotEmpty()) {
                        val idx = (contact.score - 1).coerceAtMost(currentList.size - 1)
                        contact.copy(mappedRingtoneName = currentList[idx].name)
                    } else {
                        contact
                    }
                }
            _contacts.value = updatedContacts
            _isLoading.value = false

            // Background update of contacts in system DB
            viewModelScope.launch(Dispatchers.IO) {
                val contactsList = ContactHelper.getContacts(context)
                ContactHelper.updateContactsRingtonesBasedOnScores(context, contactsList, currentList)
                val loadedContacts = ContactHelper.getContacts(context)
                _contacts.value = loadedContacts
            }
        }
    }

    fun resetContactScore(contactId: String) {
        viewModelScope.launch {
            val updatedContacts =
                _contacts.value.map { contact ->
                    if (contact.id == contactId) {
                        contact.copy(score = 0, mappedRingtoneName = "System Default", currentRingtone = null)
                    } else {
                        contact
                    }
                }
            _contacts.value = updatedContacts

            // Background update of preference and contact in system DB
            viewModelScope.launch(Dispatchers.IO) {
                PreferenceHelper.setContactScore(context, contactId, 0)
                ContactHelper.updateContactRingtoneBasedOnScore(context, contactId, 0)
                val loadedContacts = ContactHelper.getContacts(context)
                _contacts.value = loadedContacts
            }
        }
    }

    fun resetAllScores() {
        viewModelScope.launch {
            _isLoading.value = true
            val updatedContacts =
                _contacts.value.map { contact ->
                    contact.copy(score = 0, mappedRingtoneName = "System Default", currentRingtone = null)
                }
            _contacts.value = updatedContacts
            _isLoading.value = false

            // Background update of preferences and contacts in system DB
            viewModelScope.launch(Dispatchers.IO) {
                val contactsList = ContactHelper.getContacts(context)
                ContactHelper.resetAllScores(context, contactsList)
                val loadedContacts = ContactHelper.getContacts(context)
                _contacts.value = loadedContacts
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                PreferenceHelper.clearCallLogsHistory(context)
            }
            _callLogs.value = emptyList()
            _isLoading.value = false
        }
    }

    fun togglePlayPreview(uriString: String) {
        if (_playingUri.value == uriString) {
            stopPreview()
        } else {
            playPreview(uriString)
        }
    }

    private fun playPreview(uriString: String) {
        viewModelScope.launch {
            try {
                mediaPlayer?.release()
                mediaPlayer =
                    MediaPlayer().apply {
                        setDataSource(context, Uri.parse(uriString))
                        prepare()
                        start()
                        setOnCompletionListener {
                            stopPreview()
                        }
                    }
                _playingUri.value = uriString
            } catch (e: Exception) {
                stopPreview()
            }
        }
    }

    fun stopPreview() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            mediaPlayer = null
            _playingUri.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }
}
