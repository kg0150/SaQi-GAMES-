package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.OfficerId
import com.example.localization.LocalizedStrings
import com.example.ui.theme.ElevatedPanel
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.PanelBorder
import com.example.ui.theme.PatrolCobalt
import com.example.ui.theme.PoliceGold
import com.example.ui.theme.SirenBlue
import com.example.ui.theme.TacticalSlate
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CharacterInfoScreen(
    selectedOfficer: OfficerId,
    strings: LocalizedStrings,
    onSelectOfficer: (OfficerId) -> Unit,
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
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
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
                            .testTag("officers_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = TextPrimary)
                    }
                    Text(
                        text = strings.officers,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }

                OfficerDetailCard(
                    name = strings.motuName,
                    title = strings.motuTitle,
                    bio = strings.motuBio,
                    portraitRes = R.drawable.img_motu_portrait,
                    isSelected = selectedOfficer == OfficerId.MOTU,
                    accent = PoliceGold,
                    stats = listOf(
                        strings.statStrength to 0.94f,
                        strings.statStamina to 0.90f,
                        strings.statSpeed to 0.72f,
                        strings.statTactics to 0.78f
                    ),
                    strings = strings,
                    onSelect = { onSelectOfficer(OfficerId.MOTU) },
                    testTag = "select_motu_button"
                )

                OfficerDetailCard(
                    name = strings.patluName,
                    title = strings.patluTitle,
                    bio = strings.patluBio,
                    portraitRes = R.drawable.img_patlu_portrait,
                    isSelected = selectedOfficer == OfficerId.PATLU,
                    accent = SirenBlue,
                    stats = listOf(
                        strings.statStrength to 0.70f,
                        strings.statStamina to 0.80f,
                        strings.statSpeed to 0.96f,
                        strings.statTactics to 0.95f
                    ),
                    strings = strings,
                    onSelect = { onSelectOfficer(OfficerId.PATLU) },
                    testTag = "select_patlu_button"
                )
            }
        }
    }
}

@Composable
private fun OfficerDetailCard(
    name: String,
    title: String,
    bio: String,
    portraitRes: Int,
    isSelected: Boolean,
    accent: Color,
    stats: List<Pair<String, Float>>,
    strings: LocalizedStrings,
    onSelect: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ElevatedPanel)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) accent else PanelBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = portraitRes),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, accent, RoundedCornerShape(16.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = name, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Text(text = title, style = MaterialTheme.typography.labelLarge, color = accent)
                Text(text = bio, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            stats.forEach { (label, value) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.widthIn(min = 84.dp)
                    )
                    LinearProgressIndicator(
                        progress = { value },
                        modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)),
                        color = accent,
                        trackColor = ObsidianNavy
                    )
                }
            }
        }

        Button(
            onClick = onSelect,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag(testTag),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) EmeraldSignal else PatrolCobalt,
                contentColor = if (isSelected) ObsidianNavy else TextPrimary
            )
        ) {
            if (isSelected) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(text = strings.activeOfficer, style = MaterialTheme.typography.labelLarge)
            } else {
                Text(text = strings.selectOfficer, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
