package cl.huerto.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class TallerViewModelTest {
    private val dispatcher=StandardTestDispatcher()
    private val api=object : TallerApi {
        var fail=false
        val rows=mutableListOf<Registro>()
        override suspend fun listar(auth:String,coleccion:String):List<Registro> {
            if(fail) throw java.io.IOException()
            return rows.toList()
        }
        override suspend fun crear(auth:String,coleccion:String,dato:Registro)=dato.copy(id=1).also { rows.add(it) }
        override suspend fun editar(auth:String,coleccion:String,id:Long,dato:Registro)=dato.copy(id=id).also { rows[0]=it }
        override suspend fun borrar(auth:String,coleccion:String,id:Long){rows.removeAll { it.id==id }}
    }
    @Before fun setup(){Dispatchers.setMain(dispatcher)}
    @After fun cleanup(){Dispatchers.resetMain()}
    @Test fun createAndRefreshOnlyOnce()=runTest(dispatcher){
        val vm=TallerViewModel(Modulo.INSUMOS,api)
        vm.ejecutar("prueba"){a,h->a.crear(h,"insumos",registro(Modulo.INSUMOS,"Palas","2"))}
        vm.ejecutar("prueba"){_,_->fail("No debe iniciar una segunda operación")}
        assertTrue(vm.panel.value.ocupado)
        advanceUntilIdle()
        assertEquals("Palas",vm.panel.value.filas.single().nombre)
        assertFalse(vm.panel.value.ocupado)
    }
    @Test fun errorsReachTheScreen()=runTest(dispatcher){
        val vm=TallerViewModel(Modulo.JORNADAS,api)
        vm.ejecutar("");advanceUntilIdle()
        assertTrue(vm.panel.value.aviso.contains("clave"))
        api.fail=true
        vm.ejecutar("prueba");advanceUntilIdle()
        assertTrue(vm.panel.value.aviso.contains("Sin conexión"))
        assertFalse(vm.panel.value.ocupado)
    }
}
