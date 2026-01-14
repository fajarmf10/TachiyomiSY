package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.library.components.CategoryPinDialog
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.util.storage.CategoryLockCrypto
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// SY -->
object SettingsCategoryLockScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = SYMR.strings.category_lock_settings

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetCategories>() }
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val categories by getCategories.subscribe().collectAsState(initial = emptyList())
        var showPinDialog by remember { mutableStateOf<Pair<Category, Boolean>?>(null) }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(SYMR.strings.category_lock_settings),
                preferenceItems = categories
                    .filter { !it.isSystemCategory }
                    .map { category ->
                        val isLocked = CategoryLockCrypto.hasLock(category.id)

                        Preference.PreferenceItem.TextPreference(
                            title = category.name,
                            subtitle = if (isLocked) {
                                stringResource(SYMR.strings.category_lock_change_pin)
                            } else {
                                stringResource(SYMR.strings.category_lock_set_pin, category.name)
                            },
                            onClick = {
                                showPinDialog = category to true
                            },
                        )
                    }
                    .toImmutableList(),
            ),
        ).also {
            // Show PIN dialog if needed
            showPinDialog?.let { (category, isSettingPin) ->
                CategoryPinDialog(
                    categoryName = category.name,
                    onDismiss = { showPinDialog = null },
                    onPinEntered = { pin ->
                        try {
                            CategoryLockCrypto.setPinForCategory(category.id, pin)
                            context.toast(SYMR.strings.category_lock_pin_set)
                            showPinDialog = null
                            true
                        } catch (e: Exception) {
                            context.toast(e.message ?: "Error setting PIN")
                            false
                        }
                    },
                    isSettingPin = true,
                )
            }
        }
    }
}
// SY <--
