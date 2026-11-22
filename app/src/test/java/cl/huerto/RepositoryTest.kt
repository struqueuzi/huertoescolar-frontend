package cl.huerto

import android.content.Context
import androidx.room.Room
import cl.huerto.data.*
import cl.huerto.domain.*
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class RepositoryTest {
    private lateinit var context: Context
    private lateinit var db: HuertoDb
    private lateinit var repo: Repository
    private fun open() {
        db=Room.databaseBuilder(context,HuertoDb::class.java,"test-huerto.db").build()
        repo=Repository(db,File(context.filesDir,"fotos"))
    }
    @Before fun setup() {
        context=RuntimeEnvironment.getApplication()
        context.deleteDatabase("test-huerto.db")
        open()
        runBlocking { repo.seed() }
    }
    @After fun close() { db.close() }
    @Test fun authorizationAndResubmissionSurviveReopen() = runBlocking {
        assertNull(repo.login("luna","mal"))
        assertEquals(2L,repo.login("luna","Huerto123")!!.id)
        assertTrue(runCatching { repo.start(3,1) }.isFailure)
        repo.start(2,1)
        val photo=db.dao().latest(5)!!.foto
        val delivery=mapOf("Comentario" to "Riego realizado","Foto" to photo)
        repo.save(2,Form.ENTREGA,1,delivery)
        assertTrue(runCatching { repo.save(2,Form.REVISION,1,mapOf("Resultado" to APPROVED)) }.isFailure)
        assertTrue(runCatching { repo.save(1,Form.REVISION,1,mapOf("Resultado" to REJECTED)) }.isFailure)
        repo.save(1,Form.REVISION,1,mapOf("Resultado" to REJECTED,"Comentario" to "Muestra también la base."))
        repo.start(2,1)
        repo.save(2,Form.ENTREGA,1,delivery)
        repo.save(1,Form.REVISION,1,mapOf("Resultado" to APPROVED))
        db.close()
        open()
        assertEquals(APPROVED,db.dao().getTarea(1)!!.estado)
        assertTrue(File(db.dao().latest(1)!!.foto).isFile)
        assertEquals(REJECTED,db.dao().getValidacion(1)!!.resultado)
        assertEquals(APPROVED,db.dao().getValidacion(2)!!.resultado)
    }
    @Test fun reportFollowupAndRestrictedDeletion() = runBlocking {
        val fields=mapOf("Estado" to "Cerrado","Comentario" to "Riego verificado")
        assertTrue(runCatching { repo.save(2,Form.SEGUIMIENTO,1,fields) }.isFailure)
        repo.save(1,Form.SEGUIMIENTO,1,fields)
        assertEquals("Cerrado",db.dao().getReporteProblema(1)!!.estado)
        assertTrue(runCatching { repo.deleteSector(1,1) }.isFailure)
    }
}
