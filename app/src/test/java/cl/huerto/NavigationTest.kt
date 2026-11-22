package cl.huerto

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
@LooperMode(LooperMode.Mode.PAUSED)
class NavigationTest {
    @get:Rule val screen = createAndroidComposeRule<MainActivity>()

    private fun waitText(text: String) {
        screen.waitUntil(15000) { screen.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun login(account: String) {
        waitText("Iniciar sesión")
        screen.onNodeWithText("Cuenta").performTextInput(account)
        screen.onNodeWithText("Clave").performTextInput("Huerto123")
        screen.onNodeWithText("Iniciar sesión").performClick()
    }
    @Test fun studentAndTeacherHaveDifferentNavigation() {
        login("luna")
        waitText("Mi jornada")
        screen.onNodeWithText("Revisar entregas").assertDoesNotExist()
        screen.onNodeWithText("Mis tareas").performClick()
        waitText("Riego · Acelga")
        screen.onNodeWithText("Crear y asignar tarea").assertDoesNotExist()
        screen.onNodeWithText("Salir").performClick()
        login("docente")
        waitText("Cuaderno del huerto")
        screen.onNodeWithText("Revisar entregas").performClick()
        waitText("Entregas por revisar")
    }
}
