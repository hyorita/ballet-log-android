package com.hyorita.balletlog.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyorita.balletlog.data.model.WorkoutInfo

/**
 * Workout summary card matching iOS `WorkoutCardView`.
 *
 * Header (icon + title + source) over a 4-up stat row (duration / kcal /
 * avg BPM / max BPM). Tinted accent background.
 */
@Composable
fun WorkoutCard(
    workout: WorkoutInfo,
    title: String,
    durationLabel: String,
    activeCalLabel: String,
    avgBpmLabel: String,
    maxBpmLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.SportsGymnastics,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            if (workout.sourceName.isNotEmpty()) {
                Text(
                    workout.sourceName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatItem(
                icon = "⏱",
                value = formatDuration(workout.durationMinutes),
                label = durationLabel,
                modifier = Modifier.weight(1f)
            )
            StatItem(
                icon = "🔥",
                value = "${workout.activeCalories}",
                label = activeCalLabel,
                modifier = Modifier.weight(1f)
            )
            if (workout.avgHeartRate > 0) {
                StatItem(
                    icon = "❤",
                    value = "${workout.avgHeartRate}",
                    label = avgBpmLabel,
                    modifier = Modifier.weight(1f)
                )
            }
            if (workout.maxHeartRate > 0) {
                StatItem(
                    icon = "❤",
                    value = "${workout.maxHeartRate}",
                    label = maxBpmLabel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(icon, fontSize = 12.sp)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private fun formatDuration(minutes: Int): String {
    if (minutes < 60) return "${minutes}m"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}
