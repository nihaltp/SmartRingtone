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
            val added = withContext(Dispatchers.IO) {
                RingtoneHelper.addRingtoneFromUri(context, uri)
            }
            if (added != null) {
                withContext(Dispatchers.IO) {
                    val currentList = PreferenceHelper.getRingtones(context).toMutableList()
                    currentList.add(added)
                    PreferenceHelper.saveRingtones(context, currentList)
                    
                    // Update contacts' ringtones based on new list
                    val contactsList = ContactHelper.getContacts(context)
                    for (c in contactsList) {
                        if (c.score > 0) {
                            ContactHelper.updateContactRingtoneBasedOnScore(context, c.id, c.score)
                        }
                    }
                }
                loadData()
            } else {
                _isLoading.value = false
            }
        }
    }

    fun deleteRingtone(ringtoneId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                val currentList = PreferenceHelper.getRingtones(context).toMutableList()
                currentList.removeAll { it.id == ringtoneId }
                PreferenceHelper.saveRingtones(context, currentList)

                // Update contacts' ringtones (in case their mapped ringtone changed index or was deleted)
                val contactsList = ContactHelper.getContacts(context)
                for (c in contactsList) {
                    if (c.score > 0) {
                        ContactHelper.updateContactRingtoneBasedOnScore(context, c.id, c.score)
                    }
                }
            }
            loadData()
        }
    }

    fun moveRingtone(index: Int, up: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                val currentList = PreferenceHelper.getRingtones(context).toMutableList()
                val targetIndex = if (up) index - 1 else index + 1
                if (targetIndex in 0 until currentList.size) {
                    val temp = currentList[index]
                    currentList[index] = currentList[targetIndex]
                    currentList[targetIndex] = temp
                    PreferenceHelper.saveRingtones(context, currentList)

                    // Re-apply ringtones as their index maps might have changed
                    val contactsList = ContactHelper.getContacts(context)
                    for (c in contactsList) {
                        if (c.score > 0) {
                            ContactHelper.updateContactRingtoneBasedOnScore(context, c.id, c.score)
                        }
                    }
                }
            }
            loadData()
        }
    }

    fun resetContactScore(contactId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                PreferenceHelper.setContactScore(context, contactId, 0)
                ContactHelper.updateContactRingtoneBasedOnScore(context, contactId, 0)
            }
            loadData()
        }
    }

    fun resetAllScores() {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                val contactsList = ContactHelper.getContacts(context)
                for (c in contactsList) {
                    if (c.score > 0) {
                        PreferenceHelper.setContactScore(context, c.id, 0)
                        ContactHelper.updateContactRingtoneBasedOnScore(context, c.id, 0)
                    }
                }
            }
            loadData()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                PreferenceHelper.clearCallLogsHistory(context)
            }
            loadData()
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
                mediaPlayer = MediaPlayer().apply {
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
