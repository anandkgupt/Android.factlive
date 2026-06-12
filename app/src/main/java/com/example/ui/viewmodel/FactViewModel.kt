package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.FactCheckResult
import com.example.data.db.SavedFactCheck
import com.example.data.repository.FactCheckRepository
import com.example.data.repository.LiveFactFeedItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface LiveCheckState {
    object Idle : LiveCheckState
    object Loading : LiveCheckState
    data class Success(val result: FactCheckResult) : LiveCheckState
    data class Error(val message: String) : LiveCheckState
}

class FactViewModel(val repository: FactCheckRepository) : ViewModel() {

    // Tab state (0: Pulse Feed, 1: AI Verifier, 2: Saved Bookmarks)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Search query for dashboard filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Current live claim string entered by user
    private val _liveClaimInput = MutableStateFlow("")
    val liveClaimInput: StateFlow<String> = _liveClaimInput.asStateFlow()

    // Status of the active AI fact checking request
    private val _liveCheckState = MutableStateFlow<LiveCheckState>(LiveCheckState.Idle)
    val liveCheckState: StateFlow<LiveCheckState> = _liveCheckState.asStateFlow()

    // Track which preset item ID is currently expanded in the feed
    private val _expandedFeedId = MutableStateFlow<String?>(null)
    val expandedFeedId: StateFlow<String?> = _expandedFeedId.asStateFlow()

    // Reactive saved items (bookmarks) from Room database
    val savedFactChecks: StateFlow<List<SavedFactCheck>> = repository.allItemsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _presets = MutableStateFlow(repository.getPresetDebunks())

    // Filtered presets based on search query
    val filteredPresets: StateFlow<List<LiveFactFeedItem>> = combine(_presets, _searchQuery) { presets, query ->
        if (query.isBlank()) {
            presets
        } else {
            presets.filter {
                it.claim.contains(query, ignoreCase = true) || 
                it.summary.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks if current live checked result has been saved to DB
    private val _isCurrentClaimSaved = MutableStateFlow(false)
    val isCurrentClaimSaved: StateFlow<Boolean> = _isCurrentClaimSaved.asStateFlow()

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateLiveClaimInput(text: String) {
        _liveClaimInput.value = text
        // Reset states if text is cleared
        if (text.isBlank()) {
            _liveCheckState.value = LiveCheckState.Idle
            _isCurrentClaimSaved.value = false
        }
    }

    fun toggleFeedExpansion(feedId: String) {
        _expandedFeedId.update { current ->
            if (current == feedId) null else feedId
        }
    }

    fun clearLiveVerification() {
        _liveCheckState.value = LiveCheckState.Idle
        _liveClaimInput.value = ""
        _isCurrentClaimSaved.value = false
    }

    /**
     * Submits active live claim text to the Gemini fact checking engine
     */
    fun verifyClaimLive() {
        val claim = _liveClaimInput.value.trim()
        if (claim.isBlank()) {
            _liveCheckState.value = LiveCheckState.Error("Please enter a claim or rumor to check.")
            return
        }

        viewModelScope.launch {
            _liveCheckState.value = LiveCheckState.Loading
            try {
                val response = repository.checkClaimLive(claim)
                _liveCheckState.value = LiveCheckState.Success(response)
                
                // Track if saved in Room
                _isCurrentClaimSaved.value = repository.isClaimSaved(claim)
            } catch (e: Exception) {
                _liveCheckState.value = LiveCheckState.Error(e.message ?: "An unexpected error occurred during verification.")
            }
        }
    }

    /**
     * Bookmarks or un-bookmarks the current live verdict
     */
    fun toggleBookmarkLive() {
        val state = _liveCheckState.value
        if (state !is LiveCheckState.Success) return

        val claimText = _liveClaimInput.value.trim()
        val result = state.result

        viewModelScope.launch {
            val isCurrentlySaved = repository.isClaimSaved(claimText)
            if (isCurrentlySaved) {
                repository.removeCheckByClaim(claimText)
                _isCurrentClaimSaved.value = false
            } else {
                val bookmark = SavedFactCheck(
                    claim = claimText,
                    verdict = result.verdict,
                    confidence = result.confidence,
                    analysis = result.analysis,
                    context = result.context,
                    references = result.references
                )
                repository.saveCheck(bookmark)
                _isCurrentClaimSaved.value = true
            }
        }
    }

    /**
     * Bookmarks or un-bookmarks one of the preloaded Fact Live feed items
     */
    fun toggleBookmarkPreset(item: LiveFactFeedItem) {
        viewModelScope.launch {
            val isCurrentlySaved = repository.isClaimSaved(item.claim)
            if (isCurrentlySaved) {
                repository.removeCheckByClaim(item.claim)
            } else {
                val dbItem = SavedFactCheck(
                    claim = item.claim,
                    verdict = item.verdict,
                    confidence = 90, // Static baseline
                    analysis = item.details,
                    context = item.context,
                    references = item.references
                )
                repository.saveCheck(dbItem)
            }
        }
    }

    /**
     * Direct delete by room database item ID
     */
    fun deleteSavedCheckById(id: Int) {
        viewModelScope.launch {
            repository.removeCheckById(id)
            // Re-check active live claim saved state in case this item was the active one
            val state = _liveCheckState.value
            if (state is LiveCheckState.Success) {
                _isCurrentClaimSaved.value = repository.isClaimSaved(_liveClaimInput.value.trim())
            }
        }
    }
}

// Simple Custom flow getter extension mapping to the real flow
private val FactCheckRepository.allItemsFlow: Flow<List<SavedFactCheck>>
    get() = this.allSavedChecks

/**
 * Custom Factory pattern for creating VieModel with arguments without Dagger/Hilt complexity
 */
class FactViewModelFactory(private val repository: FactCheckRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FactViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FactViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
