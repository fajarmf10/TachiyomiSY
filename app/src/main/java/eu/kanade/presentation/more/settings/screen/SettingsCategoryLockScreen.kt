package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
        val context = LocalContext.current

        val categories by getCategories.subscribe().collectAsState(initial = emptyList())

        val hasMasterPin = CategoryLockCrypto.hasMasterPin()

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(SYMR.strings.category_lock_master_pin),
                preferenceItems = listOf(
                    kotlin.run {
                        var showMasterPinDialog by remember { mutableStateOf(false) }
                        if (showMasterPinDialog) {
                            CategoryPinDialog(
                                categoryName = stringResource(SYMR.strings.category_lock_master_pin),
                                onDismiss = { showMasterPinDialog = false },
                                onPinEntered = { pin ->
                                    try {
                                        CategoryLockCrypto.setMasterPin(pin)
                                        context.toast(SYMR.strings.category_lock_master_pin_set)
                                        showMasterPinDialog = false
                                        true
                                    } catch (e: Exception) {
                                        context.toast(e.message ?: "Error setting master PIN")
                                        false
                                    }
                                },
                                isSettingPin = true,
                            )
                        }
                        Preference.PreferenceItem.TextPreference(
                            title = stringResource(SYMR.strings.category_lock_master_pin),
                            subtitle = if (hasMasterPin) {
                                stringResource(SYMR.strings.category_lock_master_pin_change)
                            } else {
                                stringResource(SYMR.strings.category_lock_master_pin_set)
                            },
                            onClick = {
                                showMasterPinDialog = true
                            },
                        )
                    },
                ).let {
                    if (hasMasterPin) {
                        it + Preference.PreferenceItem.TextPreference(
                            title = stringResource(SYMR.strings.category_lock_master_pin_remove),
                            onClick = {
                                CategoryLockCrypto.removeMasterPin()
                                context.toast(SYMR.strings.category_lock_master_pin_removed)
                            },
                        )
                    } else {
                        it
                    }
                }.toImmutableList(),
            ),
            Preference.PreferenceGroup(
                title = stringResource(SYMR.strings.category_lock_settings),
                preferenceItems = categories
                    .filter { !it.isSystemCategory }
                    .map { category ->
                        kotlin.run {
                            val isLocked = CategoryLockCrypto.hasLock(category.id)
                            var showPinDialog by remember { mutableStateOf(false) }
                            if (showPinDialog) {
                                CategoryPinDialog(
                                    categoryName = category.name,
                                    onDismiss = { showPinDialog = false },
                                    onPinEntered = { pin ->
                                        try {
                                            CategoryLockCrypto.setPinForCategory(category.id, pin)
                                            context.toast(SYMR.strings.category_lock_pin_set)
                                            showPinDialog = false
                                            true
                                        } catch (e: Exception) {
                                            context.toast(e.message ?: "Error setting PIN")
                                            false
                                        }
                                    },
                                    isSettingPin = true,
                                )
                            }
                            Preference.PreferenceItem.TextPreference(
                                title = category.name,
                                subtitle = if (isLocked) {
                                    stringResource(SYMR.strings.category_lock_change_pin)
                                } else {
                                    stringResource(SYMR.strings.category_lock_set_pin, category.name)
                                },
                                onClick = {
                                    showPinDialog = true
                                },
                            )
                        }
                    }
                    .toImmutableList(),
            ),
        )
    }
}
// SY <--
