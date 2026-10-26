package com.kunk.singbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kunk.singbox.ui.theme.Neutral500
import com.kunk.singbox.ui.theme.isLiquidGlassTheme
import com.kunk.singbox.ui.theme.liquidGlassPanel
import com.kunk.singbox.ui.theme.liquidGlassPressFeedback
import com.kunk.singbox.ui.theme.RalysndEmeraldBright
import com.kunk.singbox.ui.theme.RalysndOutline
import com.kunk.singbox.ui.theme.RalysndSurface

@Composable
private fun Modifier.modeChipIndicatorPanel(indicatorColor: Color): Modifier {
    return if (isLiquidGlassTheme()) {
        size(10.dp)
            .liquidGlassPanel(shape = CircleShape, selected = true, shadowElevation = 2.dp)
            .background(indicatorColor.copy(alpha = 0.12f), CircleShape)
    } else {
        size(8.dp)
            .background(indicatorColor, CircleShape)
    }
}

@Composable
private fun Modifier.statusChipPressFeedback(
    useLiquidGlass: Boolean,
    onClick: (() -> Unit)?
): Modifier = liquidGlassPressFeedback(
    useLiquidGlass = useLiquidGlass,
    pressedScale = 0.96f,
    label = "liquid_glass_status_chip_scale",
    onClick = onClick
)

@Composable
fun StatusChip(
    label: String,
    icon: @Composable (() -> Unit)? = null,
    isActive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val useLiquidGlass = isLiquidGlassTheme()
    val textColor = when {
        useLiquidGlass && isActive -> MaterialTheme.colorScheme.primary
        isActive -> RalysndEmeraldBright
        useLiquidGlass -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val surfaceModifier = if (useLiquidGlass) {
        Modifier.liquidGlassPanel(
            shape = RoundedCornerShape(20.dp),
            selected = isActive,
            shadowElevation = 6.dp
        )
    } else {
        // RALYSND MOCKUP 2026: dark glass status pill — charcoal surface,
        // emerald border/text when active, thin charcoal outline otherwise.
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(RalysndSurface)
            .border(
                1.dp,
                if (isActive) RalysndEmeraldBright else RalysndOutline,
                RoundedCornerShape(20.dp)
            )
    }
    val modifier = surfaceModifier
        .statusChipPressFeedback(useLiquidGlass = useLiquidGlass, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 6.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isActive && icon == null) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(RalysndEmeraldBright, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (icon != null) {
            Box(modifier = Modifier.size(16.dp)) {
                icon()
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun ModeChip(
    mode: String,
    indicatorColor: Color = Neutral500,
    onClick: () -> Unit
) {
    val useLiquidGlass = isLiquidGlassTheme()
    StatusChip(
        label = mode,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .modeChipIndicatorPanel(indicatorColor)
                )
                if (useLiquidGlass) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(indicatorColor, CircleShape)
                    )
                }
            }
        },
        onClick = onClick
    )
}
