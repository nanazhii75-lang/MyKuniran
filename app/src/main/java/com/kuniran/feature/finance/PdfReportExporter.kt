package com.kuniran.feature.finance

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.kuniran.core.common.CurrencyFormatter
import com.kuniran.core.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportExporter {

    fun generateAndSharePdf(
        context: Context,
        rtName: String,
        uiState: FinanceRecordUiState
    ): Result<File> {
        return runCatching {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 standard width in points
            val pageHeight = 842 // A4 standard height in points
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
            val currentDate = dateFormat.format(Date())

            var currentY = 40f

            // 1. Header Banner
            paint.color = Color.rgb(27, 94, 32) // Forest green
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 18f
            canvas.drawText("LAPORAN KAS & TRANSPARANSI RT", 30f, 42f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 11f
            canvas.drawText("Wilayah: $rtName • Diterbitkan: $currentDate", 30f, 64f, paint)

            currentY = 115f

            // 2. Summary Financial Box
            paint.color = Color.rgb(240, 245, 240)
            canvas.drawRoundRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 70f, 8f, 8f, paint)

            paint.color = Color.rgb(27, 94, 32)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("SALDO BERSIH KAS:", 46f, currentY + 26f, paint)

            paint.textSize = 16f
            canvas.drawText(CurrencyFormatter.formatRupiah(uiState.netBalance), 46f, currentY + 50f, paint)

            paint.color = Color.DKGRAY
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Total Pemasukan: ${CurrencyFormatter.formatRupiah(uiState.totalIncome)}", 300f, currentY + 28f, paint)
            canvas.drawText("Total Pengeluaran: ${CurrencyFormatter.formatRupiah(uiState.totalExpense)}", 300f, currentY + 48f, paint)

            currentY += 95f

            // 3. Monthly Trends Summary
            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            canvas.drawText("Ringkasan Tren Kas Bulanan", 30f, currentY, paint)
            currentY += 16f

            paint.color = Color.rgb(220, 220, 220)
            canvas.drawLine(30f, currentY, (pageWidth - 30).toFloat(), currentY, paint)
            currentY += 16f

            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            if (uiState.monthlyTrends.isEmpty()) {
                paint.color = Color.GRAY
                canvas.drawText("Belum ada tren riwayat bulanan tercatat.", 30f, currentY, paint)
                currentY += 20f
            } else {
                uiState.monthlyTrends.take(5).forEach { trend ->
                    paint.color = Color.BLACK
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("Bulan ${trend.monthLabel}:", 30f, currentY, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.color = Color.rgb(46, 125, 50)
                    canvas.drawText("Masuk: +${CurrencyFormatter.formatRupiah(trend.income)}", 120f, currentY, paint)

                    paint.color = Color.rgb(198, 40, 40)
                    canvas.drawText("Keluar: -${CurrencyFormatter.formatRupiah(trend.expense)}", 270f, currentY, paint)

                    paint.color = Color.BLACK
                    canvas.drawText("Bersih: ${CurrencyFormatter.formatRupiah(trend.net)}", 420f, currentY, paint)

                    currentY += 16f
                }
            }

            currentY += 20f

            // 4. Categorized Transactions Header
            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            canvas.drawText("Daftar Transaksi Kas RT", 30f, currentY, paint)
            currentY += 16f

            paint.color = Color.rgb(220, 220, 220)
            canvas.drawLine(30f, currentY, (pageWidth - 30).toFloat(), currentY, paint)
            currentY += 14f

            // Table Header
            paint.color = Color.DKGRAY
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("POS / KATEGORI", 30f, currentY, paint)
            canvas.drawText("URAIAN TRANSAKSI", 140f, currentY, paint)
            canvas.drawText("JENIS", 350f, currentY, paint)
            canvas.drawText("JUMLAH (RP)", 450f, currentY, paint)
            currentY += 14f

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            // Render transaction rows (limited to fit single page elegantly)
            val displayRecords = uiState.records.take(20)
            displayRecords.forEach { record ->
                if (currentY > pageHeight - 60) return@forEach

                val isIncome = record.type == TransactionType.MASUK
                paint.color = Color.BLACK
                canvas.drawText(record.categoryName.take(18), 30f, currentY, paint)
                canvas.drawText(record.title.take(30), 140f, currentY, paint)

                if (isIncome) {
                    paint.color = Color.rgb(46, 125, 50)
                    canvas.drawText("MASUK", 350f, currentY, paint)
                    canvas.drawText("+${CurrencyFormatter.formatRupiah(record.amount)}", 450f, currentY, paint)
                } else {
                    paint.color = Color.rgb(198, 40, 40)
                    canvas.drawText("KELUAR", 350f, currentY, paint)
                    canvas.drawText("-${CurrencyFormatter.formatRupiah(record.amount)}", 450f, currentY, paint)
                }

                currentY += 14f
            }

            // 5. Official Footer
            paint.color = Color.GRAY
            paint.textSize = 8f
            canvas.drawLine(30f, (pageHeight - 40).toFloat(), (pageWidth - 30).toFloat(), (pageHeight - 40).toFloat(), paint)
            canvas.drawText("Dokumen digital resmi sistem myKuniran • Transparansi Keuangan Warga Rukun Tetangga", 30f, (pageHeight - 25).toFloat(), paint)

            pdfDocument.finishPage(page)

            // Save PDF to cache directory
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val outputFile = File(reportsDir, "Laporan_Kas_RT_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            // Share / Export via Intent
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Kas RT: $rtName")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan Laporan Kas RT").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            outputFile
        }
    }
}
