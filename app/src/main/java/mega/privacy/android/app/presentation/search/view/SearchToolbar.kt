package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import mega.privacy.android.app.R
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.appbar.SelectModeAppBar
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.legacy.core.ui.controls.appbar.ExpandedSearchAppBar

/**
 * Search toolbar used in search activity
 *
 * @param selectionCount
 * @param searchQuery
 * @param updateSearchQuery
 * @param menuActions
 */
@Composable
fun SearchToolBar(
    selectionCount: Int,
    searchQuery: String,
    updateSearchQuery: (String) -> Unit,
    menuActions: List<MenuAction> = emptyList(),
) {
    if (selectionCount > 0) {
        SelectModeAppBar(title = "$selectionCount", actions = menuActions)
    } else {
        ExpandedSearchAppBar(
            text = searchQuery,
            hintId = R.string.hint_action_search,
            onSearchTextChange = { updateSearchQuery(it) },
            onCloseClicked = { updateSearchQuery("") },
            elevation = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewSearchToolbar() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SearchToolBar(
            selectionCount = 10,
            searchQuery = "searchQuery",
            updateSearchQuery = {},
        )
    }
}