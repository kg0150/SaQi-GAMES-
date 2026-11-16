package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.GameSettings
import com.example.data.TimeOfDayMode
import com.example.localization.AppLanguage
import com.example.localization.LocalizedStrings
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PatrolCobalt
import com.example.ui.theme.SirenBlue
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: GameSettings,
    strings: LocalizedStrings,
    onSelectLanguage: (AppLanguage) -> Unit,
    onMusicToggle: (Boolean) -> Unit,
    onSfxToggle: (Boolean) -> Unit,
    onShadowsToggle: (Boolean) -> Unit,
    onTimeModeSelect: (TimeOfDayMode) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianNavy.copy(alpha = 0.94f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TacticalSlate)
                            .border(1.dp, PanelBorder, RoundedCornerShape(12.dp))
                            .testTag("settings_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = TextPrimary)
                    }
                    Text(
                        text = strings.settings,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }

                // Language Selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ElevatedPanel)
                        .border(1.dp, PanelBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Language, contentDescription = null, tint = SirenBlue)
                        Text(text = strings.languageLabel, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = settings.language == lang
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) PatrolCobalt else TacticalSlate)
                                    .border(1.dp, if (selected) SirenBlue else PanelBorder, RoundedCornerShape(12.dp))
                                    .clickable { onSelectLanguage(lang) }
                                    .padding(vertical = 12.dp)
                                    .testTag("settings_lang_${lang.code}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lang.nativeLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Audio Toggles
                SettingSwitchRow(
                    title = strings.musicLabel,
                    checked = settings.musicEnabled,
                    onCheckedChange = onMusicToggle,
                    icon = Icons.Rounded.MusicNote,
                    testTag = "music_switch"
                )
                SettingSwitchRow(
                    title = strings.sfxLabel,
                    subtitle = strings.audioHint,
                    checked = settings.sfxEnabled,
                    onCheckedChange = onSfxToggle,
                    icon = Icons.Rounded.GraphicEq,
                    testTag = "sfx_switch"
                )
                SettingSwitchRow(
                    title = strings.shadowsLabel,
                    checked = settings.dynamicShadows,
                    onCheckedChange = onShadowsToggle,
                    icon = Icons.Rounded.WbSunny,
                    testTag = "shadows_switch"
                )

                // Day/Night Lighting Selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(ElevatedPanel)
                        .border(1.dp, PanelBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = strings.timeModeLabel, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    val modes = listOf(
                        TimeOfDayMode.DYNAMIC to strings.timeDynamic,
                        TimeOfDayMode.DAY to strings.timeDay,
                        TimeOfDayMode.SUNSET to strings.timeSunset,
                        TimeOfDayMode.NIGHT to strings.timeNight
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { (mode, label) ->
                                    val selected = settings.timeOfDayMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (selected) PatrolCobalt else TacticalSlate)
                                            .border(1.dp, if (selected) SirenBlue else PanelBorder, RoundedCornerShape(12.dp))
                                            .clickable { onTimeModeSelect(mode) }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ElevatedPanel)
            .border(1.dp, PanelBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = SirenBlue)
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = EmeraldSignal,
                uncheckedTrackColor = TacticalSlate
            )
        )
    }
}
