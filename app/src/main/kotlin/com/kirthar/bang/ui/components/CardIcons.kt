package com.kirthar.bang.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.kirthar.bang.core.model.CardKind
import kotlin.math.cos
import kotlin.math.sin

/**
 * Iconografía western 100% original dibujada con primitivas de Compose Canvas
 * (círculos, arcos, rectángulos redondeados y trazados). Ningún icono reproduce
 * el arte del juego de mesa oficial; son pictogramas propios pensados para
 * leerse bien entre 60dp y 120dp.
 */

private fun pointOnCircle(center: Offset, radius: Float, angleDegrees: Float): Offset {
    val rad = Math.toRadians(angleDegrees.toDouble())
    return Offset(
        x = center.x + (cos(rad) * radius).toFloat(),
        y = center.y + (sin(rad) * radius).toFloat(),
    )
}

/** Dibuja el icono central correspondiente al tipo de carta. */
fun DrawScope.drawCardKindIcon(kind: CardKind, tint: Color, accent: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val center = Offset(cx, cy)
    val u = size.minDimension

    when (kind) {
        CardKind.BANG -> drawRevolver(tint, accent, center, u, muzzleFlash = true)
        CardKind.MISSED -> drawHorseshoe(tint, center, u)
        CardKind.BEER -> drawBeerMug(tint, accent, center, u)
        CardKind.PANIC -> drawGraspingHand(tint, center, u)
        CardKind.CAT_BALOU -> drawLasso(tint, center, u)
        CardKind.STAGECOACH -> drawWagonWheel(tint, accent, center, u)
        CardKind.WELLS_FARGO -> drawStrongbox(tint, accent, center, u)
        CardKind.GATLING -> drawGatling(tint, accent, center, u)
        CardKind.INDIANS -> drawFeather(tint, accent, center, u)
        CardKind.DUEL -> {
            rotate(25f, pivot = center) { drawRevolver(tint, accent, center, u * 0.8f, muzzleFlash = false) }
            rotate(-155f, pivot = center) { drawRevolver(tint, accent, center, u * 0.8f, muzzleFlash = false) }
        }
        CardKind.GENERAL_STORE -> drawStoreFront(tint, accent, center, u)
        CardKind.SALOON -> drawSaloonDoors(tint, accent, center, u)
        CardKind.JAIL -> drawJailBars(tint, center, u)
        CardKind.DYNAMITE -> drawDynamite(tint, accent, center, u)
        CardKind.BARREL -> drawBarrel(tint, accent, center, u)
        CardKind.MUSTANG -> drawHorseHead(tint, accent, center, u)
        CardKind.SCOPE -> drawScopeCrosshair(tint, accent, center, u)
        CardKind.VOLCANIC -> drawRevolver(tint, accent, center, u * 0.72f, muzzleFlash = false)
        CardKind.SCHOFIELD -> drawRevolver(tint, accent, center, u * 0.85f, muzzleFlash = false)
        CardKind.REMINGTON -> drawRevolver(tint, accent, center, u * 0.98f, muzzleFlash = false)
        CardKind.REV_CARABINE -> drawRifle(tint, accent, center, u, stockLength = 0.30f)
        CardKind.WINCHESTER -> drawRifle(tint, accent, center, u, stockLength = 0.46f)
    }
}

