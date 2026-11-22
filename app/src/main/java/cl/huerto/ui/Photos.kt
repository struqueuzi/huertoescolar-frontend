package cl.huerto.ui
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import android.net.Uri
import android.graphics.BitmapFactory
import kotlinx.coroutines.*

@Composable fun Preview(path: String) {
    if(path.isNotBlank()) AsyncImage(model=File(path),contentDescription="Fotografía del registro",contentScale=ContentScale.Fit,
        modifier=Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(12.dp)))
}
@Composable fun PhotoField(path: String,error: String?,change: (String)->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var pending by rememberSaveable { mutableStateOf("") }
    var problem by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    fun importImage(uri: Uri,temp: File?=null) {
        loading=true
        scope.launch {
            try {
                val target=withContext(Dispatchers.IO) {
                    val folder=File(context.filesDir,"fotos").apply { mkdirs() }
                    val file=File.createTempFile("huerto-",".img",folder)
                    try {
                        requireNotNull(context.contentResolver.openInputStream(uri)).use { input -> file.outputStream().use { input.copyTo(it) } }
                        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                        BitmapFactory.decodeFile(file.path,bounds)
                        require(bounds.outWidth>0) { "La fotografía no se puede leer" }
                        file.absolutePath
                    } catch(e: Exception) { file.delete(); throw e }
                }
                change(target); problem=""
            } catch(e: Exception) { problem="No se pudo incorporar la fotografía." }
            finally { temp?.delete(); loading=false }
        }
    }
    val pick=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if(uri!=null) importImage(uri) }
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if(pending.isNotEmpty()) { val file=File(pending); if(ok) importImage(Uri.fromFile(file),file) else file.delete(); pending="" }
    }
    Column(Modifier.animateContentSize()) {
        Preview(path)
        if(loading) LinearProgressIndicator()
        OutlinedButton(enabled=!loading,onClick={ runCatching { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.onFailure { problem="Selector no disponible." } }) { Text("Elegir fotografía") }
        OutlinedButton(enabled=!loading,onClick={
            runCatching {
                val folder=File(context.cacheDir,"camera").apply { mkdirs() }
                val file=File.createTempFile("captura-",".jpg",folder); pending=file.path
                camera.launch(FileProvider.getUriForFile(context,context.packageName+".photos",file))
            }.onFailure { if(pending.isNotBlank()) File(pending).delete(); pending=""; problem="Cámara no disponible." }
        }) { Text("Tomar fotografía") }
        if(error!=null || problem.isNotEmpty()) Text("⚠ "+(error ?: problem),color=MaterialTheme.colorScheme.error)
    }
}
