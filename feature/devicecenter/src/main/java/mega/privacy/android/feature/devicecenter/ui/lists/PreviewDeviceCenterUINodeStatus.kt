package mega.privacy.android.feature.devicecenter.ui.lists

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus

/**
 * A Preview Composable that displays all possible Statuses
 *
 * @param status The [DeviceCenterUINodeStatus] generated by the [DeviceCenterUINodeStatusProvider]
 */
@CombinedThemePreviews
@Composable
private fun PreviewDeviceCenterUINodeStatus(
    @PreviewParameter(DeviceCenterUINodeStatusProvider::class) status: DeviceCenterUINodeStatus,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DeviceCenterListViewItem(
            uiNode = OwnDeviceUINode(
                id = "1234-5678",
                name = "Backup Name",
                icon = DeviceIconType.Android,
                status = status,
                folders = emptyList(),
            ),
            onDeviceClicked = {},
            onMenuClicked = {},
        )
    }
}

/**
 * A [PreviewParameterProvider] class that provides the list of Statuses to be displayed in the
 * Composable preview
 */
private class DeviceCenterUINodeStatusProvider :
    PreviewParameterProvider<DeviceCenterUINodeStatus> {
    override val values = listOf(
        DeviceCenterUINodeStatus.Unknown,
        DeviceCenterUINodeStatus.UpToDate,
        DeviceCenterUINodeStatus.Initializing,
        DeviceCenterUINodeStatus.Scanning,
        DeviceCenterUINodeStatus.Syncing,
        DeviceCenterUINodeStatus.SyncingWithPercentage(50),
        DeviceCenterUINodeStatus.CameraUploadsDisabled,
        DeviceCenterUINodeStatus.Disabled,
        DeviceCenterUINodeStatus.Offline,
        DeviceCenterUINodeStatus.Paused,
        DeviceCenterUINodeStatus.Stopped,
        DeviceCenterUINodeStatus.Overquota,
        DeviceCenterUINodeStatus.Error,
        DeviceCenterUINodeStatus.FolderError(
            errorMessage = R.string.device_center_list_view_item_sub_state_storage_overquota,
        ),
        DeviceCenterUINodeStatus.Blocked,
    ).asSequence()
}