package cl.huerto.remote
import org.junit.Assert.*
import org.junit.Test
class TallerTest {
    @Test fun zeroStock() { assertEquals(Registro(nombre="Semillas",cantidad=0),registro(Modulo.INSUMOS," Semillas ","0")) }
    @Test fun negativeStock() { assertThrows(IllegalArgumentException::class.java){registro(Modulo.INSUMOS,"Palas","-1")} }
    @Test fun nonIntegerStock() { assertThrows(IllegalArgumentException::class.java){registro(Modulo.INSUMOS,"Palas","1.5")} }
    @Test fun validLeapDay() { assertEquals("2028-02-29",registro(Modulo.JORNADAS,"Taller","2028-02-29").fecha) }
    @Test fun impossibleDate() { assertThrows(IllegalArgumentException::class.java){registro(Modulo.JORNADAS,"Taller","2027-02-29")} }
    @Test fun missingName() { assertThrows(IllegalArgumentException::class.java){registro(Modulo.JORNADAS," ","2026-11-22")} }
    @Test fun longName() { assertThrows(IllegalArgumentException::class.java){registro(Modulo.INSUMOS,"a".repeat(101),"1")} }
}
