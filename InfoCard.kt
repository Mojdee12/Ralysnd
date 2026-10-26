package com.kunk.singbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kunk.singbox.R
import com.kunk.singbox.ui.theme.isLiquidGlassTheme
import com.kunk.singbox.ui.theme.liquidGlassPanel
import com.kunk.singbox.ui.theme.liquidGlassProgressColor
import com.kunk.singbox.ui.theme.liquidGlassProgressTrackColor
import com.kunk.singbox.ui.theme.RalysndEmerald
import com.kunk.singbox.ui.theme.RalysndEmeraldBright
import com.kunk.singbox.ui.theme.RalysndGlassCard
import com.kunk.singbox.ui.theme.RalysndOutline
import com.kunk.singbox.ui.theme.RalysndSurfaceAlt
import com.kunk.singbox.ui.theme.RalysndVioletLight

@Suppress("LongMethod")
@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    uploadSpeed: String = "0 KB/s",
    downloadSpeed: String = "0 KB/s",
    ping: String = "0 ms",
    isPingLoading: Boolean = false,
    onPingClick: (() -> Unit)? = null
) {
    // RALYSND MOCKUP 2026: one dark glass card holding three stat cells
    // (upload / download / ping), separated by thin dividers, each with a
    // tinted icon chip. Values are exactly the caller's live stats.
    val shape = RoundedCornerShape(24.dp)
    val containerModifier = if (isLiquidGlassTheme()) {
        modifier
            .fillMaxWidth()
            .liquidGlassPanel(shape = shape)
    } else {
        modifier.fillMaxWidth()
    }

    if (isLiquidGlassTheme()) {
        Row(
            modifier = containerModifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
        // 三等分，保证水平间距一致且整体居中
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            InfoItem(
                icon = Icons.Rounded.ArrowUpward,
                label = stringResource(R.string.info_card_upload),
                value = uploadSpeed
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            InfoItem(
                icon = Icons.Rounded.ArrowDownward,
                label = stringResource(R.string.info_card_download),
                value = downloadSpeed
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // 固定正圆点击区，加载态不变成圆角矩形
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .then(
                        if (onPingClick != null) {
                            Modifier.clickable(
                                enabled = !isPingLoading,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onPingClick
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                InfoItem(
                    icon = Icons.Rounded.Speed,
                    label = stringResource(R.string.info_card_ping),
                    value = ping,
                    isLoading = isPingLoading
                )
            }
        }
    }
    }
    if (!isLiquidGlassTheme()) {
        RalysndGlassCard(modifier = containerModifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    RalysndInfoItem(
                        icon = Icons.Rounded.ArrowUpward,
                        label = stringResource(R.string.info_card_upload),
                        value = uploadSpeed,
                        iconTint = RalysndEmeraldBright
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(RalysndOutline)
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    RalysndInfoItem(
                        icon = Icons.Rounded.ArrowDownward,
                        label = stringResource(R.string.info_card_download),
                        value = downloadSpeed,
                        iconTint = RalysndVioletLight
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(RalysndOutline)
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .then(
                                if (onPingClick != null) {
                                    Modifier.clickable(
                                        enabled = !isPingLoading,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true),
                                        onClick = onPingClick
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        RalysndInfoItem(
                            icon = Icons.Rounded.Speed,
                            label = stringResource(R.string.info_card_ping),
                            value = ping,
                            iconTint = RalysndEmerald,
                            isLoading = isPingLoading
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RalysndInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color,
    isLoading: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(RalysndSurfaceAlt),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier.height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = RalysndEmeraldBright,
                    strokeWidth = 2.dp,
                    trackColor = Color.Transparent
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    isLoading: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier.height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = liquidGlassProgressColor(MaterialTheme.colorScheme.onSurface),
                    strokeWidth = 2.dp,
                    trackColor = liquidGlassProgressTrackColor(Color.Transparent)
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
