package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MissionCatalog
import com.example.data.MissionDef
import com.example.data.PlayerProgress
import com.example.localization.LocalizedStrings
import com.example.ui.components.districtLabel
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PatrolCobalt
import com.example.ui.theme.PoliceGold
import com.example.ui.theme.SirenBlue
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MissionsScreen(
    progress: PlayerProgress,
    strings: LocalizedStrings,
    onStartMission: (Int) -> Unit,
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            .testTag("missions_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = TextPrimary)
                    }
                    Text(
                        text = strings.missions,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(MissionCatalog.missions, key = { it.id }) { mission ->
                        val unlocked = mission.id <= progress.highestUnlockedMission
                        val completed = mission.id in progress.completedMissions
                        val stars = progress.missionStars[mission.id] ?: 0
                        MissionCard(
                            mission = mission,
                            unlocked = unlocked,
                            completed = completed,
                            stars = stars,
                            strings = strings,
                            onStart = { onStartMission(mission.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionCard(
    mission: MissionDef,
    unlocked: Boolean,
    completed: Boolean,
    stars: Int,
    strings: LocalizedStrings,
    onStart: () -> Unit
) {
    val (title, desc) = when (mission.id) {
        1 -> strings.mission1Title to strings.mission1Desc
        2 -> strings.mission2Title to strings.mission2Desc
        3 -> strings.mission3Title to strings.mission3Desc
        4 -> strings.mission4Title to strings.mission4Desc
        else -> strings.mission5Title to strings.mission5Desc
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (unlocked) ElevatedPanel else TacticalSlate.copy(alpha = 0.65f))
            .border(
                1.dp,
                when {
                    completed -> EmeraldSignal.copy(alpha = 0.6f)
                    unlocked -> PatrolCobalt.copy(alpha = 0.6f)
                    else -> PanelBorder
                },
                RoundedCornerShape(18.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (unlocked) TextPrimary else TextMuted
                )
                Text(
                    text = "${districtLabel(mission.district, strings)}  •  ${strings.rewardPts}: +${mission.rewardPoints}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unlocked) SirenBlue else TextMuted
                )
            }
            if (completed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(3) { idx ->
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (idx < stars) PoliceGold else PanelBorder,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                    Icon(Icons.Rounded.CheckCircle, contentDescription = strings.completed, tint = EmeraldSignal, modifier = Modifier.size(20.dp))
                }
            } else if (!unlocked) {
                Icon(Icons.Rounded.Lock, contentDescription = strings.locked, tint = TextMuted, modifier = Modifier.size(20.dp))
            }
        }

        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = if (unlocked) TextSecondary else TextMuted
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onStart,
                enabled = unlocked,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PatrolCobalt,
                    contentColor = TextPrimary,
                    disabledContainerColor = TacticalSlate,
                    disabledContentColor = TextMuted
                ),
                modifier = Modifier.testTag("start_mission_${mission.id}")
            ) {
                Icon(
                    imageVector = if (unlocked) Icons.Rounded.PlayArrow else Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = if (unlocked) strings.startMission else strings.locked,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
