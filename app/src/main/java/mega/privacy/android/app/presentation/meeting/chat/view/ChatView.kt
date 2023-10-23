package mega.privacy.android.app.presentation.meeting.chat.view

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.isValid
import mega.privacy.android.app.presentation.extensions.vectorRes
import mega.privacy.android.app.presentation.meeting.chat.model.ChatRoomMenuAction
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.contacts.UserChatStatus

/**
 * Chat view
 *
 * @param uiState [ChatUiState]
 * @param onBackPressed Action to perform for back button.
 * @param onMenuActionPressed Action to perform for menu buttons.
 */
@Composable
internal fun ChatView(
    uiState: ChatUiState,
    onBackPressed: () -> Unit,
    onMenuActionPressed: (ChatRoomMenuAction) -> Unit,
) {
    Scaffold(
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = uiState.title.orEmpty(),
                onNavigationPressed = onBackPressed,
                titleIcons = { TitleIcons(uiState) },
                actions = getChatRoomActions(uiState),
                onActionPressed = { (it as ChatRoomMenuAction).let(onMenuActionPressed) },
                elevation = 0.dp
            )
        }
    )
    { innerPadding ->
        Text(modifier = Modifier.padding(innerPadding), text = "Hello chat fragment")
    }
}

@Composable
private fun TitleIcons(uiState: ChatUiState) {
    UserChatStateIcon(uiState.userChatStatus)
    PrivateChatIcon(uiState.isPrivateChat)
    MuteIcon(uiState.isChatNotificationMute)
}

@Composable
private fun UserChatStateIcon(userChatStatus: UserChatStatus?) {
    if (userChatStatus?.isValid() == true) {
        Image(
            painter = painterResource(id = userChatStatus.vectorRes(MaterialTheme.colors.isLight)),
            modifier = Modifier.testTag(TEST_TAG_USER_CHAT_STATE),
            contentDescription = "Status icon"
        )
    }
}

@Composable
private fun MuteIcon(isNotificationMute: Boolean) {
    if (isNotificationMute) {
        Icon(
            modifier = Modifier.testTag(TEST_TAG_NOTIFICATION_MUTE),
            painter = painterResource(id = R.drawable.ic_bell_off_small),
            contentDescription = "Mute icon"
        )
    }
}

@Composable
private fun PrivateChatIcon(isPrivateChat: Boolean?) {
    if (isPrivateChat == true) {
        Icon(
            modifier = Modifier
                .testTag(TEST_TAG_PRIVATE_ICON)
                .size(16.dp),
            painter = painterResource(id = R.drawable.ic_key_02),
            contentDescription = "private room icon",
        )
    }
}

private fun getChatRoomActions(uiState: ChatUiState): List<ChatRoomMenuAction> {
    val actions = mutableListOf<ChatRoomMenuAction>()

    with(uiState) {
        if ((myPermission == ChatRoomPermission.Moderator || myPermission == ChatRoomPermission.Standard)
            && !isJoiningOrLeaving && !isPreviewMode
        ) {
            actions.add(ChatRoomMenuAction.AudioCall(!hasACallInThisChat))

            if (!isGroup) {
                actions.add(ChatRoomMenuAction.VideoCall(!hasACallInThisChat))
            }
        }
    }

    return actions
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "ChatView")
@Composable
private fun ChatViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val uiState = ChatUiState(
            title = "My Name",
            userChatStatus = UserChatStatus.Away,
            isChatNotificationMute = true,
            isPrivateChat = true,
            myPermission = ChatRoomPermission.Standard,
        )
        ChatView(
            uiState = uiState,
            onBackPressed = {},
            onMenuActionPressed = {}
        )
    }
}

internal const val TEST_TAG_USER_CHAT_STATE = "chat_view:icon_user_chat_status"
internal const val TEST_TAG_NOTIFICATION_MUTE = "chat_view:icon_chat_notification_mute"
internal const val TEST_TAG_PRIVATE_ICON = "chat_view:icon_chat_room_private"