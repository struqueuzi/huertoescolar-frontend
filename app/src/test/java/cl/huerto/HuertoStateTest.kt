package cl.huerto

import cl.huerto.data.*
import cl.huerto.ui.*
import cl.huerto.domain.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import androidx.lifecycle.viewModelScope
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HuertoStateTest {
    @get:Rule val temp=TemporaryFolder()
    private val dispatcher=StandardTestDispatcher()
    @Before fun init(){Dispatchers.setMain(dispatcher)}
    @After fun finish(){Dispatchers.resetMain()}
    @Test fun completeWorkshop()=runTest(dispatcher){
        val dao=FakeDao()
        val repo=Repository(dao,temp.root,{ work -> work() },{ folder -> File(folder,"muestra").apply {writeText("prueba")}.path })
        val vm=HuertoViewModel(repo)
        try {
            advanceUntilIdle()
            assertTrue(vm.state.value.ready)
            vm.login("docente","incorrecta");advanceUntilIdle()
            assertTrue("Cuenta" in vm.errors.value)
            fun login(name:String) { vm.logout();vm.login(name,"Huerto123");testScheduler.advanceUntilIdle();assertEquals(name,vm.state.value.user!!.nombre) }
            fun save(f:Form,id:Long,fields:Fields){
                var saved=false
                vm.save(f,id,fields){saved=true};testScheduler.advanceUntilIdle()
                assertTrue(vm.message.value,saved)
            }
            login("docente")
            val sector=mapOf("Nombre" to "Bancal sur","Descripción" to "Hortalizas","Ubicación" to "Sur","Estado" to "Disponible")
            save(Form.SECTOR,0,sector);save(Form.SECTOR,2,sector+("Ubicación" to "Patio sur"))
            save(Form.SECTOR,0,sector)
            vm.deleteSector(3){};advanceUntilIdle();assertNull(dao.getSector(3))
            val cultivo=mapOf("Nombre" to "Apio","Sector" to "2","Descripción" to "Taller","Estado" to "Sembrado","Siembra" to "2026-10-01","Trasplante" to "2026-10-15","Cosecha" to "2026-12-01","Riego cada (días)" to "2")
            save(Form.CULTIVO,0,cultivo);save(Form.CULTIVO,2,cultivo+("Estado" to "En crecimiento"))
            val task=mapOf("Cultivo" to "2","Responsable" to "2","Tipo" to "Riego","Instrucciones" to "Regar el apio","Fecha" to "2026-11-05")
            save(Form.TAREA,0,task);save(Form.TAREA,6,task+("Fecha" to "2026-11-06"))
            login("nico");vm.start(6);advanceUntilIdle();assertEquals(PENDING,dao.getTarea(6)!!.estado)
            login("luna");assertTrue(vm.state.value.garden.tarea.all { it.responsable==2L })
            vm.start(6);advanceUntilIdle()
            val foto=dao.latest(5)!!.foto
            val entrega=mapOf("Comentario" to "Regado","Foto" to foto)
            save(Form.ENTREGA,6,entrega)
            val reporte=mapOf("Sector" to "2","Cultivo" to "2","Tipo" to "Falta de agua","Descripción" to "Bancal seco","Foto" to foto)
            save(Form.REPORTE,0,reporte)
            login("docente")
            save(Form.SEGUIMIENTO,2,mapOf("Estado" to "En revisión","Comentario" to "Revisar suelo"))
            save(Form.REVISION,6,mapOf("Resultado" to REJECTED,"Comentario" to "Repetir registro"))
            login("luna");vm.start(6);advanceUntilIdle();save(Form.ENTREGA,6,entrega)
            login("docente");save(Form.REVISION,6,mapOf("Resultado" to APPROVED))
            assertEquals(2,dao.validacionRows.value.size);assertEquals(APPROVED,dao.getTarea(6)!!.estado)
            repo.seed();assertEquals(4,dao.usuarioRows.value.size)
        } finally { vm.viewModelScope.cancel() }
    }
}
