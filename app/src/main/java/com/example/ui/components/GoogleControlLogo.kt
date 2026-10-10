package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun GoogleControlLogo(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    showText: Boolean = false,
    textColor: Color = Color.White
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier.size(size)
        ) {
            drawGoogleControlDial(this)
        }

        if (showText) {
            Spacer(modifier = Modifier.height(10.dp))
            GoogleControlBrandText(textColor = textColor)
        }
    }
}

@Composable
fun GoogleControlBrandText(
    modifier: Modifier = Modifier,
    fontSize: Int = 22,
    textColor: Color = Color.White
) {
    val annotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = GoogleBlue, fontWeight = FontWeight.Bold)) { append("G") }
        withStyle(SpanStyle(color = GoogleRed, fontWeight = FontWeight.Bold)) { append("o") }
        withStyle(SpanStyle(color = GoogleYellow, fontWeight = FontWeight.Bold)) { append("o") }
        withStyle(SpanStyle(color = GoogleBlue, fontWeight = FontWeight.Bold)) { append("g") }
        withStyle(SpanStyle(color = GoogleGreen, fontWeight = FontWeight.Bold)) { append("l") }
        withStyle(SpanStyle(color = GoogleRed, fontWeight = FontWeight.Bold)) { append("e") }
        append(" ")
        withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.Bold)) { append("Control") }
    }

    Text(
        text = annotatedString,
        fontSize = fontSize.sp,
        letterSpacing = 0.5.sp,
        modifier = modifier
    )
}

private fun drawGoogleControlDial(scope: DrawScope) {
    with(scope) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        val outerRadius = w * 0.42f
        val strokeWidth = w * 0.17f
        val arcRadius = outerRadius - strokeWidth / 2f
        val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
        val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)

        // Segment 1: Top-Left Blue Arc (from 180° to 270°, sweep 90°)
        drawArc(
            color = Color(0xFF3B82F6),
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Segment 2: Top-Right Red Arc (from 270° to 330°, sweep 60°)
        drawArc(
            color = Color(0xFFEF4444),
            startAngle = 270f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Segment 3: Bottom-Left Yellow Arc (from 90° to 180°, sweep 90°)
        drawArc(
            color = Color(0xFFFBBF24),
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Segment 4: Bottom-Right Green Arc (from 0° to 90°, sweep 90°)
        drawArc(
            color = Color(0xFF10B981),
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )

        // Sub-groove arc highlights (tactile control slots inside the ring)
        val grooveStroke = strokeWidth * 0.22f
        drawArc(
            color = Color(0x33000000),
            startAngle = 195f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = grooveStroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0x33000000),
            startAngle = 280f,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = grooveStroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0x33000000),
            startAngle = 105f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = grooveStroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0x33000000),
            startAngle = 15f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = grooveStroke, cap = StrokeCap.Round)
        )

        // Center Horizontal Bar of the "G" (Blue track capsule)
        val barHeight = strokeWidth * 0.95f
        val barWidth = w * 0.45f
        val barTopLeft = Offset(center.x - w * 0.05f, center.y - barHeight / 2f)
        val barCorner = barHeight / 2f

        // Blue track background
        drawRoundRect(
            color = Color(0xFF3B82F6),
            topLeft = barTopLeft,
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barCorner, barCorner)
        )

        // Inner darker slot of the center bar
        val slotHeight = barHeight * 0.45f
        val slotWidth = barWidth * 0.75f
        val slotTopLeft = Offset(barTopLeft.x + barWidth * 0.12f, center.y - slotHeight / 2f)
        drawRoundRect(
            color = Color(0xFF1E3A8A),
            topLeft = slotTopLeft,
            size = Size(slotWidth, slotHeight),
            cornerRadius = CornerRadius(slotHeight / 2f, slotHeight / 2f)
        )

        // Center Glowing Pearl Slider Knob on the horizontal bar
        val centerKnobRadius = barHeight * 0.48f
        val centerKnobPos = Offset(center.x + barWidth * 0.28f, center.y)

        // Subtle shadow behind knob
        drawCircle(
            color = Color(0x40000000),
            radius = centerKnobRadius * 1.15f,
            center = centerKnobPos + Offset(0f, 2f)
        )
        // Pearl metallic gradient knob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFE2E8F0), Color(0xFF94A3B8)),
                center = centerKnobPos - Offset(centerKnobRadius * 0.25f, centerKnobRadius * 0.25f),
                radius = centerKnobRadius
            ),
            radius = centerKnobRadius,
            center = centerKnobPos
        )

        // Top Metallic Volume Knob (at top junction between Blue and Red)
        val topKnobPos = Offset(center.x, center.y - arcRadius)
        val topKnobRadius = strokeWidth * 0.42f
        drawCircle(
            color = Color(0x33000000),
            radius = topKnobRadius * 1.15f,
            center = topKnobPos + Offset(0f, 2f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFCBD5E1), Color(0xFF64748B)),
                center = topKnobPos - Offset(topKnobRadius * 0.2f, topKnobRadius * 0.2f),
                radius = topKnobRadius
            ),
            radius = topKnobRadius,
            center = topKnobPos
        )
        // Indicator dot on top knob
        drawCircle(
            color = Color(0xFF0F172A),
            radius = topKnobRadius * 0.22f,
            center = topKnobPos - Offset(0f, topKnobRadius * 0.45f)
        )

        // Left Slider Knob (Yellow segment at 180°)
        val leftKnobPos = Offset(center.x - arcRadius, center.y)
        val leftKnobWidth = strokeWidth * 0.4f
        val leftKnobHeight = strokeWidth * 0.7f
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.White, Color(0xFFCBD5E1), Color(0xFF94A3B8))
            ),
            topLeft = Offset(leftKnobPos.x - leftKnobWidth / 2f, leftKnobPos.y - leftKnobHeight / 2f),
            size = Size(leftKnobWidth, leftKnobHeight),
            cornerRadius = CornerRadius(leftKnobWidth / 3f, leftKnobWidth / 3f)
        )

        // Bottom-Right Slider Lever (Green segment at ~45°)
        val angleRad = Math.toRadians(45.0)
        val brKnobPos = Offset(
            center.x + (arcRadius * Math.cos(angleRad)).toFloat(),
            center.y + (arcRadius * Math.sin(angleRad)).toFloat()
        )
        val brKnobRadius = strokeWidth * 0.36f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFCBD5E1), Color(0xFF475569)),
                center = brKnobPos,
                radius = brKnobRadius
            ),
            radius = brKnobRadius,
            center = brKnobPos
        )

        // Bottom dial knob (Green segment lower junction at ~115°)
        val dialAngleRad = Math.toRadians(120.0)
        val dialPos = Offset(
            center.x + (arcRadius * Math.cos(dialAngleRad)).toFloat(),
            center.y + (arcRadius * Math.sin(dialAngleRad)).toFloat()
        )
        val dialRadius = strokeWidth * 0.35f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0284C7), Color(0xFF0F172A)),
                center = dialPos,
                radius = dialRadius
            ),
            radius = dialRadius,
            center = dialPos
        )
        drawCircle(
            color = Color.White,
            radius = dialRadius * 0.25f,
            center = dialPos
        )
    }
}
