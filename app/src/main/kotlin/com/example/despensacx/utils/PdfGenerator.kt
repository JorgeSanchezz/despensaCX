package com.example.despensacx.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.despensacx.data.ListaEntity
import com.example.despensacx.model.ProductoConTienda
import com.example.despensacx.model.UnidadMedida
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_TOP = 60f
    private const val MARGIN_BOTTOM = 60f
    private const val X_START = 100f
    private const val X_END = 495f
    private const val CENTER_X = 297.5f

    fun generarTicketPDF(
        context: Context,
        lista: ListaEntity,
        productosPorTienda: Map<Long, List<ProductoConTienda>>,
        tiendaIdFiltro: Long? = null
    ): Uri? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        var pageCount = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageCount).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        var y = MARGIN_TOP

        fun checkNewPage(neededSpace: Float) {
            if (y + neededSpace > PAGE_HEIGHT - MARGIN_BOTTOM) {
                pdfDocument.finishPage(page)
                pageCount++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageCount).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN_TOP
                
                // Indicador de página en la nueva hoja
                paint.textSize = 8f
                paint.textAlign = Paint.Align.RIGHT
                paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                canvas.drawText("Pág. $pageCount - ${lista.nombre.uppercase()}", X_END, y, paint)
                y += 20f
            }
        }

        // --- ENCABEZADO ---
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        titlePaint.textSize = 24f
        canvas.drawText("DESPENSA CX", CENTER_X, y, titlePaint)
        
        y += 30f
        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        canvas.drawText("Ticket de Compra", CENTER_X, y, paint)

        y += 20f
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Fecha: $fecha", CENTER_X, y, paint)

        y += 20f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText("Lista: ${lista.nombre.uppercase()}", CENTER_X, y, paint)

        y += 15f
        canvas.drawText("------------------------------------------", CENTER_X, y, paint)
        y += 20f

        // --- CONTENIDO ---
        var granTotal = 0.0

        productosPorTienda.forEach { (tiendaId, prods) ->
            if (tiendaIdFiltro != null && tiendaId != tiendaIdFiltro) return@forEach
            
            val seleccionados = prods.filter { it.producto.seleccionado }
            if (seleccionados.isEmpty()) return@forEach

            val tiendaNombre = prods.firstOrNull()?.tienda?.nombre ?: "General"
            
            checkNewPage(40f)

            // Nombre Tienda
            y += 10f
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("TIENDA: ${tiendaNombre.uppercase()}", X_START, y, paint)
            y += 20f
            
            var subtotalTienda = 0.0
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            paint.textSize = 10f
            
            seleccionados.forEach { pct ->
                val p = pct.producto
                val itemSubtotal = p.precio * p.cantidad
                subtotalTienda += itemSubtotal
                granTotal += itemSubtotal

                checkNewPage(15f)

                val unitLabel = try { UnidadMedida.valueOf(p.unidad).label } catch(e: Exception) { "" }
                val cantStr = if (p.cantidad % 1.0 == 0.0) p.cantidad.toInt().toString() else p.cantidad.toString()
                val lineaDesc = "$cantStr $unitLabel x ${FormatUtils.formatCurrency(p.precio)} ${p.descripcion}"
                val descRecortada = if (lineaDesc.length > 35) lineaDesc.substring(0, 32) + "..." else lineaDesc
                
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(descRecortada, X_START, y, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(FormatUtils.formatCurrency(itemSubtotal), X_END, y, paint)
                
                y += 15f
            }
            
            // Subtotal por tienda
            checkNewPage(20f)
            y += 5f
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("SUBTOTAL ${tiendaNombre.uppercase()}: ${FormatUtils.formatCurrency(subtotalTienda)}", X_END, y, paint)
            
            y += 15f
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            canvas.drawText("------------------------------------------", CENTER_X, y, paint)
            y += 20f
        }

        // --- TOTAL ---
        checkNewPage(60f)
        y += 10f
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("TOTAL A PAGAR:", X_START, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(FormatUtils.formatCurrency(granTotal), X_END, y, paint)

        y += 40f
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC)
        canvas.drawText("¡Gracias por usar DespensaCX!", CENTER_X, y, paint)

        pdfDocument.finishPage(page)

        // Guardar archivo
        val fileName = "Ticket_${lista.nombre.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        
        return try {
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
