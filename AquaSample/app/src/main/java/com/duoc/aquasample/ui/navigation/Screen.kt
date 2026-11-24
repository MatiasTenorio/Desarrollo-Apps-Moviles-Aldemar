package com.duoc.aquasample.ui.navigation

import com.duoc.aquasample.ui.sampledetail.SampleDetailViewModel

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Home : Screen("home")

    /** Grafo anidado del asistente "Nueva muestra": sus pasos comparten ViewModel. */
    data object NewSample : Screen("new_sample")
    data object NewSampleLineaFoto : Screen("new_sample/linea_foto")
    data object NewSampleCamara : Screen("new_sample/camara")
    data object NewSampleConteo : Screen("new_sample/conteo")
    data object NewSampleGuardar : Screen("new_sample/guardar")

    data object History : Screen("history")

    data object SampleDetail : Screen("sample_detail/{${SampleDetailViewModel.ARG_MUESTRA_ID}}") {
        fun crearRuta(muestraId: Long): String = "sample_detail/$muestraId"
    }

    data object EditarMuestra : Screen("editar_muestra/{${SampleDetailViewModel.ARG_MUESTRA_ID}}") {
        fun crearRuta(muestraId: Long): String = "editar_muestra/$muestraId"
    }

    data object Profile : Screen("profile")
    data object Dashboard : Screen("dashboard")
    data object AdminUsuarios : Screen("admin_usuarios")
}
