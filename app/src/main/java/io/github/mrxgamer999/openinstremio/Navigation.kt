package io.github.mrxgamer999.openinstremio

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.mrxgamer999.openinstremio.ui.about.AboutScreen
import io.github.mrxgamer999.openinstremio.ui.home.HomeScreen
import io.github.mrxgamer999.openinstremio.ui.setup.SetupGuideScreen
import io.github.mrxgamer999.openinstremio.ui.updates.UpdateScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(HomeKey)
  val onBack: () -> Unit = { backStack.removeLastOrNull() }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<HomeKey> { HomeScreen(onNavigate = { navKey -> backStack.add(navKey) }) }
        entry<SetupGuideKey> { SetupGuideScreen(onBack = onBack) }
        entry<AboutKey> { AboutScreen(onBack = onBack) }
        entry<UpdatesKey> { UpdateScreen(onBack = onBack) }
      },
  )
}
