package cl.huerto

import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import cl.huerto.domain.*
import cl.huerto.remote.*
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.huerto.ui.*
import cl.huerto.ui.theme.GardenTheme

class MainActivity : ComponentActivity() {
    private val vm: HuertoViewModel by viewModels { viewModelFactory { initializer { HuertoViewModel.create(application) } } }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GardenTheme { App(vm) } }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun App(vm: HuertoViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val errors by vm.errors.collectAsStateWithLifecycle()
    val snack=remember { SnackbarHostState() }
    LaunchedEffect(message) { if(message.isNotBlank()) { snack.showSnackbar(message); vm.message.value="" } }
    key(state.user?.id) {
        val nav=rememberNavController()
        val active by nav.currentBackStackEntryAsState()
        val go: (String)->Unit={ nav.navigate(it) { launchSingleTop=true } }
        val back: ()->Unit={nav.popBackStack()}
        val user=state.user
        val g=state.garden
        val admin=user?.rol==ADMIN
        Scaffold(bottomBar={
            val route=active?.destination?.route
            if(user!=null && route!=null && !route.startsWith("form/")) {
                NavigationBar(containerColor=MaterialTheme.colorScheme.background) {
                    for ((target,label,icon) in listOf(Triple("inicio","Inicio",Icons.Default.Home),Triple("tareas","Tareas",Icons.Default.List),Triple("sectores","Huerto",Icons.Default.Place),Triple("avisos","Avisos",Icons.Default.DateRange))) {
                        NavigationBarItem(selected=route==target || (target=="sectores" && (route.startsWith("sector/") || route.startsWith("cultivo/"))) || (target=="tareas" && route.startsWith("tarea/")),icon={Icon(icon,null)},label={Text(label)},onClick={
                            if(route!=target) nav.navigate(target) { popUpTo("inicio"); launchSingleTop=true }
                        })
                    }
                }
            }
        },snackbarHost={SnackbarHost(snack)},topBar={
            if(user!=null) TopAppBar(title={Text("Huerto escolar")},navigationIcon={
                if(user!=null && active?.destination?.route !in listOf(null,"inicio")) TextButton(onClick=back){Text("Volver")}
            },actions={if(user!=null) TextButton(enabled=!busy,onClick=vm::logout){Text("Salir")}})
        }) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                if(!state.ready) CircularProgressIndicator()
                else if(user==null) {
                    var name by rememberSaveable { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(MaterialTheme.colorScheme.background)) {
                        Box(Modifier.fillMaxWidth().height(280.dp)) {
                            Image(painterResource(R.drawable.cosecha),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
                            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent,MaterialTheme.colorScheme.background))))
                            Text("Huerto\nescolar",Modifier.align(Alignment.BottomStart).padding(24.dp),style=MaterialTheme.typography.displayLarge.copy(fontFamily=androidx.compose.ui.text.font.FontFamily.Serif),color=Color.White)
                        }
                        Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("Ingresar al taller",style=MaterialTheme.typography.headlineMedium)

                        Field("Cuenta",name,errors["Cuenta"]){name=it}
                        Field("Clave",password,null,true){password=it}
                        Button(modifier=Modifier.fillMaxWidth().height(54.dp),enabled=!busy,onClick={vm.login(name,password)}){Text("Iniciar sesión")}
                        }
                    }
                } else NavHost(nav,"inicio") {
                    composable("inicio"){Home(user,g,go)}
                    composable("tiempo"){WeatherScreen()}
                    for(modulo in Modulo.values()) {
                        composable(modulo.ruta){if(admin) {
                            val taller=viewModel<TallerViewModel> { TallerViewModel(modulo) }
                            TallerScreen(taller)
                        }}
                    }
                    composable("sectores"){Sectors(g,admin,go)}
                    composable("tareas"){Tasks(g,admin,false,go)}
                    composable("revisiones"){if(admin) Tasks(g,true,true,go)}
                    composable("reportes"){Reports(g,go)}
                    composable("historial"){History(g,go)}
                    composable("avisos"){Alerts(g,go)}
                    composable("sensores"){if(admin) Sensors(g)}
                    for(route in listOf("sector","cultivo","tarea","reporte")) {
                        composable("$route/{id}",arguments=listOf(navArgument("id"){type=NavType.LongType})) { entry ->
                            val id=entry.arguments!!.getLong("id")
                            when(route){
                                "sector" -> SectorDetail(id,g,admin,go){vm.deleteSector(id,back)}
                                "cultivo" -> CropDetail(id,g,admin,go)
                                "tarea" -> TaskDetail(id,g,admin,go){vm.start(id)}
                                "reporte" -> ReportDetail(id,g,admin,go)
                            }
                        }
                    }
                    composable("form/{kind}/{id}?sector={sector}",arguments=listOf(navArgument("id"){type=NavType.LongType},navArgument("sector"){type=NavType.LongType; defaultValue=0L})) { entry ->
                        val form=Form.valueOf(entry.arguments!!.getString("kind")!!)
                        val id=entry.arguments!!.getLong("id")
                        val allowed=when(form){
                            Form.ENTREGA -> !admin && g.tarea.any { it.id==id && it.estado==WORKING }
                            Form.REPORTE -> true
                            else -> admin
                        }
                        if(allowed) Editor(form,id,g,vm,entry.arguments!!.getLong("sector"),back) else Empty("Acceso restringido.")
                    }
                }
            }
        }
    }
}
