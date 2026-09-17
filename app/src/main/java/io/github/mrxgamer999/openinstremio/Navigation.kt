package io.github.mrxgamer999.openinstremio

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.mrxgamer999.openinstremio.data.AppGraph
import io.github.mrxgamer999.openinstremio.ui.about.AboutScreen
import io.github.mrxgamer999.openinstremio.ui.home.HomeScreen
import io.github.mrxgamer999.openinstremio.ui.settings.SettingsScreen
import io.github.mrxgamer999.openinstremio.ui.setup.SetupGuideScreen
import io.github.mrxgamer999.openinstremio.ui.updates.UpdateScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Opens on the setup guide, stacked on Home, while the user has not yet picked which apps the
 * button opens; otherwise on Home. Nothing is drawn for the moment it takes to find out, so
 * Home does not flash first.
 */
@Composable
fun MainNavigation() {
  val appContext = LocalContext.current.applicationContext
  val isChosen by
    produceState<Boolean?>(initialValue = null) {
      value =
        try {
          AppGraph.playerChoiceStore(appContext).isChosen.first()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          // An unreadable store is no reason to trap anyone in the guide.
          Log.w("MainNavigation", "Player choice read failed", e)
          true
        }
    }
  isChosen?.let { MainNavigation(openSetupGuide = !it) }
}

@Composable
private fun MainNavigation(openSetupGuide: Boolean) {
  // Saveable, so the initial keys only apply to a fresh start: a rotation restores the stack as
  // it was and cannot stack a second guide. One call site on purpose: an `if` around two calls
  // would save them under different keys, and a choice made before rotating would then restore
  // the other branch's fresh stack.
  val initialKeys = if (openSetupGuide) arrayOf<NavKey>(HomeKey, SetupGuideKey) else arrayOf<NavKey>(HomeKey)
  val backStack = rememberNavBackStack(*initialKeys)
  val onBack: () -> Unit = { backStack.removeLastOrNull() }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    // Scope each screen's ViewModel to its back-stack entry (not the activity), so leaving
    // a screen disposes its ViewModel and revisiting it starts fresh - e.g. the update
    // checker re-checks instead of showing a stale result.
    entryDecorators =
      listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
    entryProvider =
      entryProvider {
        entry<HomeKey> { HomeScreen(onNavigate = { navKey -> backStack.add(navKey) }) }
        entry<SetupGuideKey> { SetupGuideScreen(onBack = onBack) }
        entry<SettingsKey> { SettingsScreen(onBack = onBack) }
        entry<AboutKey> { AboutScreen(onBack = onBack) }
        entry<UpdatesKey> { UpdateScreen(onBack = onBack) }
      },
  )
}
