package com.duoc.aquasample.ui.components

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Abre el menú del sistema para compartir el PDF (correo, WhatsApp, Drive...).
 * El FileProvider da permiso de lectura temporal a la app que lo reciba.
 * Lo usan el Dashboard y el Historial.
 */
fun compartirPdf(contexto: Context, archivo: File) {
    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
    val enviar = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    contexto.startActivity(Intent.createChooser(enviar, "Compartir reporte"))
}
