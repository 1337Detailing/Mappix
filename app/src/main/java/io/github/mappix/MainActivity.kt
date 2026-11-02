package io.github.mappix

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.mappix.input.ControllerEventDispatcher
import io.github.mappix.ui.editor.EditorScreen
import io.github.mappix.ui.editor.EditorViewModel
import io.github.mappix.ui.home.HomeScreen
import io.github.mappix.ui.home.HomeViewModel
import io.github.mappix.ui.onboarding.OnboardingScreen
import io.github.mappix.ui.profiles.ProfilesScreen
import io.github.mappix.ui.profiles.ProfilesViewModel
import io.github.mappix.ui.settings.SettingsScreen
import io.github.mappix.ui.settings.SettingsViewModel
import io.github.mappix.ui.settings.UpdateViewModel
import io.github.mappix.ui.tester.TesterScreen
import io.github.mappix.ui.tester.TesterViewModel
import io.github.mappix.ui.theme.MappixTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var dispatcher: ControllerEventDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MappixAppScreen() }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // L'application teste les boutons sans empêcher le système de naviguer.
        dispatcher.onKey(event)
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean =
        if (dispatcher.onMotion(event)) true else super.dispatchGenericMotionEvent(event)
}

private enum class Screen { HOME, PROFILES, TESTER, SETTINGS, EDITOR, ONBOARDING }

@Composable
private fun MappixAppScreen() {
    val home: HomeViewModel = hiltViewModel()
    val profiles: ProfilesViewModel = hiltViewModel()
    val editor: EditorViewModel = hiltViewModel()
    val tester: TesterViewModel = hiltViewModel()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val updateVm: UpdateViewModel = hiltViewModel()
    val settings by settingsVm.settings.collectAsState()
    var screen by remember { mutableStateOf(Screen.HOME) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var permissionDialogOpened by remember { mutableStateOf(false) }
    LaunchedEffect(settings?.onboardingSeen) {
        if (settings != null && settings?.onboardingSeen == false && !permissionDialogOpened) {
            screen = Screen.ONBOARDING
            permissionDialogOpened = true
        }
    }
    LaunchedEffect(settings?.language) {
        settings?.language?.let { code ->
            val locale = LocaleListCompat.forLanguageTags(code)
            if (AppCompatDelegate.getApplicationLocales() != locale) AppCompatDelegate.setApplicationLocales(locale)
        }
    }
    MappixTheme(dark = settings?.darkMode ?: true) {
        Scaffold(
            bottomBar = {
                if (screen !in listOf(Screen.EDITOR, Screen.ONBOARDING)) NavigationBar {
                    listOf(
                        Triple(Screen.HOME, R.string.nav_home, Icons.Rounded.Home),
                        Triple(Screen.PROFILES, R.string.nav_profiles, Icons.Rounded.Tune),
                        Triple(Screen.TESTER, R.string.nav_tester, Icons.Rounded.SportsEsports),
                        Triple(Screen.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings)
                    ).forEach { (target, label, icon) ->
                        NavigationBarItem(selected = screen == target, onClick = { screen = target },
                            icon = { Icon(icon, null) }, label = { Text(stringResource(label)) })
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                when (screen) {
                    Screen.HOME -> HomeScreen(home, { screen = Screen.PROFILES }, { screen = Screen.TESTER }, { screen = Screen.ONBOARDING })
                    Screen.PROFILES -> ProfilesScreen(profiles) { id -> editingId = id; screen = Screen.EDITOR }
                    Screen.TESTER -> TesterScreen(tester)
                    Screen.SETTINGS -> SettingsScreen(settingsVm, updateVm) { screen = Screen.ONBOARDING }
                    Screen.EDITOR -> editingId?.let { EditorScreen(it, editor) { screen = Screen.PROFILES } }
                    Screen.ONBOARDING -> Column(Modifier.verticalScroll(rememberScrollState())) {
                        OnboardingScreen { settingsVm.onboardingDone(); screen = Screen.HOME }
                    }
                }
            }
        }
    }
}
