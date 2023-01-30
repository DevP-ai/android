package mega.privacy.android.app.presentation.login

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.logging.LegacyLoggingSettings
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.presentation.login.model.LoginState.Companion.CLICKS_TO_ENABLE_LOGS
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.model.UserCredentials
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import mega.privacy.android.domain.usecase.GetSession
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.RootNodeExists
import mega.privacy.android.domain.usecase.setting.ResetChatSettings
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * View Model for [LoginFragment]
 *
 * @property state View state as [LoginState]
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
    private val monitorConnectivity: MonitorConnectivity,
    private val rootNodeExistsUseCase: RootNodeExists,
    private val getFeatureFlagValue: GetFeatureFlagValue,
    private val loggingSettings: LegacyLoggingSettings,
    private val resetChatSettings: ResetChatSettings,
    private val getSession: GetSession,
    private val dbH: DatabaseHandler,
    @MegaApi private val megaApi: MegaApiAndroid,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    /**
     * Get latest value of StorageState.
     */
    fun getStorageState() = monitorStorageStateEvent.getState()

    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value

    /**
     * Checks if root node exists.
     *
     * @return True if root node exists, false otherwise.
     */
    fun rootNodeExists() = runBlocking { rootNodeExistsUseCase() }

    /**
     * Reset some states values.
     */
    fun setupInitialState() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    is2FAEnabled = false,
                    isAccountConfirmed = false,
                    pressedBackWhileLogin = false,
                    isAlreadyLoggedIn = getSession() != null
                )
            }
        }

        initChatSettings()
    }

    /**
     * Reset chat settings.
     */
    fun initChatSettings() = viewModelScope.launch { resetChatSettings() }

    /**
     * Updates state with a new intentAction.
     *
     * @param intentAction Intent action.
     */
    fun setIntentAction(intentAction: String) {
        _state.update { it.copy(intentAction = intentAction) }
    }

    /**
     * Updates isFirstFetchNodesUpdate as true in state.
     */
    fun updateFetchNodesUpdate() {
        _state.update { it.copy(isFirstFetchNodesUpdate = false) }
    }

    /**
     * Updates pressBackWhileLogin value in state.
     */
    fun updatePressedBackWhileLogin(pressedBackWhileLogin: Boolean) {
        _state.update { it.copy(pressedBackWhileLogin = pressedBackWhileLogin) }
    }

    /**
     * Updates accountConfirmationLink value in state.
     */
    fun updateAccountConfirmationLink(accountConfirmationLink: String?) {
        _state.update { it.copy(accountConfirmationLink = accountConfirmationLink) }
    }

    /**
     * Updates isFetchingNodes value in state.
     */
    fun updateIsFetchingNodes(isFetchingNodes: Boolean) {
        _state.update { it.copy(isFetchingNodes = isFetchingNodes) }
    }

    /**
     * Updates isAlreadyLoggedIn value in state.
     */
    fun updateIsAlreadyLoggedIn(isAlreadyLoggedIn: Boolean) {
        _state.update { it.copy(isAlreadyLoggedIn = isAlreadyLoggedIn) }
    }

    /**
     * Updates was2FAErrorShown and is2FAErrorShown values as true in state.
     */
    fun setWas2FAErrorShown() {
        _state.update { it.copy(was2FAErrorShown = true, is2FAErrorShown = true) }
    }

    /**
     * Updates is2FAErrorShown value as false in state.
     */
    fun setIs2FAErrorNotShown() {
        _state.update { it.copy(is2FAErrorShown = false) }
    }

    /**
     * Updates is2FAEnabled value as true in state.
     */
    fun setIs2FAEnabled() {
        _state.update { it.copy(is2FAEnabled = true) }
    }

    /**
     * Updates isAccountConfirmed value in state.
     */
    fun updateIsAccountConfirmed(isAccountConfirmed: Boolean) {
        _state.update { it.copy(isAccountConfirmed = isAccountConfirmed) }
    }

    /**
     * Updates isPinLongClick value in state.
     */
    fun updateIsPinLongClick(isPinLongClick: Boolean) {
        _state.update { it.copy(isPinLongClick = isPinLongClick) }
    }

    /**
     * Updates isRefreshApiServer value in state.
     */
    fun updateIsRefreshApiServer(isRefreshApiServer: Boolean) {
        _state.update { it.copy(isRefreshApiServer = isRefreshApiServer) }
    }

    /**
     * Updates email and session values in state.
     */
    fun updateEmailAndSession() =
        dbH.credentials?.let { credentials ->
            val userCredentials = state.value.userCredentials
            _state.update {
                it.copy(
                    userCredentials = userCredentials?.copy(
                        email = credentials.email,
                        session = credentials.session
                    )
                        ?: UserCredentials(
                            email = credentials.email,
                            credentials.session,
                            null,
                            null,
                            null
                        )
                )
            }
            true
        } ?: false

    /**
     * Updates email and password values in state.
     */
    fun updateCredentials(email: String?, password: String?) {
        val userCredentials = state.value.userCredentials

        _state.update {
            it.copy(
                userCredentials = userCredentials?.copy(email = email)
                    ?: UserCredentials(email = email, null, null, null, null),
                password = password
            )
        }
    }

    /**
     * Updates temporal email and password values in state.
     */
    fun setTemporalCredentials(email: String?, password: String?) {
        _state.update { it.copy(temporalEmail = email, temporalPassword = password) }
    }

    /**
     * Set temporal email and password values in state as current email and password.
     */
    fun setTemporalCredentialsAsCurrentCredentials() = with(state.value) {
        updateCredentials(temporalEmail, temporalPassword)
    }

    /**
     * True if there is a not null email and a not null password, false otherwise.
     */
    fun areThereValidTemporalCredentials() = with(state.value) {
        temporalEmail != null && temporalPassword != null
    }

    /**
     * Saves credentials
     */
    fun saveCredentials() {
        val session = megaApi.dumpSession()
        var myUserHandle: String? = null
        var email: String? = null

        megaApi.myUser?.let { myUser ->
            email = myUser.email
            myUserHandle = myUser.handle.toString()
        }

        with(dbH) {
            saveCredentials(UserCredentials(email, session, "", "", myUserHandle))
            clearEphemeral()
        }
    }

    /**
     * Decrements the required value for enabling/disabling Karere logs.
     *
     * @param activity Required [Activity]
     */
    fun clickKarereLogs(activity: Activity) = with(state.value) {
        if (pendingClicksKarere == 1) {
            viewModelScope.launch {
                if (!getFeatureFlagValue(AppFeatures.PermanentLogging)) {
                    if (loggingSettings.areKarereLogsEnabled()) {
                        loggingSettings.setStatusLoggerKarere(activity, false)
                    } else {
                        (activity as LoginActivity).showConfirmationEnableLogsKarere()
                    }
                }
            }
            _state.update { it.copy(pendingClicksKarere = CLICKS_TO_ENABLE_LOGS) }
        } else {
            _state.update { it.copy(pendingClicksKarere = pendingClicksKarere - 1) }
        }
    }

    /**
     * Decrements the required value for enabling/disabling SDK logs.
     *
     * @param activity Required [Activity]
     */
    fun clickSDKLogs(activity: Activity) = with(state.value) {
        if (pendingClicksSDK == 1) {
            viewModelScope.launch {
                if (!getFeatureFlagValue(AppFeatures.PermanentLogging)) {
                    if (loggingSettings.areSDKLogsEnabled()) {
                        loggingSettings.setStatusLoggerSDK(activity, false)
                    } else {
                        (activity as LoginActivity).showConfirmationEnableLogsSDK()
                    }
                }
            }
            _state.update { it.copy(pendingClicksSDK = CLICKS_TO_ENABLE_LOGS) }
        } else {
            _state.update { it.copy(pendingClicksSDK = pendingClicksSDK - 1) }
        }
    }
}