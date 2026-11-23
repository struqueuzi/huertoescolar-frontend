package cl.huerto

import cl.huerto.data.*
import cl.huerto.domain.*
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BranchCoverageTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun invalidCropBoundaries() {
        val crop = mapOf("Nombre" to "Acelga","Sector" to "1","Descripción" to "Bancal","Estado" to "Sembrado","Siembra" to "2026-11-01","Cosecha" to "2026-11-30","Trasplante" to "2026-11-15","Riego cada (días)" to "365")
        for (date in listOf("2026-10-31","2026-12-01","sin fecha"))
            assertTrue("Trasplante" in Rules.validate(Form.CULTIVO,crop+("Trasplante" to date)))
        for (key in listOf("Siembra","Cosecha"))
            assertEquals(setOf(key),Rules.validate(Form.CULTIVO,crop+(key to "")).keys)
        for (interval in listOf("366","dos","-2"))
            assertTrue("Riego cada (días)" in Rules.validate(Form.CULTIVO,crop+("Riego cada (días)" to interval)))
        for ((key,value) in mapOf("Estado" to "Otro","Descripción" to "x".repeat(1001),"Nombre" to "x".repeat(101)))
            assertEquals(setOf(key),Rules.validate(Form.CULTIVO,crop+(key to value)).keys)
    }

    private suspend fun rejected(action: suspend () -> Unit) {
        try { action(); fail("Se esperaba rechazo") }
        catch (expected: IllegalArgumentException) { assertFalse(expected.message.isNullOrBlank()) }
    }

    @Test fun invalidAccountsAndAssignments() = runTest {
        val dao = FakeDao()
        val repo = Repository(dao,temporary.root,{ it() },{ File(it,"foto").apply { writeText("muestra") }.path })
        repo.seed()
        assertNull(repo.login("inexistente","clave"))
        rejected { repo.start(999,1) }
        rejected { repo.start(1,1) }
        rejected { repo.start(2,999) }
        rejected { repo.deleteSector(1,1) }
        rejected { repo.deleteSector(2,1) }
        rejected { repo.save(1,Form.SECTOR,0,emptyMap()) }
        val task=mapOf("Tipo" to "Riego","Fecha" to "2026-11-23","Cultivo" to "1","Instrucciones" to "Regar")
        dao.insert(Usuario(nombre="Inactivo",rol=STUDENT,grupo="Taller",activo=false,claveHash="x"))
        rejected { repo.start(5,1) }
        for (owner in listOf("1","5","999","abc"))
            rejected { repo.save(1,Form.TAREA,0,task+("Responsable" to owner)) }
        repo.start(2,1)
        rejected { repo.start(2,1) }
        rejected { repo.save(1,Form.TAREA,1,task+("Responsable" to "2")) }
        assertEquals(WORKING,dao.getTarea(1)!!.estado)
    }

    @Test fun photoAndReportAssociations() = runTest {
        val dao=FakeDao()
        val dir=temporary.newFolder("fotos")
        val repo=Repository(dao,dir,{ it() },{ File(it,"foto").apply { writeText("muestra") }.path })
        repo.seed()
        val fields=mapOf("Sector" to "1","Tipo" to "Falta de agua","Descripción" to "Bancal seco","Foto" to File(dir,"foto").path)
        rejected { repo.save(1,Form.REPORTE,0,fields+("Cultivo" to "99")) }
        for (file in listOf(temporary.newFile(), File(dir,"vacia").apply { createNewFile() }))
            rejected { repo.save(1,Form.REPORTE,0,fields+("Foto" to file.path)) }
        repo.save(1,Form.REPORTE,0,fields)
        assertNull(dao.getReporteProblema(2)!!.cultivo)
        rejected { repo.save(2,Form.ENTREGA,1,mapOf("Comentario" to "Hecho","Foto" to fields.getValue("Foto"))) }
        rejected { repo.save(1,Form.REVISION,1,mapOf("Resultado" to APPROVED)) }
        assertEquals(PENDING,dao.getTarea(1)!!.estado)
    }
}
