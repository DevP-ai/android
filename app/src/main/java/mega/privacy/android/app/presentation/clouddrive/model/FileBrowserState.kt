package mega.privacy.android.app.presentation.clouddrive.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.settings.model.MediaDiscoveryViewSettings
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * File browser UI state
 *
 * @property currentViewType serves as the original view type
 * @property fileBrowserHandle current file browser handle
 * @property mediaDiscoveryViewSettings current settings for displaying discovery view
 * @property parentHandle Parent Handle of current Node
 * @property mediaHandle MediaHandle of current Node
 * @property isPendingRefresh
 * @property nodesList list of [NodeUIItem]
 * @property isInSelection if list is in selection mode or not
 * @property itemIndex index of item clicked
 * @property currentFileNode [FileNode]
 * @property selectedNodeHandles List of selected node handles
 * @property selectedFileNodes number of selected file [NodeUIItem] on Compose
 * @property selectedFolderNodes number of selected folder [NodeUIItem] on Compose
 * @property sortOrder [SortOrder] of current list
 * @property optionsItemInfo information when option selected clicked
 * @property isFileBrowserEmpty information about file browser empty
 * @property showMediaDiscovery shows Media discovery of Folder Node
 * @property shouldShowBannerVisibility
 * @property bannerTime timer
 * @property showMediaDiscoveryIcon showMediaDiscoveryIcon
 * @property downloadEvent download event
 */
data class FileBrowserState(
    val currentViewType: ViewType = ViewType.LIST,
    val fileBrowserHandle: Long = -1L,
    val mediaDiscoveryViewSettings: Int = MediaDiscoveryViewSettings.INITIAL.ordinal,
    val parentHandle: Long? = null,
    val mediaHandle: Long = -1L,
    val isPendingRefresh: Boolean = false,
    val nodesList: List<NodeUIItem<TypedNode>> = emptyList(),
    val isInSelection: Boolean = false,
    val itemIndex: Int = -1,
    val currentFileNode: FileNode? = null,
    val selectedNodeHandles: List<Long> = emptyList(),
    val selectedFileNodes: Int = 0,
    val selectedFolderNodes: Int = 0,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val optionsItemInfo: OptionsItemInfo? = null,
    val isFileBrowserEmpty: Boolean = false,
    val showMediaDiscovery: Boolean = false,
    val shouldShowBannerVisibility: Boolean = false,
    val bannerTime: Long = 0L,
    val showMediaDiscoveryIcon: Boolean = false,
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
)
