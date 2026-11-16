package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.HudSnapshot
import com.example.data.MissionCatalog
import com.example.localization.LocalizedStrings
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PatrolCobalt
import com.example.ui.theme.PoliceGold
import com.example.ui.theme.SirenRed
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PauseOverlay(
    strings: LocalizedStrings,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    BackHandler(onBack = onResume)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianNavy.copy(alpha = 0.78f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(ElevatedPanel)
                .border(1.5.dp, PatrolCobalt, RoundedCornerShape(24.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = strings.pauseTitle, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)

            Button(
                onClick = onResume,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("pause_resume_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PatrolCobalt, contentColor = TextPrimary)
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(text = strings.resume, style = MaterialTheme.typography.titleMedium)
            }

            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_restart_button"),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PanelBorder)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = PoliceGold)
                Spacer(Modifier.size(6.dp))
                Text(text = strings.restartMission, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }

            OutlinedButton(
                onClick = onExitToMenu,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_exit_button"),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SirenRed.copy(alpha = 0.6f))
            ) {
                Text(text = strings.exitToMenu, style = MaterialTheme.typography.titleMedium, color = SirenRed)
            }
        }
    }
}

@Composable
fun MissionResultOverlay(
    hud: HudSnapshot,
    areaUnlocked: Boolean,
    strings: LocalizedStrings,
    onNextMission: (Int) -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit
) {
    val isComplete = hud.missionCompletePoints != null
    val hasNext = hud.activeMissionId in 1 until MissionCatalog.missions.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianNavy.copy(alpha = 0.84f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(ElevatedPanel)
                .border(2.dp, if (isComplete) PoliceGold else SirenRed, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isComplete) Icons.Rounded.EmojiEvents else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = if (isComplete) PoliceGold else SirenRed,
                modifier = Modifier.size(56.dp)
            )

            Text(
                text = if (isComplete) strings.missionCompleteTitle else strings.missionFailedTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = if (isComplete) PoliceGold else SirenRed
            )

            if (isComplete) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { idx ->
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (idx < hud.missionCompleteStars) PoliceGold else TacticalSlate,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TacticalSlate)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = strings.scoreEarned, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(text = "+${hud.missionCompletePoints}", style = MaterialTheme.typography.titleMedium, color = EmeraldSignal)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = strings.timeBonus, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(text = "+${hud.missionCompleteBonus}", style = MaterialTheme.typography.titleMedium, color = PoliceGold)
                    }
                }

                if (areaUnlocked) {
                    Text(
                        text = strings.areaUnlockedToast,
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldSignal
                    )
                }

                if (hasNext) {
                    Button(
                        onClick = { onNextMission(hud.activeMissionId + 1) },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("next_mission_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PatrolCobalt, contentColor = TextPrimary)
                    ) {
                        Text(text = strings.nextMission, style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("try_again_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SirenRed, contentColor = TextPrimary)
                ) {
                    Text(text = strings.tryAgain, style = MaterialTheme.typography.titleMedium)
                }
            }

            OutlinedButton(
                onClick = onExitToMenu,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("result_menu_button"),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PanelBorder)
            ) {
                Text(text = strings.exitToMenu, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
        }
    }
}
