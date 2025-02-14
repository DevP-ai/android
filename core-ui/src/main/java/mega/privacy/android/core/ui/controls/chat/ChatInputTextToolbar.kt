package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Attachment icon test tag.
 */
const val TEST_TAG_ATTACHMENT_ICON = "chat_input_text_toolbar:attachment_icon"

/**
 * Chat input text toolbar
 *
 * @param modifier modifier
 * @param onAttachmentClick click listener for attachment icon
 */
@Composable
fun ChatInputTextToolbar(
    text: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onAttachmentClick: () -> Unit,
) {
    var input by rememberSaveable(text) {
        mutableStateOf(text)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .padding(end = 8.dp)
                .testTag(TEST_TAG_ATTACHMENT_ICON)
                .clickable(onClick = onAttachmentClick),
            painter = painterResource(id = R.drawable.ic_plus),
            contentDescription = "Attachment icon",
            tint = MegaTheme.colors.icon.secondary,
        )

        ChatTextField(
            text = input,
            placeholder = placeholder,
            onTextChange = { input = it },
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatInputTextToolbarPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatInputTextToolbar("", "Message") {}
    }
}