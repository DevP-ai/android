package mega.privacy.android.app.presentation.meeting.chat.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.extensions.isPast
import mega.privacy.android.app.presentation.meeting.chat.mapper.InviteParticipantResultMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.ScanMessageMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.EnableGeolocationUseCase
import mega.privacy.android.domain.usecase.chat.EndCallUseCase
import mega.privacy.android.domain.usecase.chat.GetChatMuteOptionListUseCase
import mega.privacy.android.domain.usecase.chat.GetCustomSubtitleListUseCase
import mega.privacy.android.domain.usecase.chat.InviteToChatUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotificationMuteUseCase
import mega.privacy.android.domain.usecase.chat.IsGeolocationEnabledUseCase
import mega.privacy.android.domain.usecase.chat.MonitorCallInChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatConnectionStateUseCase
import mega.privacy.android.domain.usecase.chat.MonitorParticipatingInACallUseCase
import mega.privacy.android.domain.usecase.chat.MonitorUserChatStatusByHandleUseCase
import mega.privacy.android.domain.usecase.chat.MuteChatNotificationForChatRoomsUseCase
import mega.privacy.android.domain.usecase.chat.UnmuteChatNotificationUseCase
import mega.privacy.android.domain.usecase.chat.message.MonitorMessageLoadedUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetParticipantFirstNameUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.MonitorAllContactParticipantsInChatUseCase
import mega.privacy.android.domain.usecase.contact.MonitorHasAnyContactUseCase
import mega.privacy.android.domain.usecase.contact.MonitorUserLastGreenUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.meeting.IsChatStatusConnectedForCallUseCase
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase.Companion.NUMBER_MESSAGES_TO_LOAD
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.StartCallUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRingingUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import timber.log.Timber
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Chat view model.
 *
 * @property isChatNotificationMuteUseCase
 * @property getChatRoomUseCase
 * @property monitorChatRoomUpdates
 * @property monitorUpdatePushNotificationSettingsUseCase
 * @property monitorUserChatStatusByHandleUseCase
 * @property state UI state.
 *
 * @param savedStateHandle
 */
