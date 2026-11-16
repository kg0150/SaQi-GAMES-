package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.OfficerId
import com.example.data.PlayerProgress
import com.example.localization.AppLanguage
import com.example.localization.LocalizedStrings
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PatrolCobalt
import com.example.ui.theme.PoliceGold
import com.example.ui.theme.SirenBlue
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MainMenuScreen(
    progress: PlayerProgress,
    currentLanguage: AppLanguage,
    strings: LocalizedStrings,
    onContinuePatrol: () -> Unit,
    onFreePatrol: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenCharacters: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    val rank = when {
        progress.totalScore >= 3000 -> strings.rankCommander
        progress.totalScore >= 1000 -> strings.rankInspector
        else -> strings.rankRookie
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ObsidianNavy.copy(alpha = 0.72f),
                        ObsidianNavy.copy(alpha = 0.88f),
                        ObsidianNavy.copy(alpha = 0.96f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Bar: Score/Rank & Language Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(TacticalSlate.copy(alpha = 0.9f))
                            .border(1.dp, PoliceGold.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = PoliceGold, modifier = Modifier.size(18.dp))
                        Column {
                            Text(
                                text = "${strings.totalScore}: ${progress.totalScore}",
                                style = MaterialTheme.typography.labelLarge,
                                color = PoliceGold
                            )
                            Text(
                                text = "${strings.rankLabel}: $rank",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(TacticalSlate.copy(alpha = 0.9f))
                            .border(1.dp, PanelBorder, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = lang == currentLanguage
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) PatrolCobalt else Color.Transparent)
                                    .clickable { onSelectLanguage(lang) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("lang_chip_${lang.code}")
                            ) {
                                Text(
                                    text = lang.nativeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) TextPrimary else TextSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Hero Banner Card with generated 3D key art
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(195.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.5.dp, PatrolCobalt.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_menu_hero),
                        contentDescription = strings.appTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        ObsidianNavy.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Shield, contentDescription = null, tint = PoliceGold, modifier = Modifier.size(22.dp))
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = strings.appSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SirenBlue
                            )
                        }

                        // Active Officer Avatar Badge
                        val portrait = if (progress.selectedOfficer == OfficerId.MOTU) {
                            R.drawable.img_motu_portrait
                        } else {
                            R.drawable.img_patlu_portrait
                        }
                        Image(
                            painter = painterResource(id = portrait),
                            contentDescription = strings.activeOfficer,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, PoliceGold, CircleShape)
                                .clickable(onClick = onOpenCharacters)
                        )
                    }
                }

                // Primary Action Buttons
                Button(
                    onClick = onContinuePatrol,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("play_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PatrolCobalt,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = if (progress.completedMissions.isEmpty()) strings.playPatrol else strings.continuePatrol,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                OutlinedButton(
                    onClick = onFreePatrol,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("free_patrol_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SirenBlue.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = TacticalSlate.copy(alpha = 0.78f),
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = SirenBlue)
                    Spacer(Modifier.size(8.dp))
                    Text(text = strings.freePatrol, style = MaterialTheme.typography.titleMedium)
                }

                // Navigation Grid: Missions, Officers, Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuTileCard(
                        title = strings.missions,
                        subtitle = "${progress.completedMissions.size}/5",
                        icon = Icons.AutoMirrored.Rounded.Assignment,
                        accent = PoliceGold,
                        onClick = onOpenMissions,
                        testTag = "missions_menu_button",
                        modifier = Modifier.weight(1f)
                    )
                    MenuTileCard(
                        title = strings.officers,
                        subtitle = if (progress.selectedOfficer == OfficerId.MOTU) strings.motuName else strings.patluName,
                        icon = Icons.Rounded.Badge,
                        accent = SirenBlue,
                        onClick = onOpenCharacters,
                        testTag = "officers_menu_button",
                        modifier = Modifier.weight(1f)
                    )
                    MenuTileCard(
                        title = strings.settings,
                        subtitle = currentLanguage.nativeLabel,
                        icon = Icons.Rounded.Settings,
                        accent = PatrolCobalt,
                        onClick = onOpenSettings,
                        testTag = "settings_menu_button",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuTileCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(ElevatedPanel.copy(alpha = 0.90f))
            .border(1.dp, PanelBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(24.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1
        )
    }
}
