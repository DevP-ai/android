package mega.privacy.android.app.lollipop.managerSections.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.*
import mega.privacy.android.app.utils.LogUtil
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getAccountDetails: GetAccountDetails,
    private val canDeleteAccount: CanDeleteAccount,
    private val refreshPasscodeLockPreference: RefreshPasscodeLockPreference,
    private val isLoggingEnabled: IsLoggingEnabled,
    private val isChatLoggingEnabled: IsChatLoggingEnabled,
    private val isCameraSyncEnabled: IsCameraSyncEnabled,
    private val rootNodeExists: RootNodeExists,
    private val isMultiFactorAuthAvailable: IsMultiFactorAuthAvailable,
    private val fetchAutoAcceptQRLinks: FetchAutoAcceptQRLinks,
    private val startScreen: GetStartScreen,
    private val shouldHideRecentActivity: ShouldHideRecentActivity,
    private val toggleAutoAcceptQRLinks: ToggleAutoAcceptQRLinks,
    fetchMultiFactorAuthSetting: FetchMultiFactorAuthSetting,
    private val isOnline: IsOnline,
    private val requestAccountDeletion: RequestAccountDeletion
) : ViewModel() {
    private val userAccount = MutableStateFlow(getAccountDetails(false))
    private val state = MutableStateFlow(initialiseState())
    val uiState: StateFlow<SettingsState> = state

    private fun initialiseState(): SettingsState {
        return SettingsState(
            autoAcceptEnabled = false,
            autoAcceptChecked = false,
            multiFactorAuthChecked = false,
            multiFactorEnabled = false,
            multiFactorVisible = false,
            deleteAccountVisible = false,
            deleteEnabled = false,
            cameraUploadEnabled = false,
            chatEnabled = false,
            startScreen = 0,
            hideRecentActivity = false,
        )
    }

    val passcodeLock: Boolean
        get() = refreshPasscodeLockPreference()
    val email: String
        get() = userAccount.value.email
    val isLoggerEnabled: Boolean
        get() = isLoggingEnabled()
    val isChatLoggerEnabled: Boolean
        get() = isChatLoggingEnabled()
    val isCamSyncEnabled: Boolean
        get() = isCameraSyncEnabled()
    val accountType: Int
        get() = userAccount.value.accountTypeIdentifier


    init {
        viewModelScope.launch {
            merge(
                userAccount.map {
                    { state: SettingsState -> state.copy(deleteAccountVisible = canDeleteAccount(it)) }
                },
                flowOf(isMultiFactorAuthAvailable())
                    .map { available ->
                        { state: SettingsState -> state.copy(multiFactorVisible = available) }
                    },
                flowOf(fetchAutoAcceptQRLinks())
                    .map { enabled ->
                        { state: SettingsState -> state.copy(autoAcceptChecked = enabled) }
                    },
                fetchMultiFactorAuthSetting()
                    .map { enabled ->
                        { state: SettingsState -> state.copy(multiFactorAuthChecked = enabled) }
                    },
                isOnline()
                    .map { it && rootNodeExists() }
                    .map { online ->
                        { state: SettingsState ->
                            state.copy(
                                cameraUploadEnabled = online,
                                chatEnabled = online,
                                autoAcceptEnabled = online,
                                multiFactorEnabled = online,
                                deleteEnabled = online,
                            )
                        }
                    },
                startScreen()
                    .map{ screen ->
                        { state: SettingsState -> state.copy(startScreen = screen)}
                    },
                shouldHideRecentActivity()
                    .map{ hide ->
                        { state: SettingsState -> state.copy(hideRecentActivity = hide)}
                    },
            ).collect {
                state.update(it)
            }

        }

    }

    fun refreshAccount() {
        viewModelScope.launch {
            userAccount.value = getAccountDetails(true)
        }
    }

    fun toggleAutoAcceptPreference() {
        viewModelScope.launch {
            kotlin.runCatching {
                toggleAutoAcceptQRLinks()
            }.onSuccess { autoAccept ->
                state.update { it.copy(autoAcceptChecked = autoAccept) }
            }
        }
    }

    suspend fun deleteAccount(): Boolean {
        return kotlin.runCatching { requestAccountDeletion() }
            .fold(
                { return true },
                { e ->
                    LogUtil.logError( "Error when asking for the cancellation link: ${e.message}")
                    return false
                }
            )
    }

}