@HiltViewModel
internal class ChatViewModel @Inject constructor(
    private val isChatNotificationMuteUseCase: IsChatNotificationMuteUseCase,
    private val getChatRoomUseCase: GetChatRoom,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
    private val monitorUserChatStatusByHandleUseCase: MonitorUserChatStatusByHandleUseCase,
    private val monitorParticipatingInACallUseCase: MonitorParticipatingInACallUseCase,
    private val monitorCallInChatUseCase: MonitorCallInChatUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val monitorChatConnectionStateUseCase: MonitorChatConnectionStateUseCase,
    private val isChatStatusConnectedForCallUseCase: IsChatStatusConnectedForCallUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val requestUserLastGreenUseCase: RequestUserLastGreenUseCase,
    private val monitorUserLastGreenUpdatesUseCase: MonitorUserLastGreenUpdatesUseCase,
    private val getParticipantFirstNameUseCase: GetParticipantFirstNameUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
    private val getScheduledMeetingByChatUseCase: GetScheduledMeetingByChat,
    private val monitorHasAnyContactUseCase: MonitorHasAnyContactUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val getCustomSubtitleListUseCase: GetCustomSubtitleListUseCase,
    private val monitorAllContactParticipantsInChatUseCase: MonitorAllContactParticipantsInChatUseCase,
    private val inviteToChatUseCase: InviteToChatUseCase,
    private val inviteParticipantResultMapper: InviteParticipantResultMapper,
    private val unmuteChatNotificationUseCase: UnmuteChatNotificationUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val archiveChatUseCase: ArchiveChatUseCase,
    private val endCallUseCase: EndCallUseCase,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    private val startCallUseCase: StartCallUseCase,
    private val chatManagement: ChatManagement,
    private val loadMessagesUseCase: LoadMessagesUseCase,
    private val monitorMessageLoadedUseCase: MonitorMessageLoadedUseCase,
    private val getChatMuteOptionListUseCase: GetChatMuteOptionListUseCase,
    private val muteChatNotificationForChatRoomsUseCase: MuteChatNotificationForChatRoomsUseCase,
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase,
    private val scanMessageMapper: ScanMessageMapper,
    private val isGeolocationEnabledUseCase: IsGeolocationEnabledUseCase,
    private val enableGeolocationUseCase: EnableGeolocationUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private val chatId = savedStateHandle.get<Long>(Constants.CHAT_ID)
    private val usersTyping = Collections.synchronizedMap(mutableMapOf<Long, String?>())
    private val jobs = mutableMapOf<Long, Job>()

    private val ChatRoom.isPrivateRoom: Boolean
        get() = !isGroup || !isPublic

    private val ChatRoom.haveAtLeastReadPermission: Boolean
        get() = ownPrivilege != ChatRoomPermission.Unknown
                && ownPrivilege != ChatRoomPermission.Removed

    private var monitorAllContactParticipantsInChatJob: Job? = null

    init {
        checkGeolocation()
        monitorParticipatingInACall()
        monitorStorageStateEvent()
        chatId?.let {
            updateChatId(it)
            getChatRoom(it)
            getNotificationMute(it)
            getChatConnectionState(it)
            getScheduledMeeting(it)
            monitorACallInThisChat(it)
            monitorChatRoom(it)
            monitorNotificationMute(it)
            monitorChatConnectionState(it)
            monitorNetworkConnectivity(it)
            monitorMessageLoaded(it)
        }
    }

    private fun checkIfMoreMessagesShouldBeRequested() = with(state.value) {
        when {
            chatHistoryLoadStatus == ChatHistoryLoadStatus.NONE -> {
                Timber.d("Whole history already loaded. No more messages to request.")
                false
            }

            pendingMessagesToLoad > 0 && chatHistoryLoadStatus == ChatHistoryLoadStatus.REMOTE -> {
                Timber.d("Waiting for messages to load. No more can be requested. Previous history status $chatHistoryLoadStatus")
                false
            }

            else -> {
                Timber.d("Requesting more messages. Previous history status $chatHistoryLoadStatus")
                _state.update { state -> state.copy(pendingMessagesToLoad = NUMBER_MESSAGES_TO_LOAD) }
                true
            }
        }
    }

    /**
     * Request messages.
     * Must be called only if the first messages in the history are shown in the screen.
     */
    fun requestMessages() {
        if (checkIfMoreMessagesShouldBeRequested() && chatId != null) {
            viewModelScope.launch {
                val newHistoryLoadStatus = loadMessagesUseCase(chatId)
                Timber.d("New history status $newHistoryLoadStatus")
                _state.update { state -> state.copy(chatHistoryLoadStatus = newHistoryLoadStatus) }
            }
        }
    }

    private fun monitorMessageLoaded(chatId: Long) {
        viewModelScope.launch {
            monitorMessageLoadedUseCase(chatId).collect { loadedMessage ->
                Timber.d("New message: ${loadedMessage.msgId}")
                val state = state.value
                val newMessages = scanMessageMapper(
                    isOneToOne = !state.isGroup && !state.isMeeting,
                    currentItems = state.messages,
                    newMessage = loadedMessage
                )
                val pendingMessagesToLoad = state.pendingMessagesToLoad - 1
                _state.update { state ->
                    state.copy(
                        messages = newMessages,
                        pendingMessagesToLoad = pendingMessagesToLoad
                    )
                }
            }
        }
    }

    private fun monitorAllContactParticipantsInChat(peerHandles: List<Long>) {
        monitorAllContactParticipantsInChatJob?.cancel()
        monitorAllContactParticipantsInChatJob = viewModelScope.launch {
            monitorAllContactParticipantsInChatUseCase(peerHandles)
                .catch { Timber.e(it) }
                .collect { allContactsParticipateInChat ->
                    _state.update { state -> state.copy(allContactsParticipateInChat = allContactsParticipateInChat) }
                }
        }
    }

    private fun getScheduledMeeting(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                getScheduledMeetingByChatUseCase(chatId)
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList?.firstOrNull { it.parentSchedId == INVALID_HANDLE }
                    ?.let { meeting ->
                        _state.update {
                            it.copy(
                                schedIsPending = !meeting.isPast(),
                                scheduledMeeting = meeting
                            )
                        }
                    }
            }.onFailure {
                Timber.e(it)
                _state.update { state -> state.copy(scheduledMeeting = null) }
            }
        }
    }

    private fun monitorNetworkConnectivity(chatId: Long) {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .collect { networkConnected ->
                    val isChatConnected = if (networkConnected) {
                        isChatStatusConnectedForCallUseCase(chatId = chatId)
                    } else {
                        false
                    }

                    _state.update {
                        it.copy(isConnected = isChatConnected)
                    }
                }
        }
    }

    private fun updateChatId(chatId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(chatId = chatId) }
        }
    }

    private fun monitorChatConnectionState(chatId: Long) {
        viewModelScope.launch {
            monitorChatConnectionStateUseCase()
                .filter { it.chatId == chatId }
                .collect { state ->
                    if (state.chatConnectionStatus != ChatConnectionStatus.Online) {
                        _state.update {
                            it.copy(isConnected = false)
                        }
                    } else {
                        getChatConnectionState(chatId = chatId)
                    }
                }
        }
    }

    private fun getChatConnectionState(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                isChatStatusConnectedForCallUseCase(chatId = chatId)
            }.onSuccess { connected ->
                _state.update { state ->
                    state.copy(isConnected = connected)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorStorageStateEvent() {
        viewModelScope.launch {
            monitorStorageStateEventUseCase()
                .collect { storageState ->
                    _state.update { state -> state.copy(storageState = storageState.storageState) }
                }
        }
    }

    private fun getChatRoom(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                getChatRoomUseCase(chatId)
            }.onSuccess { chatRoom ->
                requestMessages()
                chatRoom?.let {
                    with(chatRoom) {
                        checkCustomTitle()
                        _state.update { state ->
                            state.copy(
                                title = title,
                                isPrivateChat = chatRoom.isPrivateRoom,
                                myPermission = ownPrivilege,
                                isPreviewMode = isPreview,
                                isGroup = isGroup,
                                isOpenInvite = isOpenInvite,
                                isActive = isActive,
                                isArchived = isArchived,
                                isMeeting = isMeeting,
                                participantsCount = getNumberParticipants(),
                                isWaitingRoom = isWaitingRoom
                            )
                        }
                        if (peerHandlesList.isNotEmpty()) {
                            if (!isGroup) {
                                peerHandlesList[0].let {
                                    getUserChatStatus(it)
                                    monitorUserOnlineStatusUpdates(it)
                                    monitorUserLastGreen(it)
                                }
                            } else {
                                monitorAllContactParticipantsInChat(peerHandlesList)
                                monitorHasAnyContact()
                            }
                        }
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun ChatRoom.checkCustomTitle() {
        if (isGroup && hasCustomTitle) {
            viewModelScope.launch {
                runCatching { getCustomSubtitleListUseCase(chatId, peerHandlesList, isPreview) }
                    .onSuccess { customSubtitleList ->
                        _state.update { state -> state.copy(customSubtitleList = customSubtitleList) }
                    }
                    .onFailure { Timber.w(it) }
            }
        }
    }

    private fun ChatRoom.getNumberParticipants() =
        (peerCount + if (haveAtLeastReadPermission) 1 else 0)
            .takeIf { isGroup }

    private fun getUserChatStatus(userHandle: Long) {
        viewModelScope.launch {
            runCatching {
                getUserOnlineStatusByHandleUseCase(userHandle)
            }.onSuccess { userChatStatus ->
                updateUserChatStatus(userHandle, userChatStatus)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun getNotificationMute(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                isChatNotificationMuteUseCase(chatId)
            }.onSuccess { isMute ->
                _state.update { it.copy(isChatNotificationMute = isMute) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorChatRoom(chatId: Long) {
        viewModelScope.launch {
            monitorChatRoomUpdates(chatId)
                .collect { chat ->
                    with(chat) {
                        changes?.forEach { change ->
                            when (change) {
                                ChatRoomChange.Title -> {
                                    checkCustomTitle()
                                    _state.update { state -> state.copy(title = title) }
                                }

                                ChatRoomChange.ChatMode -> _state.update { state ->
                                    state.copy(isPrivateChat = !isPublic)
                                }

                                ChatRoomChange.OwnPrivilege -> _state.update { state ->
                                    state.copy(
                                        myPermission = ownPrivilege,
                                        isActive = isActive,
                                        participantsCount = getNumberParticipants()
                                    )
                                }

                                ChatRoomChange.OpenInvite -> _state.update { state ->
                                    state.copy(isOpenInvite = isOpenInvite)
                                }

                                ChatRoomChange.Closed -> _state.update { state ->
                                    state.copy(
                                        myPermission = ownPrivilege,
                                        isActive = isActive,
                                        participantsCount = getNumberParticipants()
                                    )
                                }

                                ChatRoomChange.Archive -> _state.update { state ->
                                    state.copy(isArchived = isArchived)
                                }

                                ChatRoomChange.UserTyping -> {
                                    if (userTyping != getMyUserHandleUseCase()) {
                                        handleUserTyping(userTyping)
                                    }
                                }

                                ChatRoomChange.UserStopTyping -> handleUserStopTyping(userTyping)

                                ChatRoomChange.Participants -> {
                                    checkCustomTitle()
                                    _state.update { state ->
                                        state.copy(participantsCount = getNumberParticipants())
                                    }
                                    monitorAllContactParticipantsInChat(peerHandlesList)
                                }

                                ChatRoomChange.WaitingRoom -> {
                                    _state.update { state ->
                                        state.copy(isWaitingRoom = isWaitingRoom)
                                    }
                                }

                                else -> {}
                            }
                        }
                    }
                }
        }
    }

    private fun handleUserStopTyping(userTypingHandle: Long) {
        jobs[userTypingHandle]?.cancel()
        usersTyping.remove(userTypingHandle)
        _state.update { state ->
            state.copy(usersTyping = usersTyping.values.toList())
        }
    }

    private fun handleUserTyping(userTypingHandle: Long) {
        // if user is in the map, we don't need to add again
        if (!usersTyping.contains(userTypingHandle)) {
            viewModelScope.launch {
                val firstName = getParticipantFirstNameUseCase(userTypingHandle)
                usersTyping[userTypingHandle] = firstName
                _state.update { state ->
                    state.copy(usersTyping = usersTyping.values.toList())
                }
            }
        }
        // if user continue typing, cancel timer and start new timer
        jobs[userTypingHandle]?.cancel()
        jobs[userTypingHandle] = viewModelScope.launch {
            delay(TimeUnit.SECONDS.toMillis(5))
            usersTyping.remove(userTypingHandle)
            _state.update { state ->
                state.copy(usersTyping = usersTyping.values.toList())
            }
        }
    }

    private fun monitorNotificationMute(chatId: Long) {
        viewModelScope.launch {
            monitorUpdatePushNotificationSettingsUseCase().collect { changed ->
                if (changed) {
                    getNotificationMute(chatId)
                }
            }
        }
    }

    private fun monitorUserOnlineStatusUpdates(userHandle: Long) {
        viewModelScope.launch {
            monitorUserChatStatusByHandleUseCase(userHandle).conflate()
                .collect { userChatStatus ->
                    updateUserChatStatus(userHandle, userChatStatus)
                }
        }
    }

    private fun updateUserChatStatus(userHandle: Long, userChatStatus: UserChatStatus) {
        viewModelScope.launch {
            if (userChatStatus != UserChatStatus.Online) {
                _state.update { state -> state.copy(userChatStatus = userChatStatus) }
                runCatching { requestUserLastGreenUseCase(userHandle) }
                    .onFailure { Timber.e(it) }
            } else {
                _state.update { state ->
                    state.copy(
                        userChatStatus = userChatStatus,
                        userLastGreen = null
                    )
                }
            }
        }
    }

    private fun monitorParticipatingInACall() {
        viewModelScope.launch {
            monitorParticipatingInACallUseCase()
                .catch { Timber.e(it) }
                .collect {
                    Timber.d("Monitor call in progress returned chat id: $it")
                    _state.update { state -> state.copy(currentCall = it) }
                }
        }
    }

    private fun monitorACallInThisChat(chatId: Long) {
        viewModelScope.launch {
            monitorCallInChatUseCase(chatId)
                .catch { Timber.e(it) }
                .collect {
                    _state.update { state -> state.copy(callInThisChat = it) }
                }
        }
    }

    private fun monitorUserLastGreen(userHandle: Long) {
        viewModelScope.launch {
            monitorUserLastGreenUpdatesUseCase(userHandle).conflate()
                .collect { userLastGreen ->
                    if (state.value.userChatStatus != UserChatStatus.Online) {
                        _state.update { state -> state.copy(userLastGreen = userLastGreen) }
                    }
                }
        }
    }

    private fun monitorHasAnyContact() {
        viewModelScope.launch {
            monitorHasAnyContactUseCase().conflate()
                .collect { hasAnyContact ->
                    _state.update { state -> state.copy(hasAnyContact = hasAnyContact) }
                }
        }
    }

    /**
     * Get another call participating
     *
     */
    fun enablePasscodeCheck() {
        passcodeManagement.showPasscodeScreen = true
    }


    /**
     * Handle action press
     *
     * @param action [ChatRoomMenuAction].
     */
    fun handleActionPress(action: ChatRoomMenuAction) {
        when (action) {
            is ChatRoomMenuAction.Unmute -> unmutePushNotification()
            else -> {}
        }
    }

    private fun unmutePushNotification() {
        chatId?.let { chatId ->
            viewModelScope.launch {
                runCatching {
                    unmuteChatNotificationUseCase(chatId)
                    _state.update {
                        it.copy(
                            infoToShowEvent = triggered(
                                InfoToShow(
                                    chatPushNotificationMuteOption = ChatPushNotificationMuteOption.Unmute
                                )
                            )
                        )
                    }
                }.onFailure { Timber.e(it) }
            }
        }
    }

    /**
     * Show the dialog of selecting chat mute options
     */
    fun showMutePushNotificationDialog() {
        chatId?.let {
            viewModelScope.launch {
                val muteOptionList = getChatMuteOptionListUseCase(listOf(chatId))
                _state.update {
                    it.copy(mutePushNotificationDialogEvent = triggered(muteOptionList))
                }
            }
        }
    }

    fun onShowMutePushNotificationDialogConsumed() {
        _state.update { state -> state.copy(mutePushNotificationDialogEvent = consumed()) }
    }

    /**
     * Mute chat push notification based on user selection. And once mute operation succeeds,
     * send [InfoToShow] to show a message to indicate the result in UI.
     *
     * @param option [ChatPushNotificationMuteOption]
     */
    fun mutePushNotification(option: ChatPushNotificationMuteOption) {
        chatId?.let { chatId ->
            viewModelScope.launch {
                runCatching {
                    muteChatNotificationForChatRoomsUseCase(listOf(chatId), option)
                    _state.update {
                        it.copy(
                            infoToShowEvent = triggered(
                                InfoToShow(
                                    chatPushNotificationMuteOption = option
                                )
                            )
                        )
                    }
                }.onFailure { Timber.e(it) }
            }
        }
    }

    fun inviteContactsToChat(chatId: Long, contactsData: List<String>) =
        viewModelScope.launch {
            runCatching {
                inviteToChatUseCase(chatId, contactsData)
            }.onSuccess { result ->
                _state.update { state ->
                    state.copy(
                        infoToShowEvent = triggered(
                            InfoToShow(
                                inviteContactToChatResult = inviteParticipantResultMapper(result)
                            )
                        )
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatId?.let {
                runCatching { clearChatHistoryUseCase(it) }
                    .onSuccess {
                        _state.update { state ->
                            state.copy(
                                infoToShowEvent = triggered(InfoToShow(stringId = R.string.clear_history_success))
                            )
                        }
                    }
                    .onFailure {
                        Timber.e("Error clearing chat history $it")
                        _state.update { state ->
                            state.copy(
                                infoToShowEvent = triggered(InfoToShow(stringId = R.string.clear_history_error))
                            )
                        }
                    }
            }
        }
    }

    fun onInfoToShowEventConsumed() {
        _state.update { state -> state.copy(infoToShowEvent = consumed()) }
    }

    fun endCall() {
        chatId?.let {
            viewModelScope.launch {
                runCatching {
                    endCallUseCase(chatId)
                    sendStatisticsMeetingsUseCase(EndCallForAll())
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }

    fun archiveChat() {
        viewModelScope.launch {
            chatId?.let {
                runCatching { archiveChatUseCase(it, true) }
                    .onFailure {
                        Timber.e("Error archiving chat $it")
                        _state.update { state ->
                            state.copy(
                                infoToShowEvent = triggered(
                                    InfoToShow(
                                        stringId = R.string.error_archive_chat,
                                        args = state.title?.let { title -> listOf(title) }.orEmpty()
                                    )
                                )
                            )
                        }
                    }
            }
        }
    }

    fun unarchiveChat() {
        viewModelScope.launch {
            chatId?.let {
                runCatching { archiveChatUseCase(it, false) }
                    .onFailure {
                        Timber.e("Error unarchiving chat $it")
                        _state.update { state ->
                            state.copy(
                                infoToShowEvent = triggered(
                                    InfoToShow(
                                        stringId = R.string.error_unarchive_chat,
                                        args = state.title?.let { title -> listOf(title) }.orEmpty()
                                    )
                                )
                            )
                        }
                    }
            }
        }
    }

    fun startCall(video: Boolean) {
        viewModelScope.launch {
            chatId?.let {
                runCatching { startCallUseCase(it, video) }
                    .onSuccess { call ->
                        setCallReady(call)
                    }.onFailure { Timber.e("Exception starting call $it") }
            }
        }
    }

    private fun setCallReady(call: ChatCall?) {
        call?.let {
            chatManagement.setSpeakerStatus(call.chatId, call.hasLocalVideo)
            chatManagement.setRequestSentCall(call.callId, call.isOutgoing)
            passcodeManagement.showPasscodeScreen = true
            _state.update { state ->
                state.copy(callInThisChat = call, isStartingCall = true)
            }
        }
    }

    /**
     * On call started.
     */
    fun onCallStarted() {
        _state.update { state -> state.copy(isStartingCall = false) }
    }

    /**
     * On opened waiting room
     */
    fun onWaitingRoomOpened() {
        _state.update { state -> state.copy(openWaitingRoomScreen = false) }
    }

    fun onStartOrJoinMeeting(isStarted: Boolean) {
        val isWaitingRoom = state.value.isWaitingRoom
        if (isStarted) {
            val isHost = state.value.myPermission == ChatRoomPermission.Moderator
            if (isWaitingRoom && !isHost) {
                _state.update { state -> state.copy(openWaitingRoomScreen = true) }
            } else {
                onAnswerCall()
            }
        } else {
            if (isWaitingRoom) {
                startWaitingRoomMeeting()
            } else {
                startMeeting()
            }
        }
    }

    private fun startWaitingRoomMeeting() {
        val isHost = state.value.myPermission == ChatRoomPermission.Moderator
        if (isHost) {
            viewModelScope.launch {
                runCatching {
                    val chatId = requireNotNull(chatId)
                    val schedId = state.value.scheduledMeeting?.schedId ?: -1L
                    startMeetingInWaitingRoomChatUseCase(
                        chatId = chatId,
                        schedIdWr = schedId,
                        enabledVideo = false,
                        enabledAudio = true,
                    )
                }.onSuccess { chatCall ->
                    setCallReady(chatCall)
                }.onFailure {
                    Timber.e(it)
                }
            }
        } else {
            _state.update { state -> state.copy(openWaitingRoomScreen = true) }
        }
    }

    private fun startMeeting() {
        viewModelScope.launch {
            runCatching {
                val chatId = requireNotNull(chatId)
                val scheduledMeeting = requireNotNull(state.value.scheduledMeeting)
                startChatCallNoRingingUseCase(
                    chatId = chatId,
                    schedId = scheduledMeeting.schedId,
                    enabledVideo = false,
                    enabledAudio = true
                )
            }.onSuccess { chatCall ->
                setCallReady(chatCall)
            }.onFailure { Timber.e(it) }
        }
    }

    fun onAnswerCall() {
        viewModelScope.launch {
            chatId?.let {
                chatManagement.addJoiningCallChatId(chatId)
                runCatching {
                    answerChatCallUseCase(chatId = chatId, video = false, audio = true)
                }.onSuccess { call ->
                    call?.apply {
                        chatManagement.removeJoiningCallChatId(chatId)
                        rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                        _state.update { state -> state.copy(isStartingCall = true) }
                    }
                }.onFailure {
                    Timber.w("Exception answering call: $it")
                    chatManagement.removeJoiningCallChatId(chatId)
                }
            }
        }
    }

    private fun checkGeolocation() {
        viewModelScope.launch {
            runCatching { isGeolocationEnabledUseCase() }
                .onSuccess { isGeolocationEnabled ->
                    _state.update { state -> state.copy(isGeolocationEnabled = isGeolocationEnabled) }
                }.onFailure {
                    Timber.e(it)
                }
        }
    }

    fun onEnableGeolocation() {
        viewModelScope.launch {
            runCatching { enableGeolocationUseCase() }
                .onSuccess {
                    _state.update { state -> state.copy(isGeolocationEnabled = true) }
                }.onFailure {
                    Timber.e(it)
                }
        }
    }

    companion object {
        private const val INVALID_HANDLE = -1L
    }
}
