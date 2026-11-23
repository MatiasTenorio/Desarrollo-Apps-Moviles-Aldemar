package com.duoc.aquasample.data.reporte

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reporte PDF con PdfDocument, que viene incluido en Android (sin librerías).
 * Página A4 vertical (595 x 842 puntos). Se guarda en cacheDir/reportes y se
 * comparte con un FileProvider (ver AndroidManifest y res/xml/file_paths.xml).
 */
class GeneradorReportePdf(private val contexto: Context) : GeneradorReporte {

    private val anchoPagina = 595
    private val altoPagina = 842
    private val margen = 36f

    // Columnas: título y posición x de inicio.
    private val columnas = listOf(
        "ID" to 36f, "Fecha" to 78f, "Centro" to 160f, "Tren" to 262f,
        "Línea" to 306f, "Choritos" to 342f, "Tam. prom." to 392f, "Observaciones" to 450f
    )

    override suspend fun generar(
        filas: List<FilaReporte>,
        generadoPor: String,
        generadoEn: LocalDateTime
    ): File = withContext(Dispatchers.IO) {
        val documento = PdfDocument()
        val titulo = Paint().apply { textSize = 16f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK }
        val normal = Paint().apply { textSize = 9f; color = Color.BLACK }
        val encabezado = Paint().apply { textSize = 9f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK }
        val linea = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }

        var numeroPagina = 0
        var pagina: PdfDocument.Page? = null
        var y = 0f

        fun nuevaPagina() {
            pagina?.let(documento::finishPage)
            numeroPagina++
            pagina = documento.startPage(PdfDocument.PageInfo.Builder(anchoPagina, altoPagina, numeroPagina).create())
            val lienzo = pagina!!.canvas
            y = margen + 16f
            if (numeroPagina == 1) {
                lienzo.drawText("AquaSample · Reporte de muestras validadas", margen, y, titulo)
                y += 18f
                val generado = generadoEn.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                lienzo.drawText("ALDEMAR SpA · Generado el $generado por $generadoPor · ${filas.size} muestras", margen, y, normal)
                y += 10f
                lienzo.drawText(
                    "Total de choritos contados: ${filas.sumOf { it.cantidadChoritos }}",
                    margen, y + 10f, normal
                )
                y += 30f
            }
            columnas.forEach { (nombre, x) -> lienzo.drawText(nombre, x, y, encabezado) }
            y += 4f
            lienzo.drawLine(margen, y, anchoPagina - margen, y, linea)
            y += 12f
        }

        nuevaPagina()
        for (fila in filas) {
            if (y > altoPagina - margen) nuevaPagina()
            val lienzo = pagina!!.canvas
            val valores = listOf(
                fila.codigo, fila.fecha, recortar(fila.centro, 18), fila.tren, fila.linea.toString(),
                fila.cantidadChoritos.toString(), fila.tamanoPromedio, recortar(fila.observaciones, 22)
            )
            valores.zip(columnas).forEach { (valor, columna) -> lienzo.drawText(valor, columna.second, y, normal) }
            y += 4f
            lienzo.drawLine(margen, y, anchoPagina - margen, y, linea)
            y += 12f
        }
        pagina?.let(documento::finishPage)

        val carpeta = File(contexto.cacheDir, "reportes").apply { mkdirs() }
        val nombre = "reporte_validadas_${generadoEn.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.pdf"
        val archivo = File(carpeta, nombre)
        archivo.outputStream().use(documento::writeTo)
        documento.close()
        archivo
    }

    private fun recortar(texto: String, maximo: Int): String =
        if (texto.length <= maximo) texto else texto.take(maximo - 1) + "…"
}
