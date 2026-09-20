package com.example.dibina.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun DibinaBottomBar(
    currentTab: BottomTab,
    todayHasJournal: Boolean,
    onTabSelected: (BottomTab) -> Unit,
    onCenterActionClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dibina_bottom_bar"),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Kabar Teman
                BottomBarItem(
                    label = stringResource(R.string.nav_home),
                    icon = Icons.Default.Feed,
                    isSelected = currentTab == BottomTab.Home,
                    onClick = { onTabSelected(BottomTab.Home) },
                    modifier = Modifier.weight(1f)
                )

                // Tab 2: Rekap
                BottomBarItem(
                    label = stringResource(R.string.nav_recap),
                    icon = Icons.Default.Assessment,
                    isSelected = currentTab == BottomTab.Recap,
                    onClick = { onTabSelected(BottomTab.Recap) },
                    modifier = Modifier.weight(1f)
                )

                // Spacer for the elevated center button
                Spacer(modifier = Modifier.weight(1.2f))

                // Tab 4: Peringkat
                BottomBarItem(
                    label = stringResource(R.string.nav_leaderboard),
                    icon = Icons.Default.Leaderboard,
                    isSelected = currentTab == BottomTab.Leaderboard,
                    onClick = { onTabSelected(BottomTab.Leaderboard) },
                    modifier = Modifier.weight(1f)
                )

                // Tab 5: Pengaturan
                BottomBarItem(
                    label = stringResource(R.string.nav_settings),
                    icon = Icons.Default.Settings,
                    isSelected = currentTab == BottomTab.Settings,
                    onClick = { onTabSelected(BottomTab.Settings) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Elevated Center Button: "+" / Edit
            FloatingActionButton(
                onClick = onCenterActionClicked,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .offset(y = (-14).dp)
                    .size(56.dp)
                    .testTag("center_action_fab")
            ) {
                Icon(
                    imageVector = if (todayHasJournal) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = if (todayHasJournal) stringResource(R.string.nav_edit_journal) else stringResource(R.string.nav_create_journal),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun BottomBarItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp),
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            maxLines = 1
        )
    }
}
