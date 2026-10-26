package com.kunk.singbox.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kunk.singbox.ui.theme.isLiquidGlassTheme
import com.kunk.singbox.ui.theme.liquidGlassPanel
import com.kunk.singbox.ui.theme.liquidGlassPressFeedback
import com.kunk.singbox.ui.theme.RalysndGlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurfaceVariant),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    if (isLiquidGlassTheme()) {
        Column(
            modifier = modifier
                .liquidGlassPressFeedback(
                    label = "liquid_glass_standard_card_scale",
                    onClick = onClick
                )
                .liquidGlassPanel(shape = shape, shadowElevation = 6.dp),
            content = content
        )
        return
    }

    // RALYSND MOCKUP 2026: dark glass card instead of the flat surface card.
    // onClick/border callers keep working: onClick is forwarded, and the
    // legacy border parameter is intentionally superseded by the glass
    // outline so every StandardCard shares one look.
    RalysndGlassCard(
        modifier = modifier,
        onClick = onClick,
        content = content
    )
}
