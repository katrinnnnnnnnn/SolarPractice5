package ua.kpi.solarpractice5.ui

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.solarpractice5.notify.Alerts
import ua.kpi.solarpractice5.viewmodel.MonitoringState
import ua.kpi.solarpractice5.viewmodel.SolarViewModel
import kotlin.math.roundToInt

private val purple = Color(0xFF6C4DA2)
@Composable
fun MainScreen(vm: SolarViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var page by remember { mutableIntStateOf(17) }
    Column(Modifier.fillMaxSize()) {
        Surface(color=Color(0xFFF0E8F7)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Text("SolarPractice · ПР №5", style=MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    (17..20).forEach { n ->
                        if(n==page) Button(onClick={page=n}, modifier=Modifier.weight(1f),contentPadding=PaddingValues(2.dp)) { Text("ПЗ $n") }
                        else OutlinedButton(onClick={page=n},modifier=Modifier.weight(1f),contentPadding=PaddingValues(2.dp)) { Text("ПЗ $n") }
                    }
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
            when(page) {
                17 -> SensorsScreen(vm,state)
                18 -> NotificationsScreen(vm,state)
                19 -> ChartsScreen(vm,state)
                else -> CustomScreen(vm,state)
            }
        }
    }
}
@Composable private fun Section(title: String, note: String) {
    Text(title,style=MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(7.dp))
    Text(note,style=MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(12.dp))
}
@Composable private fun Metric(title:String,value:String,extra:String="") {
    Card(Modifier.fillMaxWidth().padding(vertical=3.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFEAE4EF))) {
        Column(Modifier.padding(14.dp)) { Text(title,fontWeight=FontWeight.SemiBold);Text(value,style=MaterialTheme.typography.headlineSmall); if(extra.isNotEmpty())Text(extra,style=MaterialTheme.typography.bodySmall) }
    }
}
@Composable fun SensorsScreen(vm:SolarViewModel,state:MonitoringState) {
    var includeAccel by remember { mutableStateOf(true) }
    var includeLight by remember { mutableStateOf(true) }
    var threshold by remember { mutableFloatStateOf(1100f) }
    Section("ПЗ 17 · Моніторинг сенсорів","Акселерометр та освітленість — датчики телефону. Електричні параметри — навчальна симуляція.")
    Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(includeAccel,{includeAccel=it});Text("Акселерометр");Checkbox(includeLight,{includeLight=it});Text("Світло") }
    if(includeAccel) Metric("Прискорення","${state.readings.acceleration?.let { "%.2f".format(it) } ?: "Недоступно"} м/с²",if(vm.hasAccelerometer) "Сенсор телефону" else "Сенсор відсутній")
    if(includeLight) Metric("Освітленість","${state.readings.light?.let { "%.1f".format(it) } ?: "Недоступно"} лк",if(vm.hasLight) "Сенсор телефону" else "Сенсор відсутній")
    Metric("Потужність станції (модель)","${state.simulated.power.roundToInt()} Вт")
    Text("Поріг сповіщення: ${threshold.roundToInt()} Вт")
    Slider(value=threshold,onValueChange={threshold=it},valueRange=500f..2000f)
    Text(if(state.simulated.power>threshold)"Поріг перевищено" else "Показники в межах порогу")
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Button(onClick=vm::start,enabled=!state.running) {Text("Старт")}
        OutlinedButton(onClick=vm::stop,enabled=state.running){Text("Стоп")}
    }
}
@Composable fun NotificationsScreen(vm:SolarViewModel,state:MonitoringState) {
    val ctx=LocalContext.current
    var selected by remember { mutableStateOf("Потужність") }
    var sent by remember { mutableIntStateOf(0) }
    val launcher= rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
    Section("ПЗ 18 · Сповіщення","Локальні сповіщення з каналом та дією «Підтвердити». Серверне FCM тут не налаштовано.")
    Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        listOf("Потужність","Температура").forEach { name ->
            if(name==selected) Button(onClick={selected=name}){Text(name)} else OutlinedButton(onClick={selected=name}){Text(name)}
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(onClick={
        if(Build.VERSION.SDK_INT>=33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else {
            sent++
            Alerts.show(ctx,100+sent,"Сонячна станція: $selected", "Повідомлення №$sent · відкрийте або підтвердьте")
            vm.recordEvent("Створено повідомлення №$sent: $selected")
        }
    }) { Text("Надіслати локальне сповіщення") }
    Text("Відправлено в поточному сеансі: $sent")
    val count=ctx.getSharedPreferences("solar_alerts",android.content.Context.MODE_PRIVATE).getInt("ack_count",0)
    Text("Підтверджено на пристрої: $count (оновлюється при повторному відкритті екрана)")
    Spacer(Modifier.height(12.dp))
    Text("Події:",fontWeight=FontWeight.Bold)
    state.eventLog.forEach{ Text("• $it") }
}
@Composable fun ChartsScreen(vm:SolarViewModel,state:MonitoringState) {
    var showPower by remember { mutableStateOf(true) }
    Section("ПЗ 19 · Графіки і діаграми","Графік потужності оновлюється з навчальної моделі щосекунди.")
    Row(verticalAlignment=Alignment.CenterVertically) { Switch(checked=showPower,onCheckedChange={showPower=it}); Spacer(Modifier.width(8.dp)); Text("Показувати потужність") }
    if(showPower) {
        Metric("Поточна потужність", "${state.simulated.power.roundToInt()} Вт")
        LineGraph(state.history,Modifier.fillMaxWidth().height(210.dp))
    }
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Button(onClick=vm::start,enabled=!state.running){Text("Старт даних")}
        OutlinedButton(onClick=vm::stop,enabled=state.running){Text("Стоп")}
    }
    Text("Дані симульовані: фізичні датчики напруги і струму до Android не підключені.")
}
@Composable private fun LineGraph(values:List<Float>,modifier:Modifier=Modifier) {
    Card(modifier,colors=CardDefaults.cardColors(containerColor=Color(0xFFF0EBF6))) {
        Canvas(Modifier.fillMaxSize().padding(20.dp)) {
            drawLine(Color.Gray.copy(alpha=0.4f), androidx.compose.ui.geometry.Offset(0f,size.height),androidx.compose.ui.geometry.Offset(size.width,size.height),strokeWidth=2f)
            if(values.size>1) {
                val min=values.minOrNull() ?: 0f
                val max=values.maxOrNull() ?: 1f
                val span=(max-min).coerceAtLeast(1f)
                val path=Path()
                values.forEachIndexed{ index,v ->
                    val x=index.toFloat()/(values.size-1)*size.width
                    val y=size.height-(v-min)/span*size.height
                    if(index==0)path.moveTo(x,y) else path.lineTo(x,y)
                }
                drawPath(path,purple,style=Stroke(width=5f,cap=StrokeCap.Round))
            }
        }
    }
}
@Composable fun CustomScreen(vm:SolarViewModel,state:MonitoringState) {
    var voltageMax by remember { mutableFloatStateOf(250f) }
    Section("ПЗ 20 · Власні компоненти","Компонент SensorGauge із параметрами та кольоровою індикацією стану.")
    SensorGauge("Напруга (симуляція)",state.simulated.voltage,voltageMax,"В")
    SensorGauge("Температура (симуляція)",state.simulated.temperature,50f,"°C")
    SensorGauge("Освітленість (сенсор)",state.readings.light ?: 0f,1200f,"лк")
    Text("Максимум шкали напруги: ${voltageMax.roundToInt()} В")
    Slider(value=voltageMax,onValueChange={voltageMax=it},valueRange=220f..300f)
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(onClick=vm::start,enabled=!state.running){Text("Старт")}
        OutlinedButton(onClick=vm::stop,enabled=state.running){Text("Стоп")}
    }
}
@Composable private fun SensorGauge(title:String,value:Float,max:Float,unit:String) {
    val fraction=(value/max).coerceIn(0f,1f)
    val color=if(fraction>0.9f)Color(0xFFC45C47) else purple
    Card(Modifier.fillMaxWidth().padding(vertical=7.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title,fontWeight=FontWeight.SemiBold)
            Text("${"%.1f".format(value)} $unit",style=MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress={fraction},modifier=Modifier.fillMaxWidth().height(12.dp),color=color)
        }
    }
}
