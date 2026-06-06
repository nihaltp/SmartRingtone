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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RingtoneChangerViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context get() = getApplication()
    private val stateMutex = Mutex()

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

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error

    private val _isLoggingEnabled = MutableStateFlow(false)
    val isLoggingEnabled: StateFlow<Boolean> = _isLoggingEnabled

    private val _logsText = MutableStateFlow("")
    val logsText: StateFlow<String> = _logsText

    private val _theme = MutableStateFlow("system")
    val theme: StateFlow<String> = _theme

    private val _fallbackRingtoneUri = MutableStateFlow<String?>(null)
    val fallbackRingtoneUri: StateFlow<String?> = _fallbackRingtoneUri

    private val _fallbackRingtoneName = MutableStateFlow<String?>(null)
    val fallbackRingtoneName: StateFlow<String?> = _fallbackRingtoneName

    private val _isAppPaused = MutableStateFlow(false)
    val isAppPaused: StateFlow<Boolean> = _isAppPaused

    private var mediaPlayer: MediaPlayer? = null
    private var currentRingtone: android.media.Ringtone? = null

    init {
        _isLoggingEnabled.value = AppLogger.isLoggingEnabled(context)
        _theme.value = PreferenceHelper.getTheme(context)
        _fallbackRingtoneUri.value = PreferenceHelper.getFallbackRingtoneUri(context)
        _fallbackRingtoneName.value = PreferenceHelper.getFallbackRingtoneName(context)
        _isAppPaused.value = PreferenceHelper.isAppPaused(context)
        loadData()
    }

    fun clearError() {
        _error.value = null
    }

    fun setTheme(themeValue: String) {
        PreferenceHelper.setTheme(context, themeValue)
        _theme.value = themeValue
        AppLogger.log(context, "ViewModel", "Theme changed to: $themeValue")
    }

    fun setLoggingEnabled(enabled: Boolean) {
        AppLogger.setLoggingEnabled(context, enabled)
        _isLoggingEnabled.value = enabled
        AppLogger.log(context, "ViewModel", "Logging state changed to: $enabled")
        loadLogs()
    }

    fun loadLogs() {
        _logsText.value = AppLogger.getLogs(context)
    }

    fun clearLogs() {
        AppLogger.clearLogs(context)
        _logsText.value = ""
    }

    fun loadData() {
        viewModelScope.launch {
            stateMutex.withLock {
                AppLogger.log(context, "ViewModel", "loadData() started")
                _isLoading.value = true
                try {
                    withContext(Dispatchers.IO) {
                        val loadedRingtones = PreferenceHelper.getRingtones(context)
                        val loadedContacts = ContactHelper.getContacts(context)
                        val loadedCallLogs = PreferenceHelper.getCallLogsHistory(context)

                        _ringtones.value = loadedRingtones
                        _contacts.value = loadedContacts
                        _callLogs.value = loadedCallLogs
                    }
                    AppLogger.log(
                        context,
                        "ViewModel",
                        "loadData() completed. " +
                            "Ringtones: ${_ringtones.value.size}, " +
                            "Contacts: ${_contacts.value.size}, " +
                            "CallLogs: ${_callLogs.value.size}",
                    )
                } catch (e: Exception) {
                    AppLogger.log(context, "ViewModel", "loadData() failed", e)
                    _error.value = e
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun syncCallLogs() {
        viewModelScope.launch {
            stateMutex.withLock {
                AppLogger.log(context, "ViewModel", "syncCallLogs() started")
                _isLoading.value = true
                try {
                    withContext(Dispatchers.IO) {
                        CallSyncHelper.syncCallLogs(context)
                        val loadedContacts = ContactHelper.getContacts(context)
                        val loadedCallLogs = PreferenceHelper.getCallLogsHistory(context)
                        _contacts.value = loadedContacts
                        _callLogs.value = loadedCallLogs
                    }
                    AppLogger.log(context, "ViewModel", "syncCallLogs() completed successfully")
                } catch (e: Exception) {
                    AppLogger.log(context, "ViewModel", "syncCallLogs() failed", e)
                    _error.value = e
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun addRingtone(uri: Uri) {
        viewModelScope.launch {
            stateMutex.withLock {
                AppLogger.log(context, "ViewModel", "addRingtone() started: uri=$uri")
                _isLoading.value = true
                try {
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
                        AppLogger.log(context, "ViewModel", "addRingtone() added: name=${added.name}")

                        // Background update of contacts in system DB
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                val contactsList = ContactHelper.getContacts(context)
                                ContactHelper.updateContactsRingtonesBasedOnScores(context, contactsList, currentList)
                                val loadedContacts = ContactHelper.getContacts(context)
                                _contacts.value = loadedContacts
                                AppLogger.log(context, "ViewModel", "Background contacts ringtones sync completed")
                            } catch (bgEx: Exception) {
                                AppLogger.log(context, "ViewModel", "Background contacts ringtones sync failed", bgEx)
                            }
                        }
                    } else {
                        AppLogger.log(context, "ViewModel", "addRingtone() failed to resolve ringtone from URI")
                        _error.value = Exception("Failed to resolve audio file from URI: $uri")
                    }
                } catch (e: Exception) {
                    AppLogger.log(context, "ViewModel", "addRingtone() failed", e)
                    _error.value = e
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun deleteRingtone(ringtoneId: Int) {
        viewModelScope.launch {
            stateMutex.withLock {
                AppLogger.log(context, "ViewModel", "deleteRingtone() started: id=$ringtoneId")
                _isLoading.value = true
                try {
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
                    AppLogger.log(context, "ViewModel", "deleteRingtone() deleted from preferences")

                    // Background update of contacts in system DB
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val contactsList = ContactHelper.getContacts(context)
                            ContactHelper.updateContactsRingtonesBasedOnScores(context, contactsList, currentList)
                            val loadedContacts = ContactHelper.getContacts(context)
                            _contacts.value = loadedContacts
                            AppLogger.log(context, "ViewModel", "Background contacts ringtones sync completed after deletion")
                        } catch (bgEx: Exception) {
                            AppLogger.log(context, "ViewModel", "Background contacts ringtones sync after deletion failed", bgEx)
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.log(context, "ViewModel", "deleteRingtone() failed", e)
                    _error.value = e
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun moveRingtone(
        index: Int,
        up: Boolean,
    ) {
        viewModelScope.launch {
            stateMutex.withLock {
                AppLogger.log(context, "ViewModel", "moveRingtone() started: index=$index, up=$up")
                _isLoading.value = true
                try {
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
                    AppLogger.log(context, "ViewModel", "moveRingtone() completed")

                    // Background update of contacts in system DB
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val contactsList = ContactHelper.getContacts(context)
                            ContactHelper.updateContactsRingtonesBasedOnScores(context, contactsList, currentList)
                            val loadedContacts = ContactHelper.getContacts(context)
                            _contacts.value = loadedContacts
                            AppLogger.log(context, "ViewModel", "Background contacts ringtones sync completed after reordering")
                        } catch (bgEx: Exception) {
                            AppLogger.log(context, "ViewModel", "Background contacts ringtones sync after reordering failed", bgEx)
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.log(context, "ViewModel", "moveRingtone() failed", e)
                    _error.value = e
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun resetContactScore(contactId: String) {
        viewModelScope.launch {
            AppLogger.log(context, "ViewModel", "resetContactScore() started: contactId=$contactId")
            try {
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
                    try {
                        PreferenceHelper.setContactScore(context, contactId, 0)
                        ContactHelper.updateContactRingtoneBasedOnScore(context, contactId, 0)
                        val loadedContacts = ContactHelper.getContacts(context)
                        _contacts.value = loadedContacts
                        AppLogger.log(context, "ViewModel", "Background reset completed for contact $contactId")
                    } catch (bgEx: Exception) {
                        AppLogger.log(context, "ViewModel", "Background reset failed for contact $contactId", bgEx)
                    }
                }
            } catch (e: Exception) {
                AppLogger.log(context, "ViewModel", "resetContactScore() failed", e)
                _error.value = e
            }
        }
    }

    fun resetAllScores() {
        viewModelScope.launch {
            AppLogger.log(context, "ViewModel", "resetAllScores() started")
            _isLoading.value = true
            try {
                val updatedContacts =
                    _contacts.value.map { contact ->
                        contact.copy(score = 0, mappedRingtoneName = "System Default", currentRingtone = null)
                    }
                _contacts.value = updatedContacts

                // Background update of preferences and contacts in system DB
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val contactsList = ContactHelper.getContacts(context)
                        ContactHelper.resetAllScores(context, contactsList)
                        val loadedContacts = ContactHelper.getContacts(context)
                        _contacts.value = loadedContacts
                        AppLogger.log(context, "ViewModel", "Background reset all contacts scores completed")
                    } catch (bgEx: Exception) {
                        AppLogger.log(context, "ViewModel", "Background reset all scores failed", bgEx)
                    }
                }
            } catch (e: Exception) {
                AppLogger.log(context, "ViewModel", "resetAllScores() failed", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            AppLogger.log(context, "ViewModel", "clearHistory() started")
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    PreferenceHelper.clearCallLogsHistory(context)
                }
                _callLogs.value = emptyList()
                AppLogger.log(context, "ViewModel", "clearHistory() completed")
            } catch (e: Exception) {
                AppLogger.log(context, "ViewModel", "clearHistory() failed", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setFallbackRingtone(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val persisted =
                    withContext(Dispatchers.IO) {
                        RingtoneHelper.persistRingtone(context, uri)
                    }
                if (persisted != null) {
                    val (name, uriString) = persisted
                    withContext(Dispatchers.IO) {
                        PreferenceHelper.setFallbackRingtone(context, uriString, name)
                    }
                    _fallbackRingtoneUri.value = uriString
                    _fallbackRingtoneName.value = name
                    AppLogger.log(context, "ViewModel", "Fallback default ringtone set to: $name")
                } else {
                    AppLogger.log(context, "ViewModel", "Failed to persist fallback ringtone from URI")
                    _error.value = Exception("Failed to resolve fallback audio file from URI: $uri")
                }
            } catch (e: Exception) {
                AppLogger.log(context, "ViewModel", "setFallbackRingtone failed", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearFallbackRingtone() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    PreferenceHelper.setFallbackRingtone(context, null, null)
                }
                _fallbackRingtoneUri.value = null
                _fallbackRingtoneName.value = null
                AppLogger.log(context, "ViewModel", "Fallback default ringtone cleared")
            } catch (e: Exception) {
                AppLogger.log(context, "ViewModel", "clearFallbackRingtone failed", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setAppPaused(paused: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    PreferenceHelper.setAppPaused(context, paused)
                    if (paused) {
                        ContactHelper.restoreAllRingtonesToDefault(context)
                    } else {
                        val contactsList = ContactHelper.getContacts(context)
                        val ringtonesList = PreferenceHelper.getRingtones(context)
                        ContactHelper.updateContactsRingtonesBasedOnScores(context, contactsList, ringtonesList)
                    }
                }
                _isAppPaused.value = paused
                AppLogger.log(context, "ViewModel", "App pause state changed to: $paused")
                loadData()
            } catch (e: Exception) {
                AppLogger.log(context, "ViewModel", "setAppPaused failed", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
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
            AppLogger.log(context, "ViewModel", "playPreview() started: uri=$uriString")
            try {
                stopPreview()
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
                AppLogger.log(context, "ViewModel", "playPreview() via MediaPlayer failed, trying RingtoneManager fallback", e)
                try {
                    val ringtone = android.media.RingtoneManager.getRingtone(context, Uri.parse(uriString))
                    if (ringtone != null) {
                        ringtone.play()
                        currentRingtone = ringtone
                        _playingUri.value = uriString
                        viewModelScope.launch {
                            while (ringtone.isPlaying && _playingUri.value == uriString) {
                                kotlinx.coroutines.delay(500)
                            }
                            if (_playingUri.value == uriString) {
                                stopPreview()
                            }
                        }
                    } else {
                        throw Exception("RingtoneManager returned null")
                    }
                } catch (fallbackEx: Exception) {
                    AppLogger.log(context, "ViewModel", "playPreview() fallback failed", fallbackEx)
                    _error.value = e // Report the original MediaPlayer error
                    stopPreview()
                }
            }
        }
    }

    fun stopPreview() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            AppLogger.log(context, "ViewModel", "stopPreview() error (ignored)", e)
        } finally {
            mediaPlayer = null
        }
        try {
            currentRingtone?.stop()
        } catch (e: Exception) {
            AppLogger.log(context, "ViewModel", "stopPreview() ringtone error (ignored)", e)
        } finally {
            currentRingtone = null
            _playingUri.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        currentRingtone?.stop()
        AppLogger.log(context, "ViewModel", "onCleared() - MediaPlayer and Ringtone released")
    }
}
