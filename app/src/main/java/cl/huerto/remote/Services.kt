package cl.huerto.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class Registro(val id: Long? = null, val nombre: String, val cantidad: Int? = null, val fecha: String? = null)
data class Clima(val current: LecturaActual)
data class LecturaActual(val temperature_2m: Double,val precipitation: Double,val time: String)
interface TallerApi {
    @GET("{coleccion}") suspend fun listar(@Header("Authorization") auth: String,@Path("coleccion") coleccion: String): List<Registro>
    @POST("{coleccion}") suspend fun crear(@Header("Authorization") auth: String,@Path("coleccion") coleccion: String,@Body dato: Registro): Registro
    @PUT("{coleccion}/{id}") suspend fun editar(@Header("Authorization") auth: String,@Path("coleccion") coleccion: String,@Path("id") id: Long,@Body dato: Registro): Registro
    @DELETE("{coleccion}/{id}") suspend fun borrar(@Header("Authorization") auth: String,@Path("coleccion") coleccion: String,@Path("id") id: Long)
}
interface ClimaApi {
    @GET("v1/forecast?latitude=-33.59&longitude=-70.70&current=temperature_2m,precipitation&timezone=America%2FSantiago")
    suspend fun consultar(): Clima
}
enum class Modulo(val ruta: String,val puerto: Int) { INSUMOS("insumos",8081), JORNADAS("jornadas",8082) }
fun retrofit(url: String): Retrofit = Retrofit.Builder().baseUrl(url).addConverterFactory(GsonConverterFactory.create()).build()
fun registro(modulo: Modulo,nombre: String,valor: String): Registro {
    require(nombre.trim().length in 1..100) { "Escribe un nombre de 1 a 100 caracteres." }
    return when(modulo) {
        Modulo.INSUMOS -> Registro(nombre=nombre.trim(),cantidad=requireNotNull(valor.toIntOrNull()?.takeIf { it>=0 }) { "La cantidad debe ser un entero mayor o igual a cero." })
        Modulo.JORNADAS -> {
            require(runCatching { java.time.LocalDate.parse(valor) }.isSuccess) { "Usa una fecha válida AAAA-MM-DD." }
            Registro(nombre=nombre.trim(),fecha=valor)
        }
    }
}
