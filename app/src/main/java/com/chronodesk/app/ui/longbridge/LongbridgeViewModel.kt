package com.chronodesk.app.ui.longbridge

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chronodesk.app.data.model.LongbridgeFullSnapshot
import com.chronodesk.app.data.model.LongbridgeSettings
import com.chronodesk.app.data.provider.LongbridgeAuthResult
import com.chronodesk.app.data.repository.LongbridgeOAuthLaunch
import com.chronodesk.app.data.repository.LongbridgeRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 长桥证券 (Longbridge) 看板 ViewModel：
 * 衔接 Settings/Home Card/Dedicated Screen 与 LongbridgeRepository。
 */
class LongbridgeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LongbridgeRepository(application)

    val settings: StateFlow<LongbridgeSettings> = repository.settingsFlow
    val snapshot: StateFlow<LongbridgeFullSnapshot> = repository.snapshotFlow

    fun hasCredentials(): Boolean = repository.hasCredentials()

    fun setMockMode(enabled: Boolean) {
        repository.setMockMode(enabled)
    }

    fun onAppForeground() {
        repository.onAppForeground()
    }

    fun onAppBackground() {
        repository.onAppBackground()
    }

    fun setActiveVisibility(isHomePage: Boolean, isLongbridgePage: Boolean) {
        repository.setActiveVisibility(isHomePage = isHomePage, isLongbridgePage = isLongbridgePage)
    }

    fun updateSettings(transform: (LongbridgeSettings) -> LongbridgeSettings) {
        repository.updateSettings(transform)
    }

    fun togglePrivacyMask() {
        repository.togglePrivacyMask()
    }

    fun cycleDisplayCurrency() {
        repository.cycleDisplayCurrency()
    }

    fun refreshNow() {
        viewModelScope.launch {
            repository.refresh(force = true)
        }
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun resolveConnectUrl(): String = repository.resolveConnectUrl()

    suspend fun authenticateWithOneTimeCode(rawAuthCode: String): LongbridgeAuthResult {
        return repository.authenticateWithOneTimeCode(rawAuthCode)
    }

    suspend fun registerAndBuildOAuthRequest(redirectUri: String): Result<LongbridgeOAuthLaunch> {
        return repository.registerAndBuildOAuthRequest(redirectUri)
    }

    suspend fun completeOAuthAuthorization(
        launch: LongbridgeOAuthLaunch,
        rawCallbackOrCode: String
    ): LongbridgeAuthResult {
        return repository.completeOAuthAuthorization(launch, rawCallbackOrCode)
    }

    suspend fun importCredentialText(rawInput: String): LongbridgeAuthResult {
        return repository.importCredentialText(rawInput)
    }
}