private fun DrawScope.drawRevolver(
    tint: Color,
    accent: Color,
    center: Offset,
    unit: Float,
    muzzleFlash: Boolean,
) {
    rotate(-15f, pivot = center) {
        val barrelLen = unit * 0.42f
        val barrelH = unit * 0.11f
        val originX = center.x - unit * 0.22f

        drawRoundRect(
            color = tint,
            topLeft = Offset(originX, center.y - barrelH / 2f),
            size = Size(barrelLen, barrelH),
            cornerRadius = CornerRadius(barrelH / 2f),
        )
        drawCircle(color = tint, radius = unit * 0.09f, center = Offset(originX + unit * 0.05f, center.y))

        val gripPath = Path().apply {
            moveTo(originX - unit * 0.01f, center.y + barrelH * 0.3f)
            lineTo(originX - unit * 0.15f, center.y + unit * 0.30f)
            lineTo(originX - unit * 0.02f, center.y + unit * 0.34f)
            lineTo(originX + unit * 0.09f, center.y + barrelH * 0.5f)
            close()
        }
        drawPath(gripPath, color = tint)

        drawArc(
            color = tint,
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            style = Stroke(width = unit * 0.03f, cap = StrokeCap.Round),
            topLeft = Offset(originX - unit * 0.01f, center.y + unit * 0.01f),
            size = Size(unit * 0.14f, unit * 0.14f),
        )

        if (muzzleFlash) {
            val tipX = originX + barrelLen
            for (i in 0 until 4) {
                val angle = -35f + i * 22f
                val rad = Math.toRadians(angle.toDouble())
                val dx = (cos(rad) * unit * 0.14f).toFloat()
                val dy = (sin(rad) * unit * 0.14f).toFloat()
                drawLine(
                    color = accent,
                    start = Offset(tipX, center.y),
                    end = Offset(tipX + dx, center.y + dy),
                    strokeWidth = unit * 0.025f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun DrawScope.drawRifle(tint: Color, accent: Color, center: Offset, unit: Float, stockLength: Float) {
    rotate(-10f, pivot = center) {
        val barrelLen = unit * 0.62f
        val barrelH = unit * 0.08f
        val originX = center.x - unit * 0.30f

        drawRoundRect(
            color = tint,
            topLeft = Offset(originX, center.y - barrelH / 2f),
            size = Size(barrelLen, barrelH),
            cornerRadius = CornerRadius(barrelH / 2f),
        )

        val stockPath = Path().apply {
            moveTo(originX + unit * 0.03f, center.y - barrelH / 2f)
            lineTo(originX - unit * stockLength, center.y + unit * 0.16f)
            lineTo(originX - unit * stockLength * 0.65f, center.y + unit * 0.20f)
            lineTo(originX + unit * 0.03f, center.y + barrelH / 2f)
            close()
        }
        drawPath(stockPath, color = tint)

        drawCircle(color = accent, radius = unit * 0.022f, center = Offset(originX + barrelLen, center.y))
    }
}

private fun DrawScope.drawHorseshoe(tint: Color, center: Offset, unit: Float) {
    val radius = unit * 0.28f
    drawArc(
        color = tint,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        style = Stroke(width = unit * 0.09f, cap = StrokeCap.Round),
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
    )
    for (angle in listOf(205f, 250f, 290f, 335f)) {
        val p = pointOnCircle(center, radius, angle)
        drawCircle(color = tint, radius = unit * 0.02f, center = p)
    }
}

private fun DrawScope.drawBeerMug(tint: Color, accent: Color, center: Offset, unit: Float) {
    val bodyW = unit * 0.30f
    val bodyH = unit * 0.40f
    val left = center.x - bodyW / 2f - unit * 0.03f
    val top = center.y - bodyH / 2f

    drawRoundRect(
        color = tint,
        topLeft = Offset(left, top),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(unit * 0.03f),
    )
    drawArc(
        color = tint,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        style = Stroke(width = unit * 0.05f),
        topLeft = Offset(left + bodyW - unit * 0.03f, center.y - bodyH * 0.26f),
        size = Size(unit * 0.16f, unit * 0.46f),
    )
    drawRoundRect(
        color = accent,
        topLeft = Offset(left - unit * 0.015f, top - unit * 0.05f),
        size = Size(bodyW + unit * 0.03f, unit * 0.08f),
        cornerRadius = CornerRadius(unit * 0.04f),
    )
}

private fun DrawScope.drawGraspingHand(tint: Color, center: Offset, unit: Float) {
    val palmW = unit * 0.30f
    val palmH = unit * 0.22f
    val palmTop = center.y - unit * 0.02f

    drawRoundRect(
        color = tint,
        topLeft = Offset(center.x - palmW / 2f, palmTop),
        size = Size(palmW, palmH),
        cornerRadius = CornerRadius(unit * 0.05f),
    )
    val fingerW = palmW / 4.6f
    for (i in 0 until 4) {
        val fx = center.x - palmW / 2f + i * (fingerW * 1.15f) + unit * 0.01f
        val fingerH = unit * 0.16f - (if (i == 0 || i == 3) unit * 0.02f else 0f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(fx, palmTop - fingerH + unit * 0.03f),
            size = Size(fingerW, fingerH),
            cornerRadius = CornerRadius(fingerW / 2f),
        )
    }
    rotate(-35f, pivot = Offset(center.x - palmW / 2f, palmTop + unit * 0.06f)) {
        drawRoundRect(
            color = tint,
            topLeft = Offset(center.x - palmW / 2f - unit * 0.13f, palmTop + unit * 0.02f),
            size = Size(unit * 0.15f, unit * 0.07f),
            cornerRadius = CornerRadius(unit * 0.035f),
        )
    }
}

private fun DrawScope.drawLasso(tint: Color, center: Offset, unit: Float) {
    drawCircle(
        color = tint,
        radius = unit * 0.22f,
        center = Offset(center.x, center.y - unit * 0.06f),
        style = Stroke(width = unit * 0.045f),
    )
    drawLine(
        color = tint,
        start = Offset(center.x + unit * 0.17f, center.y + unit * 0.10f),
        end = Offset(center.x + unit * 0.30f, center.y + unit * 0.34f),
        strokeWidth = unit * 0.045f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawWagonWheel(tint: Color, accent: Color, center: Offset, unit: Float) {
    val r = unit * 0.26f
    drawCircle(color = tint, radius = r, center = center, style = Stroke(width = unit * 0.035f))
    for (i in 0 until 6) {
        val p = pointOnCircle(center, r, i * 60f)
        drawLine(color = tint, start = center, end = p, strokeWidth = unit * 0.025f)
    }
    drawCircle(color = accent, radius = unit * 0.05f, center = center)
}

private fun DrawScope.drawStrongbox(tint: Color, accent: Color, center: Offset, unit: Float) {
    val w = unit * 0.48f
    val h = unit * 0.34f
    drawRoundRect(
        color = tint,
        topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
        size = Size(w, h),
        cornerRadius = CornerRadius(unit * 0.04f),
    )
    drawLine(accent, Offset(center.x - w / 2f, center.y - h / 6f), Offset(center.x + w / 2f, center.y - h / 6f), strokeWidth = unit * 0.015f)
    drawLine(accent, Offset(center.x - w / 2f, center.y + h / 6f), Offset(center.x + w / 2f, center.y + h / 6f), strokeWidth = unit * 0.015f)
    drawCircle(color = accent, radius = unit * 0.07f, center = center)
    drawCircle(color = tint, radius = unit * 0.02f, center = center)
}

private fun DrawScope.drawGatling(tint: Color, accent: Color, center: Offset, unit: Float) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(center.x - unit * 0.18f, center.y + unit * 0.06f),
        size = Size(unit * 0.36f, unit * 0.12f),
        cornerRadius = CornerRadius(unit * 0.02f),
    )
    val hub = Offset(center.x, center.y - unit * 0.05f)
    for (i in 0 until 6) {
        val p = pointOnCircle(hub, unit * 0.14f, i * 60f)
        drawCircle(color = tint, radius = unit * 0.035f, center = p)
    }
    drawCircle(color = accent, radius = unit * 0.05f, center = hub)
}

private fun DrawScope.drawFeather(tint: Color, accent: Color, center: Offset, unit: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - unit * 0.32f)
        quadraticTo(center.x + unit * 0.17f, center.y - unit * 0.10f, center.x + unit * 0.05f, center.y + unit * 0.30f)
        quadraticTo(center.x, center.y + unit * 0.33f, center.x - unit * 0.05f, center.y + unit * 0.30f)
        quadraticTo(center.x - unit * 0.17f, center.y - unit * 0.10f, center.x, center.y - unit * 0.32f)
        close()
    }
    drawPath(path, color = tint)
    drawLine(accent, Offset(center.x, center.y - unit * 0.28f), Offset(center.x, center.y + unit * 0.26f), strokeWidth = unit * 0.015f)
    for (t in listOf(-0.14f, 0.02f, 0.16f)) {
        val y = center.y + t * unit
        drawLine(accent, Offset(center.x, y), Offset(center.x - unit * 0.10f, y - unit * 0.05f), strokeWidth = unit * 0.012f)
        drawLine(accent, Offset(center.x, y), Offset(center.x + unit * 0.10f, y - unit * 0.05f), strokeWidth = unit * 0.012f)
    }
}

private fun DrawScope.drawStoreFront(tint: Color, accent: Color, center: Offset, unit: Float) {
    val w = unit * 0.42f
    val h = unit * 0.28f
    val top = center.y - unit * 0.02f

    drawRoundRect(color = tint, topLeft = Offset(center.x - w / 2f, top), size = Size(w, h), cornerRadius = CornerRadius(unit * 0.02f))
    val roofPath = Path().apply {
        moveTo(center.x - w / 2f - unit * 0.04f, top)
        lineTo(center.x, top - unit * 0.15f)
        lineTo(center.x + w / 2f + unit * 0.04f, top)
        close()
    }
    drawPath(roofPath, color = accent)
    drawRoundRect(
        color = accent,
        topLeft = Offset(center.x - unit * 0.05f, top + h - unit * 0.14f),
        size = Size(unit * 0.10f, unit * 0.14f),
        cornerRadius = CornerRadius(unit * 0.01f),
    )
    drawRect(
        color = accent,
        topLeft = Offset(center.x - w / 2f + unit * 0.04f, top + unit * 0.04f),
        size = Size(unit * 0.08f, unit * 0.08f),
    )
}

private fun DrawScope.drawSaloonDoors(tint: Color, accent: Color, center: Offset, unit: Float) {
    val doorW = unit * 0.16f
    val doorH = unit * 0.30f
    val gap = unit * 0.04f

    drawRoundRect(
        color = tint,
        topLeft = Offset(center.x - gap / 2f - doorW, center.y - doorH / 2f),
        size = Size(doorW, doorH),
        cornerRadius = CornerRadius(unit * 0.03f),
    )
    drawRoundRect(
        color = tint,
        topLeft = Offset(center.x + gap / 2f, center.y - doorH / 2f),
        size = Size(doorW, doorH),
        cornerRadius = CornerRadius(unit * 0.03f),
    )
    drawLine(
        color = accent,
        start = Offset(center.x - gap / 2f - doorW - unit * 0.02f, center.y - doorH / 2f - unit * 0.04f),
        end = Offset(center.x + gap / 2f + doorW + unit * 0.02f, center.y - doorH / 2f - unit * 0.04f),
        strokeWidth = unit * 0.025f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawJailBars(tint: Color, center: Offset, unit: Float) {
    val w = unit * 0.40f
    val h = unit * 0.44f
    drawRoundRect(
        color = tint,
        topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
        size = Size(w, h),
        style = Stroke(width = unit * 0.035f),
        cornerRadius = CornerRadius(unit * 0.02f),
    )
    for (i in 1..3) {
        val x = center.x - w / 2f + w * i / 4f
        drawLine(
            color = tint,
            start = Offset(x, center.y - h / 2f + unit * 0.02f),
            end = Offset(x, center.y + h / 2f - unit * 0.02f),
            strokeWidth = unit * 0.025f,
        )
    }
}

private fun DrawScope.drawDynamite(tint: Color, accent: Color, center: Offset, unit: Float) {
    val stickW = unit * 0.09f
    val stickH = unit * 0.34f
    val gap = unit * 0.02f
    val startX = center.x - (stickW * 3f + gap * 2f) / 2f
    val top = center.y - stickH / 2f + unit * 0.02f

    for (i in 0 until 3) {
        drawRoundRect(
            color = tint,
            topLeft = Offset(startX + i * (stickW + gap), top),
            size = Size(stickW, stickH),
            cornerRadius = CornerRadius(unit * 0.02f),
        )
    }
    drawRect(
        color = accent,
        topLeft = Offset(startX - unit * 0.01f, center.y - unit * 0.03f),
        size = Size(stickW * 3f + gap * 2f + unit * 0.02f, unit * 0.06f),
    )

    val fuseStart = Offset(startX + stickW * 2.5f, top)
    val fuseTip = Offset(center.x + unit * 0.14f, center.y - unit * 0.38f)
    val fusePath = Path().apply {
        moveTo(fuseStart.x, fuseStart.y)
        quadraticTo(center.x + unit * 0.22f, center.y - unit * 0.22f, fuseTip.x, fuseTip.y)
    }
    drawPath(fusePath, color = accent, style = Stroke(width = unit * 0.02f, cap = StrokeCap.Round))
    for (i in 0 until 4) {
        val p = pointOnCircle(fuseTip, unit * 0.05f, i * 90f + 20f)
        drawLine(accent, fuseTip, p, strokeWidth = unit * 0.012f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawBarrel(tint: Color, accent: Color, center: Offset, unit: Float) {
    val w = unit * 0.32f
    val h = unit * 0.42f
    drawRoundRect(
        color = tint,
        topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
        size = Size(w, h),
        cornerRadius = CornerRadius(w * 0.4f),
    )
    for (t in listOf(-0.24f, 0f, 0.24f)) {
        drawLine(
            color = accent,
            start = Offset(center.x - w / 2f + unit * 0.02f, center.y + t * h),
            end = Offset(center.x + w / 2f - unit * 0.02f, center.y + t * h),
            strokeWidth = unit * 0.02f,
        )
    }
}

private fun DrawScope.drawHorseHead(tint: Color, accent: Color, center: Offset, unit: Float) {
    val path = Path().apply {
        moveTo(center.x - unit * 0.05f, center.y + unit * 0.30f)
        lineTo(center.x - unit * 0.02f, center.y - unit * 0.05f)
        lineTo(center.x - unit * 0.15f, center.y - unit * 0.20f)
        lineTo(center.x - unit * 0.02f, center.y - unit * 0.32f)
        lineTo(center.x + unit * 0.07f, center.y - unit * 0.16f)
        lineTo(center.x + unit * 0.11f, center.y - unit * 0.02f)
        lineTo(center.x + unit * 0.03f, center.y + unit * 0.08f)
        lineTo(center.x + unit * 0.09f, center.y + unit * 0.30f)
        close()
    }
    drawPath(path, color = tint)
    for (i in 0 until 3) {
        val y = center.y - unit * 0.18f + i * unit * 0.10f
        drawLine(
            color = accent,
            start = Offset(center.x - unit * 0.02f, y),
            end = Offset(center.x - unit * 0.11f, y + unit * 0.04f),
            strokeWidth = unit * 0.02f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawScopeCrosshair(tint: Color, accent: Color, center: Offset, unit: Float) {
    val r = unit * 0.24f
    drawCircle(color = tint, radius = r, center = center, style = Stroke(width = unit * 0.03f))
    drawLine(tint, Offset(center.x - r - unit * 0.05f, center.y), Offset(center.x + r + unit * 0.05f, center.y), strokeWidth = unit * 0.02f)
    drawLine(tint, Offset(center.x, center.y - r - unit * 0.05f), Offset(center.x, center.y + r + unit * 0.05f), strokeWidth = unit * 0.02f)
    drawCircle(color = accent, radius = unit * 0.02f, center = center)
}

/**
 * Estrella de N puntas, reutilizada por el dorso de las cartas, la estrella de
 * sheriff de [PlayerBadge] y el icono de la aplicación.
 */
fun DrawScope.drawStarShape(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
    points: Int = 5,
    rotationDegrees: Float = -90f,
) {
    val path = Path()
    val step = 360f / points
    for (i in 0 until points) {
        val outerAngle = rotationDegrees + i * step
        val innerAngle = outerAngle + step / 2f
        val outerPoint = pointOnCircle(center, outerRadius, outerAngle)
        val innerPoint = pointOnCircle(center, innerRadius, innerAngle)
        if (i == 0) path.moveTo(outerPoint.x, outerPoint.y) else path.lineTo(outerPoint.x, outerPoint.y)
        path.lineTo(innerPoint.x, innerPoint.y)
    }
    path.close()
    drawPath(path, color = color)
}
