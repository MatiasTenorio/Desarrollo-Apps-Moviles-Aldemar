package com.duoc.aquasample.ui.newSample

import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario
import com.duoc.aquasample.data.model.ValidadorMuestra
import com.duoc.aquasample.data.repository.FotoRepository
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.File
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NewSampleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val momentoFoto = LocalDateTime.of(2026, 11, 24, 9, 15)
    private val fotos = mockk<FotoRepository>(relaxed = true)
    private val muestras = mockk<MuestraRepository>(relaxed = true)
    private val sesion = SesionRepository().apply { iniciar(Usuario("operador1", "1234", Rol.OPERADOR)) }

    private fun crearViewModel() = NewSampleViewModel(fotos, muestras, sesion, ahora = { momentoFoto })

    private val viewModel = crearViewModel()

    /** Paso 1 completo, listo para marcar. */
    private fun completarPaso1(vm: NewSampleViewModel = viewModel) {
        vm.onCentroSeleccionado("Centro Bahía Norte")
        vm.onTrenSeleccionado("Tren 2")
        vm.onLineaSeleccionada(3)
        vm.onLongitudChange("1,5")
        vm.onFotoCapturada("/fotos/a.jpg")
    }

    // Foto en pantalla de 100 x 200 px, radio de toque de 10 px.
    private fun tocar(x: Float, y: Float) = viewModel.onTocarFoto(x, y, ancho = 100f, alto = 200f, radioToque = 10f)

    // ---------- Paso 1 ----------

    @Test
    fun `el paso 1 exige centro, tren, linea, longitud y foto`() {
        assertFalse(viewModel.uiState.value.puedeAvanzar)

        viewModel.onCentroSeleccionado("Centro Bahía Norte")
        viewModel.onTrenSeleccionado("Tren 2")
        viewModel.onLineaSeleccionada(3)
        viewModel.onLongitudChange("1,5")
        assertFalse(viewModel.uiState.value.puedeAvanzar) // falta la foto

        viewModel.onFotoCapturada("/fotos/a.jpg")
        assertTrue(viewModel.uiState.value.puedeAvanzar)
        assertEquals(1.5, viewModel.uiState.value.longitudTramoM!!, 0.0001)
    }

    @Test
    fun `una longitud cero no permite avanzar`() {
        completarPaso1()
        viewModel.onLongitudChange("0")
        assertFalse(viewModel.uiState.value.puedeAvanzar)
    }

    @Test
    fun `la longitud solo acepta numeros con hasta 2 decimales`() {
        viewModel.onLongitudChange("12.25")
        assertEquals("12.25", viewModel.uiState.value.longitudTramoTexto)

        viewModel.onLongitudChange("12.255")
        viewModel.onLongitudChange("abc")
        assertEquals("12.25", viewModel.uiState.value.longitudTramoTexto)
    }

    @Test
    fun `la fecha y hora de la muestra es el momento de la foto`() {
        viewModel.onFotoCapturada("/fotos/a.jpg")
        assertEquals(momentoFoto, viewModel.uiState.value.fechaHora)
    }

    @Test
    fun `una foto nueva reemplaza a la anterior, borra su archivo y sus marcas`() {
        completarPaso1()
        tocar(50f, 50f)

        viewModel.onFotoCapturada("/fotos/b.jpg")

        assertEquals("/fotos/b.jpg", viewModel.uiState.value.fotoPath)
        assertTrue(viewModel.uiState.value.marcas.isEmpty())
        verify(exactly = 1) { fotos.eliminar("/fotos/a.jpg") }
        verify(exactly = 0) { fotos.eliminar("/fotos/b.jpg") }
    }

    @Test
    fun `la camara recibe un archivo nuevo del repositorio`() {
        every { fotos.crearArchivoNuevo() } returns File("/fotos/nueva.jpg")
        assertEquals(File("/fotos/nueva.jpg"), viewModel.crearArchivoParaCamara())
    }

    @Test
    fun `elegir de la galeria copia la foto a la carpeta de la app`() {
        val uri = mockk<Uri>()
        coEvery { fotos.copiarDesdeGaleria(uri) } returns "/fotos/galeria.jpg"

        viewModel.onFotoSeleccionadaDesdeGaleria(uri)

        val estado = viewModel.uiState.value
        assertEquals("/fotos/galeria.jpg", estado.fotoPath)
        assertFalse(estado.cargandoFoto)
        assertNull(estado.errorFoto)
    }

    @Test
    fun `si la galeria falla se avisa y se conserva la foto anterior`() {
        val uri = mockk<Uri>()
        coEvery { fotos.copiarDesdeGaleria(uri) } returns null
        viewModel.onFotoCapturada("/fotos/a.jpg")

        viewModel.onFotoSeleccionadaDesdeGaleria(uri)

        val estado = viewModel.uiState.value
        assertEquals("/fotos/a.jpg", estado.fotoPath)
        assertFalse(estado.cargandoFoto)
        assertNotNull(estado.errorFoto)
        verify(exactly = 0) { fotos.eliminar(any()) }
    }

    @Test
    fun `quitar la foto borra el archivo y las marcas`() {
        completarPaso1()
        tocar(50f, 50f)

        viewModel.onQuitarFoto()

        assertNull(viewModel.uiState.value.fotoPath)
        assertTrue(viewModel.uiState.value.marcas.isEmpty())
        verify { fotos.eliminar("/fotos/a.jpg") }
    }

    // ---------- Paso 2: conteo asistido ----------

    @Test
    fun `tocar un lugar vacio agrega una marca relativa a la foto`() {
        tocar(25f, 150f)

        val marca = viewModel.uiState.value.marcas.single()
        assertEquals(0.25f, marca.posX, 0.0001f)
        assertEquals(0.75f, marca.posY, 0.0001f)
        assertEquals(1, viewModel.uiState.value.cantidadChoritos)
        assertTrue(viewModel.uiState.value.puedeIrAGuardar)
    }

    @Test
    fun `tocar sobre una marca la quita`() {
        tocar(25f, 150f)
        tocar(80f, 20f)

        tocar(27f, 148f) // a menos de 10 px de la primera

        assertEquals(1, viewModel.uiState.value.cantidadChoritos)
        assertEquals(0.8f, viewModel.uiState.value.marcas.single().posX, 0.0001f)
    }

    @Test
    fun `si hay dos marcas cerca se quita la mas proxima`() {
        val marcas = listOf(MarcaChorito(0.10f, 0.10f), MarcaChorito(0.15f, 0.10f))
        // En 100 x 200 px: marcas en (10, 20) y (15, 20); toque en (14, 20).
        assertEquals(1, indiceMarcaTocada(marcas, 14f, 20f, 100f, 200f, radio = 10f))
        assertNull(indiceMarcaTocada(marcas, 60f, 60f, 100f, 200f, radio = 10f))
    }

    @Test
    fun `una foto sin tamaño en pantalla no agrega marcas`() {
        viewModel.onTocarFoto(10f, 10f, ancho = 0f, alto = 0f, radioToque = 10f)
        assertTrue(viewModel.uiState.value.marcas.isEmpty())
    }

    @Test
    fun `deshacer quita la ultima marca y limpiar quita todas`() {
        tocar(10f, 10f)
        tocar(50f, 50f)
        tocar(90f, 190f)

        viewModel.onDeshacerMarca()
        assertEquals(2, viewModel.uiState.value.cantidadChoritos)
        assertEquals(0.5f, viewModel.uiState.value.marcas.last().posX, 0.0001f)

        viewModel.onLimpiarMarcas()
        assertEquals(0, viewModel.uiState.value.cantidadChoritos)
        assertFalse(viewModel.uiState.value.puedeIrAGuardar)

        viewModel.onDeshacerMarca() // sin marcas no hace nada
        assertEquals(0, viewModel.uiState.value.cantidadChoritos)
    }

    @Test
    fun `se registra el tamaño de cada chorito avanzando de uno en uno`() {
        tocar(10f, 10f)
        tocar(50f, 50f)

        viewModel.onTamanoChange("5,4")
        viewModel.onMedirSiguiente()
        assertEquals(2, viewModel.uiState.value.numeroEnMedicion)
        assertFalse(viewModel.uiState.value.puedeMedirSiguiente)
        viewModel.onMedirSiguiente() // ya es el último
        assertEquals(2, viewModel.uiState.value.numeroEnMedicion)

        viewModel.onTamanoChange("6")
        viewModel.onMedirAnterior()
        assertEquals("5,4", viewModel.uiState.value.tamanoEnMedicion)
        viewModel.onMedirAnterior() // ya es el primero
        assertEquals(1, viewModel.uiState.value.numeroEnMedicion)

        val choritos = viewModel.uiState.value.choritos
        assertEquals(listOf(1, 2), choritos.map { it.numero })
        assertEquals(listOf(5.4, 6.0), choritos.map { it.tamanoCm })
        assertEquals(2, viewModel.uiState.value.choritosMedidos)
    }

    @Test
    fun `el tamaño rechaza texto invalido y no hace nada sin marcas`() {
        viewModel.onTamanoChange("5")
        assertTrue(viewModel.uiState.value.marcas.isEmpty())

        tocar(10f, 10f)
        viewModel.onTamanoChange("123")
        viewModel.onTamanoChange("x")
        assertEquals("", viewModel.uiState.value.tamanoEnMedicion)
    }

    @Test
    fun `quitar el chorito que se estaba midiendo ajusta el indice`() {
        tocar(10f, 10f)
        tocar(50f, 50f)
        viewModel.onMedirSiguiente()

        viewModel.onDeshacerMarca()

        assertEquals(1, viewModel.uiState.value.numeroEnMedicion)
    }

    // ---------- Paso 3: guardar ----------

    @Test
    fun `la muestra se arma con todo lo ingresado y el operador de la sesion`() {
        completarPaso1()
        tocar(10f, 10f)
        viewModel.onObservacionesChange("Línea con algas")

        val muestra = viewModel.muestraActual()

        assertEquals("Centro Bahía Norte", muestra.centro)
        assertEquals("Tren 2", muestra.tren)
        assertEquals(3, muestra.linea)
        assertEquals(1.5, muestra.longitudTramoM, 0.0001)
        assertEquals(momentoFoto, muestra.fechaHora)
        assertEquals("operador1", muestra.operador)
        assertEquals("Línea con algas", muestra.observaciones)
        assertEquals("/fotos/a.jpg", muestra.fotoPath)
        assertEquals(1, muestra.cantidadChoritos)
    }

    @Test
    fun `las observaciones no pasan del maximo`() {
        viewModel.onObservacionesChange("a".repeat(ValidadorMuestra.MAX_OBSERVACIONES))
        viewModel.onObservacionesChange("a".repeat(ValidadorMuestra.MAX_OBSERVACIONES + 1))
        assertEquals(ValidadorMuestra.MAX_OBSERVACIONES, viewModel.uiState.value.observaciones.length)
    }

    @Test
    fun `guardar una muestra completa la manda al repositorio`() {
        val guardada = slot<Muestra>()
        coEvery { muestras.guardar(capture(guardada)) } returns 15L
        completarPaso1()
        tocar(10f, 10f)

        viewModel.guardar()

        assertEquals(15L, viewModel.uiState.value.idGuardada)
        assertFalse(viewModel.uiState.value.guardando)
        assertEquals(1, guardada.captured.cantidadChoritos)

        viewModel.guardar() // no guarda dos veces
        coVerify(exactly = 1) { muestras.guardar(any()) }
    }

    @Test
    fun `guardar sin datos muestra los errores y no llama al repositorio`() {
        viewModel.guardar()

        assertTrue(viewModel.uiState.value.errores.contains("Selecciona el centro de cultivo"))
        assertTrue(viewModel.uiState.value.errores.contains("Agrega la foto de la muestra"))
        assertNull(viewModel.uiState.value.idGuardada)
        coVerify(exactly = 0) { muestras.guardar(any()) }
    }

    @Test
    fun `si falla el guardado se avisa y se puede reintentar`() {
        coEvery { muestras.guardar(any()) } throws IllegalStateException("disco lleno")
        completarPaso1()
        tocar(10f, 10f)

        viewModel.guardar()

        assertEquals(listOf("No se pudo guardar la muestra. Intenta de nuevo."), viewModel.uiState.value.errores)
        assertFalse(viewModel.uiState.value.guardando)
        assertNull(viewModel.uiState.value.idGuardada)

        viewModel.onObservacionesChange("reintento")
        assertTrue(viewModel.uiState.value.errores.isEmpty())
    }

    // ---------- Limpieza de la foto al salir ----------

    /** Crea el ViewModel dentro de un ViewModelStore para poder simular que se destruye. */
    private fun viewModelEnStore(store: ViewModelStore): NewSampleViewModel =
        ViewModelProvider(store, viewModelFactory { initializer { crearViewModel() } })
            .get(NewSampleViewModel::class.java)

    @Test
    fun `salir del asistente sin guardar borra la foto`() {
        val store = ViewModelStore()
        viewModelEnStore(store).onFotoCapturada("/fotos/a.jpg")

        store.clear()

        verify { fotos.eliminar("/fotos/a.jpg") }
    }

    @Test
    fun `si la muestra se guardo la foto no se borra al salir`() {
        coEvery { muestras.guardar(any()) } returns 1L
        val store = ViewModelStore()
        val enStore = viewModelEnStore(store)
        completarPaso1(enStore)
        enStore.onTocarFoto(10f, 10f, 100f, 200f, 10f)
        enStore.guardar()

        store.clear()

        verify(exactly = 0) { fotos.eliminar(any()) }
    }
}
