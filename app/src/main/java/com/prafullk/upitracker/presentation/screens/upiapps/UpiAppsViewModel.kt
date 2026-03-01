package com.prafullk.upitracker.presentation.screens.upiapps

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prafullk.upitracker.data.db.dao.UpiAppDao
import com.prafullk.upitracker.data.db.entities.UpiAppEntity
import com.prafullk.upitracker.data.detection.upi.DiscoveredUpiApp
import com.prafullk.upitracker.data.detection.upi.UpiAppDiscoveryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UpiAppUiItem(
        val packageName: String,
        val displayName: String,
        val icon: Drawable?,
        val isActive: Boolean,
        val isRecentlyActive: Boolean // transaction in last 7 days
)

sealed class UpiAppsUiState {
    object Loading : UpiAppsUiState()
    data class Success(val apps: List<UpiAppUiItem>) : UpiAppsUiState()
    data class Error(val message: String) : UpiAppsUiState()
}

class UpiAppsViewModel(
        private val discoveryService: UpiAppDiscoveryService,
        private val upiAppDao: UpiAppDao
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)

    val uiState: StateFlow<UpiAppsUiState> =
            combine(upiAppDao.observeAll(), _isScanning) { apps, isScanning ->
                if (isScanning) return@combine UpiAppsUiState.Loading
                val now = System.currentTimeMillis()
                val sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000L
                val items = apps.map { entity ->
                    UpiAppUiItem(
                            packageName = entity.packageName,
                            displayName = entity.displayName,
                            icon = discoveryService.getCachedIcon(entity.packageName)
                                    ?: discoveryService.getIcon(entity.packageName),
                            isActive = entity.isActive,
                            isRecentlyActive = (entity.lastTransactionAt ?: 0L) >= sevenDaysAgo
                    )
                }
                UpiAppsUiState.Success(items)
            }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = UpiAppsUiState.Loading
            )

    /** Trigger a fresh scan for installed UPI apps. */
    fun scanForUpiApps() {
        viewModelScope.launch {
            _isScanning.value = true
            runCatching { discoveryService.discoverAndSync() }
            _isScanning.value = false
        }
    }

    /** Toggle tracking for a specific app. */
    fun setAppActive(packageName: String, isActive: Boolean) {
        viewModelScope.launch {
            runCatching { discoveryService.setAppActive(packageName, isActive) }
        }
    }
}
