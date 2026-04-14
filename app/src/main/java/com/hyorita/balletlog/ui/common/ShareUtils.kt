package com.hyorita.balletlog.ui.common

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.exifinterface.media.ExifInterface
import androidx.core.content.FileProvider
import com.hyorita.balletlog.data.PhotoManager
import com.hyorita.balletlog.data.model.ClassLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("No Activity found")
}

private fun loadCorrectlyOrientedBitmap(filePath: String): Bitmap? {
    val bitmap = BitmapFactory.decodeFile(filePath) ?: return null
    val exif = ExifInterface(filePath)
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun shareLogCard(context: Context, log: ClassLog, tabIndex: Int) {
    android.os.Handler(android.os.Looper.getMainLooper()).post {
        android.widget.Toast.makeText(
            context,
            "로그 공유하는 중...",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    val d = context.resources.displayMetrics.density
    val widthPx = (390 * d).toInt()
    val pad = 24 * d
    val innerPad = 16 * d
    val cardRadius = 28 * d
    val innerRadius = 14 * d
    val wf = widthPx.toFloat()

    val sectionName = if (tabIndex == 0) "Barre" else "Center"
    val steps = if (tabIndex == 0) log.barreSteps else log.centerSteps
    val displaySteps = steps.filter { it.name.isNotEmpty() }.take(10)
    val workout = log.workout

    val photoFile = log.photos.firstOrNull()?.let {
        PhotoManager.getPhotoFile(context, it.fileName)
    }
    val hasPhoto = photoFile != null && photoFile.exists()

    // --- Pass 1: calculate height ---
    var totalH = 40 * d    // top padding
    totalH += 36 * d       // emoji + date
    totalH += 40 * d       // day of week + gap

    if (hasPhoto) totalH += 220 * d + 16 * d

    if (workout != null) {
        totalH += 120 * d + 16 * d  // workout card height + gap
    }

    totalH += 28 * d  // section label + gap

    if (displaySteps.isNotEmpty()) {
        totalH += 14 * d  // card top padding
        displaySteps.forEachIndexed { i, step ->
            if (i > 0) totalH += 1 * d + 6 * d  // divider + gap
            totalH += 22 * d  // number + name
            if (step.note.isNotEmpty()) totalH += 18 * d
            totalH += 10 * d
        }
        totalH += 10 * d  // card bottom padding
        totalH += 16 * d  // gap after card
    }

    totalH += 60 * d  // watermark

    val heightPx = totalH.toInt()

    // --- Pass 2: draw ---
    val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val hf = heightPx.toFloat()

    // Paints
    val bgPaint = Paint().apply { color = 0xFFF2F2F7.toInt(); isAntiAlias = true }
    val whitePaint = Paint().apply { color = android.graphics.Color.WHITE; isAntiAlias = true }
    val grayText = Paint().apply { color = 0xFF8E8E93.toInt(); isAntiAlias = true }
    val dividerPaint = Paint().apply { color = 0xFFE5E5EA.toInt(); strokeWidth = 1 * d }

    // White background (outside rounded area)
    canvas.drawRect(0f, 0f, wf, hf, whitePaint)

    // Round clip + card background
    val cardPath = Path()
    cardPath.addRoundRect(RectF(0f, 0f, wf, hf), cardRadius, cardRadius, Path.Direction.CW)
    canvas.clipPath(cardPath)
    canvas.drawRect(0f, 0f, wf, hf, bgPaint)

    var y = 40 * d

    // --- Header: emoji + date ---
    val emojiPaint = Paint().apply { textSize = 24 * d; isAntiAlias = true }
    canvas.drawText("🩰", pad, y + 28 * d, emojiPaint)

    val datePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 24 * d
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(log.date))
    canvas.drawText(dateStr, pad + 36 * d, y + 28 * d, datePaint)
    y += 36 * d

    // Day of week
    grayText.textSize = 13 * d
    val dayStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(log.date))
    canvas.drawText(dayStr, pad, y + 16 * d, grayText)
    y += 40 * d

    // --- Photo ---
    if (hasPhoto && photoFile != null) {
        val photoBitmap = loadCorrectlyOrientedBitmap(photoFile.absolutePath)
        if (photoBitmap != null) {
            val photoW = wf - pad * 2
            val photoH = 220 * d
            val dstRect = RectF(pad, y, pad + photoW, y + photoH)
            val radius = 16 * d

            val photoRatio = photoBitmap.width.toFloat() / photoBitmap.height.toFloat()
            val dstRatio = photoW / photoH
            val srcRect = if (photoRatio > dstRatio) {
                val cropW = (photoBitmap.height * dstRatio).toInt()
                val cropL = (photoBitmap.width - cropW) / 2
                android.graphics.Rect(cropL, 0, cropL + cropW, photoBitmap.height)
            } else {
                val cropH = (photoBitmap.width / dstRatio).toInt()
                val cropT = (photoBitmap.height - cropH) / 2
                android.graphics.Rect(0, cropT, photoBitmap.width, cropT + cropH)
            }

            canvas.save()
            val clipP = Path().apply { addRoundRect(dstRect, radius, radius, Path.Direction.CW) }
            canvas.clipPath(clipP)
            canvas.drawBitmap(photoBitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
            photoBitmap.recycle()

            y += photoH + 16 * d
        }
    }

    // --- Workout card ---
    if (workout != null) {
        val cardH = 120 * d
        val cardRect = RectF(pad, y, wf - pad, y + cardH)
        canvas.drawRoundRect(cardRect, innerRadius, innerRadius, whitePaint)

        val cx = pad + innerPad

        // Title: 🩰 Barre Workout
        val titleEmoji = Paint().apply { textSize = 16 * d; isAntiAlias = true }
        canvas.drawText("🩰", cx, y + 26 * d, titleEmoji)
        val titlePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 16 * d
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("$sectionName Workout", cx + 24 * d, y + 26 * d, titlePaint)

        // 4 stats row
        val statY = y + 52 * d
        val statIconPaint = Paint().apply { textSize = 16 * d; isAntiAlias = true }
        val statValuePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 16 * d
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val statLabelPaint = Paint().apply {
            color = 0xFF8E8E93.toInt()
            textSize = 10 * d
            isAntiAlias = true
        }

        val durStr = if (workout.durationMinutes >= 60)
            "${workout.durationMinutes / 60}h ${workout.durationMinutes % 60}m"
        else "${workout.durationMinutes}m"

        data class Stat(val icon: String, val value: String, val label: String)
        val stats = listOf(
            Stat("⏱", durStr, "Duration"),
            Stat("🔥", "${workout.activeCalories}", "Active Cal"),
            Stat("🩷", "${workout.avgHeartRate}", "Avg BPM"),
            Stat("🩷", "${workout.maxHeartRate}", "Max BPM")
        )

        val colW = (wf - pad * 2 - innerPad * 2) / stats.size
        stats.forEachIndexed { i, stat ->
            val sx = cx + i * colW
            canvas.drawText(stat.icon, sx, statY, statIconPaint)
            canvas.drawText(stat.value, sx + 22 * d, statY, statValuePaint)
            canvas.drawText(stat.label, sx, statY + 20 * d, statLabelPaint)
        }

        y += cardH + 16 * d
    }

    // --- Section label ---
    val labelPaint = Paint().apply {
        color = 0xFF8E8E93.toInt()
        textSize = 11 * d
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    val sectionLabel = if (tabIndex == 0) "BARRE" else "CENTER"
    canvas.drawText(sectionLabel, pad, y + 14 * d, labelPaint)
    y += 28 * d

    // --- Steps card ---
    if (displaySteps.isNotEmpty()) {
        // Pre-calculate card height
        var stepCardH = 14 * d
        displaySteps.forEachIndexed { i, step ->
            if (i > 0) stepCardH += 1 * d + 6 * d
            stepCardH += 22 * d
            if (step.note.isNotEmpty()) stepCardH += 18 * d
            stepCardH += 10 * d
        }
        stepCardH += 10 * d

        val stepRect = RectF(pad, y, wf - pad, y + stepCardH)
        canvas.drawRoundRect(stepRect, innerRadius, innerRadius, whitePaint)

        val numPaint = Paint().apply {
            color = 0xFFAEAEB2.toInt()
            textSize = 13 * d
            isAntiAlias = true
        }
        val namePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 15 * d
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val notePaint = Paint().apply {
            color = 0xFF8E8E93.toInt()
            textSize = 12 * d
            isAntiAlias = true
        }

        var sy = y + 14 * d
        displaySteps.forEachIndexed { i, step ->
            if (i > 0) {
                canvas.drawLine(pad + innerPad, sy, wf - pad - innerPad, sy, dividerPaint)
                sy += 1 * d + 6 * d
            }
            val numStr = String.format("%02d", i + 1)
            canvas.drawText(numStr, pad + innerPad, sy + 16 * d, numPaint)
            canvas.drawText(step.name, pad + innerPad + 30 * d, sy + 16 * d, namePaint)
            sy += 22 * d
            if (step.note.isNotEmpty()) {
                canvas.drawText(step.note, pad + innerPad + 30 * d, sy + 12 * d, notePaint)
                sy += 18 * d
            }
            sy += 10 * d
        }

        y += stepCardH + 16 * d
    }

    // --- Watermark ---
    val wmPaint = Paint().apply {
        color = 0xFFAEAEB2.toInt()
        textSize = 11 * d
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("Ballet Log", wf / 2, hf - 36 * d, wmPaint)
    val wmSubPaint = Paint().apply {
        color = 0xFFC7C7CC.toInt()
        textSize = 10 * d
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("hyorita", wf / 2, hf - 20 * d, wmSubPaint)

    // --- Save & share ---
    val file = File(context.cacheDir, "ballet_log_share.png")
    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "Ballet Log", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Ballet Log"))
}

fun shareStatsCard(
    context: Context,
    period: String,
    periodLabel: String,
    totalClasses: Int,
    totalMinutes: Double,
    totalCalories: Double,
    hardestClass: ClassLog?,
    cubeData: List<Pair<String, Int>>,
    isCubeChart: Boolean,
    chartTitle: String = "",
    monthlyAvgMinutes: List<Double> = emptyList()
) {
    val d = context.resources.displayMetrics.density
    val widthPx = (390 * d).toInt()
    val pad = 20 * d
    val wf = widthPx.toFloat()
    val cardRadius = 28 * d
    val innerRadius = 14 * d

    val headerH = 120 * d
    val metricsH = 200 * d
    val chartTitleH = if (chartTitle.isNotEmpty()) 24 * d else 0f
    val chartH = if (cubeData.isNotEmpty()) {
        if (isCubeChart) {
            val maxCount = cubeData.maxOf { it.second }.coerceIn(1, 5)
            val cubeSize = 24 * d
            val cubeGap = 4 * d
            chartTitleH + 16 * d + maxCount * (cubeSize + cubeGap) + 24 * d
        } else {
            val barChartH = chartTitleH + 80 * d + 24 * d
            val lineChartH = if (monthlyAvgMinutes.isNotEmpty()) 24 * d + 80 * d + 24 * d else 0f
            barChartH + lineChartH
        }
    } else 0f
    val footerH = 60 * d
    val totalH = (headerH + metricsH + chartH + footerH).toInt()

    val bmp = Bitmap.createBitmap(widthPx, totalH, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val hf = totalH.toFloat()

    val whitePaint = Paint().apply { color = android.graphics.Color.WHITE; isAntiAlias = true }
    val bgPaint = Paint().apply { color = 0xFFF2F2F7.toInt(); isAntiAlias = true }

    canvas.drawRect(0f, 0f, wf, hf, whitePaint)
    val cardPath = Path()
    cardPath.addRoundRect(RectF(0f, 0f, wf, hf), cardRadius, cardRadius, Path.Direction.CW)
    canvas.clipPath(cardPath)
    canvas.drawRect(0f, 0f, wf, hf, bgPaint)

    var y = pad

    // Header: 🩰 Ballet Log + period
    val emojiPaint = Paint().apply { textSize = 22 * d; isAntiAlias = true }
    canvas.drawText("🩰", pad, y + 28 * d, emojiPaint)
    val titlePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 22 * d
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    canvas.drawText("Ballet Log", pad + 34 * d, y + 28 * d, titlePaint)
    y += 36 * d

    val periodPaint = Paint().apply {
        color = 0xFF8E8E93.toInt()
        textSize = 13 * d
        isAntiAlias = true
    }
    canvas.drawText(periodLabel, pad, y + 18 * d, periodPaint)
    y += 36 * d

    val dividerPaint = Paint().apply { color = 0xFFE5E5EA.toInt(); strokeWidth = 1 * d }
    canvas.drawLine(pad, y, wf - pad, y, dividerPaint)
    y += 20 * d

    // Metrics 2x2
    val metricW = (wf - pad * 3) / 2
    val metricH = 80 * d

    val durStr = if (totalMinutes >= 60) String.format("%.1fh", totalMinutes / 60)
    else String.format("%.0fm", totalMinutes)
    val hardestVal = hardestClass?.workout?.activeCalories?.let { "$it" } ?: "—"
    val hardestSub = hardestClass?.let {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(it.date))
    }

    data class Metric(val icon: String, val value: String, val label: String, val sub: String? = null)
    val metrics = listOf(
        Metric("📚", "$totalClasses", "Classes"),
        Metric("⏱", durStr, "Workout Time"),
        Metric("🔥", String.format("%,.0f", totalCalories), "kcal"),
        Metric("🏆", hardestVal, "Hardest Class", hardestSub)
    )

    val mIconPaint = Paint().apply { textSize = 16 * d; isAntiAlias = true }
    val mValuePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 20 * d
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    val mLabelPaint = Paint().apply {
        color = 0xFF8E8E93.toInt()
        textSize = 11 * d
        isAntiAlias = true
    }
    val mSubPaint = Paint().apply {
        color = 0xFFAEAEB2.toInt()
        textSize = 10 * d
        isAntiAlias = true
    }
    val metricBgPaint = Paint().apply { color = android.graphics.Color.WHITE; isAntiAlias = true }

    metrics.forEachIndexed { i, m ->
        val col = i % 2
        val row = i / 2
        val mx = pad + col * (metricW + pad)
        val my = y + row * (metricH + 8 * d)
        val rect = RectF(mx, my, mx + metricW, my + metricH)
        canvas.drawRoundRect(rect, innerRadius, innerRadius, metricBgPaint)
        canvas.drawText(m.icon, mx + 12 * d, my + 22 * d, mIconPaint)
        canvas.drawText(m.value, mx + 12 * d, my + 48 * d, mValuePaint)
        canvas.drawText(m.label, mx + 12 * d, my + 66 * d, mLabelPaint)
        m.sub?.let { canvas.drawText(it, mx + 12 * d, my + 76 * d, mSubPaint) }
    }
    y += metricH * 2 + 8 * d + 20 * d

    // Chart
    if (cubeData.isNotEmpty()) {
        // Chart title
        if (chartTitle.isNotEmpty()) {
            val ctPaint = Paint().apply {
                color = 0xFF8E8E93.toInt()
                textSize = 12 * d
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            canvas.drawText(chartTitle, pad, y + 14 * d, ctPaint)
            y += 24 * d
        }

        if (isCubeChart) {
            // Week/Month: cube chart
            val cubeSize = 24 * d
            val cubeGap = 4 * d
            val maxCount = cubeData.maxOf { it.second }.coerceIn(1, 5)
            val colWidth = (wf - pad * 2) / cubeData.size

            val pinkColors = intArrayOf(
                0xFFFFD8DA.toInt(),
                0xFFF5B4BA.toInt(),
                0xFFDC828C.toInt()
            )
            val emptyColor = 0xFFE5E5EA.toInt()

            val numPaint = Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 10 * d
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val cubeLabelPaint = Paint().apply {
                color = 0xFF8E8E93.toInt()
                textSize = 10 * d
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            cubeData.forEachIndexed { ci, (label, count) ->
                val cx = pad + ci * colWidth + (colWidth - cubeSize) / 2
                val centerX = cx + cubeSize / 2

                if (count > 0) {
                    canvas.drawText("$count", centerX, y + 12 * d, numPaint)
                }

                for (slot in 0 until maxCount) {
                    val cubeY = y + 16 * d + (maxCount - 1 - slot) * (cubeSize + cubeGap)
                    val color = when {
                        slot >= count -> emptyColor
                        slot == 0 -> pinkColors[0]
                        slot == 1 -> pinkColors[1]
                        else -> pinkColors[2]
                    }
                    val cubePaint = Paint().apply { this.color = color; isAntiAlias = true }
                    canvas.drawRoundRect(
                        RectF(cx, cubeY, cx + cubeSize, cubeY + cubeSize),
                        4 * d, 4 * d, cubePaint
                    )
                }

                val labelY = y + 16 * d + maxCount * (cubeSize + cubeGap) + 14 * d
                canvas.drawText(label, centerX, labelY, cubeLabelPaint)
            }
        } else {
            // Year: bar chart
            val barWidth = (wf - pad * 2) / cubeData.size
            val maxCount = cubeData.maxOf { it.second }.coerceAtLeast(1)
            val barMaxH = 80 * d
            val barPaint = Paint().apply {
                color = 0xFFDC828C.toInt()
                isAntiAlias = true
            }
            val barLabelPaint = Paint().apply {
                color = 0xFF8E8E93.toInt()
                textSize = 9 * d
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            cubeData.forEachIndexed { i, (label, count) ->
                val bx = pad + i * barWidth
                val barH = (count.toFloat() / maxCount) * barMaxH
                if (barH > 0f) {
                    val rectF = RectF(
                        bx + 2 * d, y + barMaxH - barH,
                        bx + barWidth - 2 * d, y + barMaxH
                    )
                    canvas.drawRoundRect(rectF, 4 * d, 4 * d, barPaint)
                }
                canvas.drawText(label, bx + barWidth / 2, y + barMaxH + 14 * d, barLabelPaint)
            }

            y += barMaxH + 24 * d

            // Line chart: Avg workout time per month
            if (monthlyAvgMinutes.isNotEmpty()) {
                val lineTitle = Paint().apply {
                    color = 0xFF8E8E93.toInt()
                    textSize = 12 * d
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                canvas.drawText("Avg workout time per month (min)", pad, y + 14 * d, lineTitle)
                y += 24 * d

                val lineMaxH = 80 * d
                val maxMin = monthlyAvgMinutes.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                val pointSpacing = (wf - pad * 2) / (monthlyAvgMinutes.size - 1).coerceAtLeast(1)

                val linePaint2 = Paint().apply {
                    color = 0xFFDC828C.toInt()
                    strokeWidth = 2 * d
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                }
                val dotPaint = Paint().apply {
                    color = 0xFFDC828C.toInt()
                    isAntiAlias = true
                }

                val path = Path()
                monthlyAvgMinutes.forEachIndexed { i, mins ->
                    val px = pad + i * pointSpacing
                    val py = y + lineMaxH - (mins / maxMin * lineMaxH).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                canvas.drawPath(path, linePaint2)

                monthlyAvgMinutes.forEachIndexed { i, mins ->
                    val px = pad + i * pointSpacing
                    val py = y + lineMaxH - (mins / maxMin * lineMaxH).toFloat()
                    canvas.drawCircle(px, py, 3 * d, dotPaint)
                }
            }
        }
    }

    // Watermark
    val wmPaint = Paint().apply {
        color = 0xFFAEAEB2.toInt()
        textSize = 11 * d
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("Ballet Log • hyorita", wf / 2, hf - 16 * d, wmPaint)

    // Save & share
    val file = File(context.cacheDir, "ballet_stats_share.png")
    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "Ballet Log Stats", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Ballet Log Stats"))
}
