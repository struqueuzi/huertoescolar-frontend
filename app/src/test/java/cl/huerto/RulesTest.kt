package cl.huerto

import cl.huerto.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RulesTest {
    private val crop=mapOf("Nombre" to "Acelga","Sector" to "1","Descripción" to "Bancal","Estado" to "Sembrado","Siembra" to "2026-10-20","Cosecha" to "2026-12-10","Riego cada (días)" to "2")
    @Test fun validCrop() { assertTrue(Rules.validate(Form.CULTIVO,crop).isEmpty()) }
    @Test fun harvestBeforeSowing() { assertTrue("Cosecha" in Rules.validate(Form.CULTIVO,crop+("Cosecha" to "2026-10-19"))) }
    @Test fun impossibleDate() { assertTrue("Siembra" in Rules.validate(Form.CULTIVO,crop+("Siembra" to "2026-02-30"))) }
    @Test fun positiveWatering() { assertTrue("Riego cada (días)" in Rules.validate(Form.CULTIVO,crop+("Riego cada (días)" to "0"))) }
    @Test fun rejectionRequiresReason() { assertTrue("Comentario" in Rules.validate(Form.REVISION,mapOf("Resultado" to REJECTED,"Comentario" to "  "))) }
    @Test fun evidenceRequiresPhoto() { assertTrue("Foto" in Rules.validate(Form.ENTREGA,mapOf("Comentario" to "Listo"))) }
    @Test fun pendingReviewIsNotLate() { assertFalse(overdue(SUBMITTED,"2026-10-20",LocalDate.parse("2026-10-21"))) }
    @Test fun overdueOnlyAfterDate() {
        assertTrue(overdue(WORKING,"2026-10-20",LocalDate.parse("2026-10-21")))
        assertFalse(overdue(PENDING,"2026-10-20",LocalDate.parse("2026-10-20")))
    }
}
