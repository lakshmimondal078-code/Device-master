package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

@Composable
fun DeviceMasterLogo(
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
            drawDeviceMasterBadge(this)
        }

        if (showText) {
            Spacer(modifier = Modifier.height(10.dp))
            DeviceMasterBrandText(textColor = textColor)
        }
    }
}

@Composable
fun DeviceMasterBrandText(
    modifier: Modifier = Modifier,
    fontSize: Int = 22,
    textColor: Color = Color.White
) {
    val annotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.ExtraBold)) {
            append("Device ")
        }
        withStyle(SpanStyle(color = Color(0xFF00B4D8), fontWeight = FontWeight.ExtraBold)) {
            append("M")
        }
        withStyle(SpanStyle(color = Color(0xFFF97316), fontWeight = FontWeight.ExtraBold)) {
            append("aster")
        }
    }

    Text(
        text = annotatedString,
        fontSize = fontSize.sp,
        letterSpacing = 0.5.sp,
        modifier = modifier
    )
}

private fun drawDeviceMasterBadge(scope: DrawScope) {
    with(scope) {
        val w = size.width
        val h = size.height

        // 1. Base 'D' outer chassis (Deep blue / cyan metallic frame)
        val dLeft = w * 0.12f
        val dTop = h * 0.14f
        val dWidth = w * 0.44f
        val dHeight = h * 0.72f
        val dCorner = dHeight * 0.35f

        // Shadow behind 'D'
        drawRoundRect(
            color = Color(0x35000000),
            topLeft = Offset(dLeft - w * 0.02f, dTop + h * 0.03f),
            size = Size(dWidth + w * 0.04f, dHeight + h * 0.02f),
            cornerRadius = CornerRadius(dCorner, dCorner)
        )

        // Outer Metallic D frame
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color(0xFF1E3A8A), Color(0xFF0284C7), Color(0xFF0A192F)),
                start = Offset(dLeft, dTop),
                end = Offset(dLeft + dWidth, dTop + dHeight)
            ),
            topLeft = Offset(dLeft, dTop),
            size = Size(dWidth, dHeight),
            cornerRadius = CornerRadius(dCorner, dCorner)
        )

        // Metallic bevel highlight
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color(0x9038BDF8), Color(0x200284C7)),
                start = Offset(dLeft, dTop),
                end = Offset(dLeft + dWidth, dTop + dHeight)
            ),
            topLeft = Offset(dLeft, dTop),
            size = Size(dWidth, dHeight),
            cornerRadius = CornerRadius(dCorner, dCorner),
            style = Stroke(width = w * 0.025f)
        )

        // Inner hollow of 'D'
        val innerDLeft = dLeft + dWidth * 0.35f
        val innerDTop = dTop + dHeight * 0.22f
        val innerDWidth = dWidth * 0.5f
        val innerDHeight = dHeight * 0.56f
        val innerDCorner = innerDHeight * 0.35f

        drawRoundRect(
            color = Color(0xFF070E1A),
            topLeft = Offset(innerDLeft, innerDTop),
            size = Size(innerDWidth, innerDHeight),
            cornerRadius = CornerRadius(innerDCorner, innerDCorner)
        )

        // Slider track on left side of 'D'
        val faderX = dLeft + dWidth * 0.16f
        val faderTop = dTop + dHeight * 0.25f
        val faderBottom = dTop + dHeight * 0.75f
        drawLine(
            color = Color(0xFF0369A1),
            start = Offset(faderX, faderTop),
            end = Offset(faderX, faderBottom),
            strokeWidth = w * 0.02f,
            cap = StrokeCap.Round
        )

        // Tick marks on 'D' fader
        for (i in 0..4) {
            val y = faderTop + (faderBottom - faderTop) * (i / 4f)
            drawLine(
                color = Color(0x6038BDF8),
                start = Offset(faderX - w * 0.035f, y),
                end = Offset(faderX - w * 0.015f, y),
                strokeWidth = w * 0.008f
            )
        }

        // Fader metallic knob on left track
        val knobY = faderTop + (faderBottom - faderTop) * 0.45f
        drawCircle(
            color = Color(0xFFE2E8F0),
            radius = w * 0.04f,
            center = Offset(faderX, knobY)
        )
        drawCircle(
            color = Color(0xFF0284C7),
            radius = w * 0.02f,
            center = Offset(faderX, knobY)
        )

        // LED indicators on 'D'
        drawCircle(color = Color(0xFFF59E0B), radius = w * 0.018f, center = Offset(dLeft + dWidth * 0.16f, dTop + dHeight * 0.15f))
        drawCircle(color = Color(0xFF38BDF8), radius = w * 0.015f, center = Offset(dLeft + dWidth * 0.32f, dTop + dHeight * 0.88f))

        // 2. Geometric 'M' shape (Right side, Cyan + Golden Amber)
        val mPath = Path().apply {
            val mLeft = w * 0.45f
            val mRight = w * 0.88f
            val mTopPeak1 = h * 0.25f
            val mTopPeak2 = h * 0.18f
            val mCenterV = h * 0.58f
            val mBottom = h * 0.86f

            moveTo(mLeft, mCenterV)
            lineTo(w * 0.58f, mTopPeak1)
            lineTo(w * 0.70f, mCenterV * 0.8f)
            lineTo(w * 0.76f, mTopPeak2)
            lineTo(mRight, mCenterV)
            lineTo(mRight, mBottom)
            lineTo(w * 0.76f, mBottom)
            lineTo(w * 0.76f, mCenterV * 0.95f)
            lineTo(w * 0.70f, mBottom * 0.75f)
            lineTo(w * 0.58f, mCenterV * 0.95f)
            lineTo(w * 0.58f, mBottom)
            lineTo(mLeft, mBottom)
            close()
        }

        // Draw 'M' with glowing cyan and amber gradient
        drawPath(
            path = mPath,
            brush = Brush.linearGradient(
                listOf(Color(0xFF00B4D8), Color(0xFF0284C7), Color(0xFFF97316)),
                start = Offset(w * 0.45f, h * 0.2f),
                end = Offset(w * 0.88f, h * 0.86f)
            )
        )

        // 'M' metallic stroke highlight
        drawPath(
            path = mPath,
            brush = Brush.linearGradient(
                listOf(Color(0xFF7DD3FC), Color(0xFFFDBA74)),
                start = Offset(w * 0.45f, h * 0.2f),
                end = Offset(w * 0.88f, h * 0.86f)
            ),
            style = Stroke(width = w * 0.02f)
        )

        // Fader track on the right leg of 'M'
        val mFaderX = w * 0.82f
        val mFaderTop = h * 0.45f
        val mFaderBottom = h * 0.80f
        drawLine(
            color = Color(0xFF0C4A6E),
            start = Offset(mFaderX, mFaderTop),
            end = Offset(mFaderX, mFaderBottom),
            strokeWidth = w * 0.016f,
            cap = StrokeCap.Round
        )

        // Knob on 'M' leg
        val mKnobY = mFaderTop + (mFaderBottom - mFaderTop) * 0.65f
        drawCircle(
            color = Color(0xFFF1F5F9),
            radius = w * 0.035f,
            center = Offset(mFaderX, mKnobY)
        )
        drawCircle(
            color = Color(0xFFF97316),
            radius = w * 0.018f,
            center = Offset(mFaderX, mKnobY)
        )

        // 3. Central Tactile Control Gear Dial (connecting D and M)
        val gearCenter = Offset(w * 0.48f, h * 0.46f)
        val gearRadius = w * 0.17f

        // Outer Gear Shadow
        drawCircle(
            color = Color(0x50000000),
            radius = gearRadius * 1.15f,
            center = gearCenter + Offset(0f, h * 0.015f)
        )

        // Gear Outer Ring (Dark Blue / Amber border)
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(Color(0xFF0284C7), Color(0xFFF59E0B), Color(0xFF00B4D8), Color(0xFF0284C7)),
                center = gearCenter
            ),
            radius = gearRadius,
            center = gearCenter
        )

        // Gear Teeth Notches
        for (i in 0..7) {
            val angle = Math.toRadians((i * 45).toDouble())
            val tx = gearCenter.x + (gearRadius * Math.cos(angle)).toFloat()
            val ty = gearCenter.y + (gearRadius * Math.sin(angle)).toFloat()
            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = w * 0.025f,
                center = Offset(tx, ty)
            )
        }

        // Inner Gear Core
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF0284C7), Color(0xFF0C4A6E), Color(0xFF031633)),
                center = gearCenter,
                radius = gearRadius * 0.75f
            ),
            radius = gearRadius * 0.75f,
            center = gearCenter
        )

        // Center Chrome Dial Button
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White, Color(0xFF94A3B8), Color(0xFF334155)),
                center = gearCenter - Offset(gearRadius * 0.1f, gearRadius * 0.1f),
                radius = gearRadius * 0.42f
            ),
            radius = gearRadius * 0.42f,
            center = gearCenter
        )

        // Center Indicator Dot
        drawCircle(
            color = Color(0xFF38BDF8),
            radius = gearRadius * 0.15f,
            center = gearCenter
        )
    }
}
