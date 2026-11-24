package com.duoc.aquasample.ui.admin

import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario
import com.duoc.aquasample.data.repository.ErrorUsuarios
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.data.repository.UsuarioAdministrado
import com.duoc.aquasample.data.repository.UsuariosRepository
import com.duoc.aquasample.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdminUsuariosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val ana = UsuarioAdministrado(1, "ana@test.cl", "Ana", Rol.OPERADOR)
    private val beto = UsuarioAdministrado(2, "beto@test.cl", "Beto", Rol.ANALISTA)

    private val repository = mockk<UsuariosRepository> {
        coEvery { listar() } returns listOf(ana, beto)
    }

    private fun crearViewModel(rol: Rol = Rol.ADMINISTRADOR) = AdminUsuariosViewModel(
        repository,
        SesionRepository().apply { iniciar(Usuario("jefa", "", rol, "jefa@test.cl")) }
    )

    @Test
    fun `el administrador ve la lista de usuarios`() {
        val estado = crearViewModel().uiState.value

        assertTrue(estado.permitido)
        assertFalse(estado.cargando)
        assertEquals(listOf(ana, beto), estado.usuarios)
    }

    @Test
    fun `otro rol no puede ni cargar`() {
        val estado = crearViewModel(Rol.ANALISTA).uiState.value

        assertFalse(estado.permitido)
        coVerify(exactly = 0) { repository.listar() }
    }

    @Test
    fun `si falla la carga muestra el error`() {
        coEvery { repository.listar() } throws ErrorUsuarios("No se pudo conectar con el backend. ¿Está encendido?")

        assertEquals("No se pudo conectar con el backend. ¿Está encendido?", crearViewModel().uiState.value.error)
    }

    @Test
    fun `alta valida el formulario antes de llamar al repositorio`() {
        val viewModel = crearViewModel()
        viewModel.onNuevo()
        viewModel.onCorreoChange("no-es-correo")
        viewModel.onContrasenaChange("123")

        viewModel.guardar()

        assertEquals(
            listOf(
                "Escribe un correo válido",
                "Escribe el nombre",
                "La contraseña debe tener al menos 8 caracteres"
            ),
            viewModel.uiState.value.formulario?.errores
        )
        coVerify(exactly = 0) { repository.crear(any(), any(), any(), any()) }
    }

    @Test
    fun `alta correcta agrega al usuario y cierra el formulario`() {
        val nueva = UsuarioAdministrado(3, "carla@test.cl", "Carla", Rol.ANALISTA)
        coEvery { repository.crear("carla@test.cl", "Carla", "clave123", Rol.ANALISTA) } returns nueva
        val viewModel = crearViewModel()
        viewModel.onNuevo()
        viewModel.onCorreoChange("carla@test.cl")
        viewModel.onNombreChange("Carla")
        viewModel.onContrasenaChange("clave123")
        viewModel.onRolChange(Rol.ANALISTA)

        viewModel.guardar()

        val estado = viewModel.uiState.value
        assertNull(estado.formulario)
        assertEquals(listOf("Ana", "Beto", "Carla"), estado.usuarios.map { it.nombre })
        assertEquals("Usuario carla@test.cl creado", estado.mensaje)

        viewModel.onMensajeMostrado()
        assertNull(viewModel.uiState.value.mensaje)
    }

    @Test
    fun `editar cambia nombre y rol sin pedir contrasena`() {
        val editado = ana.copy(nombre = "Ana María", rol = Rol.ADMINISTRADOR)
        coEvery { repository.actualizar(1, "Ana María", Rol.ADMINISTRADOR) } returns editado
        val viewModel = crearViewModel()
        viewModel.onEditar(ana)
        assertFalse(viewModel.uiState.value.formulario!!.esNuevo)
        viewModel.onNombreChange("Ana María")
        viewModel.onRolChange(Rol.ADMINISTRADOR)

        viewModel.guardar()

        assertEquals(Rol.ADMINISTRADOR, viewModel.uiState.value.usuarios.first { it.id == 1L }.rol)
        assertEquals("Cambios guardados", viewModel.uiState.value.mensaje)
    }

    @Test
    fun `si el repositorio falla el error queda en el formulario`() {
        coEvery { repository.actualizar(any(), any(), any()) } throws ErrorUsuarios("El backend rechazó la operación: HTTP 404")
        val viewModel = crearViewModel()
        viewModel.onEditar(beto)

        viewModel.guardar()

        val formulario = viewModel.uiState.value.formulario!!
        assertEquals(listOf("El backend rechazó la operación: HTTP 404"), formulario.errores)
        assertFalse(formulario.guardando)
    }

    @Test
    fun `cerrar el formulario lo descarta`() {
        val viewModel = crearViewModel()
        viewModel.onNuevo()
        viewModel.onCerrarFormulario()
        assertNull(viewModel.uiState.value.formulario)
        // Sin formulario abierto, los cambios no hacen nada.
        viewModel.onNombreChange("x")
        viewModel.guardar()
        assertNull(viewModel.uiState.value.formulario)
    }

    @Test
    fun `validar formulario acepta un alta completa`() {
        assertTrue(validarFormulario(FormularioUsuario(correo = "a@b.cl", nombre = "A", contrasena = "12345678")).isEmpty())
        assertEquals(
            listOf("El nombre no puede superar 100 caracteres"),
            validarFormulario(FormularioUsuario(id = 1, nombre = "x".repeat(101)))
        )
    }

    // --- Dar de baja ---

    @Test
    fun `dar de baja pide confirmacion y luego quita al usuario`() {
        coEvery { repository.eliminar(2) } returns Unit
        val viewModel = crearViewModel()
        viewModel.onEditar(beto)
        assertTrue(viewModel.uiState.value.formulario!!.puedeDarDeBaja)

        viewModel.onPedirBaja()
        assertTrue(viewModel.uiState.value.formulario!!.confirmandoBaja)
        viewModel.onCancelarBaja()
        assertFalse(viewModel.uiState.value.formulario!!.confirmandoBaja)
        viewModel.onPedirBaja()
        viewModel.darDeBaja()

        val estado = viewModel.uiState.value
        assertNull(estado.formulario)
        assertEquals(listOf(ana), estado.usuarios)
        assertEquals("Usuario beto@test.cl dado de baja", estado.mensaje)
    }

    @Test
    fun `el administrador no puede darse de baja a si mismo`() {
        val propia = UsuarioAdministrado(9, "jefa@test.cl", "Jefa", Rol.ADMINISTRADOR)
        val viewModel = crearViewModel()
        viewModel.onEditar(propia)

        assertFalse(viewModel.uiState.value.formulario!!.puedeDarDeBaja)
        viewModel.onPedirBaja()
        assertFalse(viewModel.uiState.value.formulario!!.confirmandoBaja)
        viewModel.darDeBaja()
        coVerify(exactly = 0) { repository.eliminar(any()) }
    }

    @Test
    fun `si la baja falla el error queda en el formulario`() {
        coEvery { repository.eliminar(2) } throws ErrorUsuarios("Insforge no permitió dar de baja la cuenta.")
        val viewModel = crearViewModel()
        viewModel.onEditar(beto)
        viewModel.onPedirBaja()

        viewModel.darDeBaja()

        val formulario = viewModel.uiState.value.formulario!!
        assertEquals(listOf("Insforge no permitió dar de baja la cuenta."), formulario.errores)
        assertFalse(formulario.confirmandoBaja)
        assertFalse(formulario.guardando)
    }

    @Test
    fun `en un alta nueva no se puede dar de baja`() {
        val viewModel = crearViewModel()
        viewModel.onNuevo()
        viewModel.darDeBaja()
        coVerify(exactly = 0) { repository.eliminar(any()) }
    }
}
