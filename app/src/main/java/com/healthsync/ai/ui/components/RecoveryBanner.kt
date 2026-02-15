package com.healthsync.ai.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.healthsync.ai.domain.model.RecoveryStatus
import com.healthsync.ai.ui.theme.RecoveryActive
import com.healthsync.ai.ui.theme.RecoveryFullSend
import com.healthsync.ai.ui.theme.RecoveryModerate

@Composable
fun RecoveryBanner(
    recoveryStatus: RecoveryStatus,
    modifier: Modifier = Modifier
) {
    val (color, icon, title, description) = when (recoveryStatus) {
        RecoveryStatus.FULL_SEND -> BannerData(
            RecoveryFullSend,
            Icons.Default.Bolt,
            "Full Send",
            "You're fully recovered — push hard today!"
        )
        RecoveryStatus.MODERATE -> BannerData(
            RecoveryModerate,
            Icons.Default.Warning,
            "Moderate",
            "Recovery is moderate — train smart today."
        )
        RecoveryStatus.ACTIVE_RECOVERY -> BannerData(
            RecoveryActive,
            Icons.Default.Favorite,
            "Active Recovery",
            "Take it easy — focus on recovery today."
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            androidx.compose.foundation.layout.Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = color
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class BannerData(
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String
)
