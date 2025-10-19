package com.example.practicasonidosimagenes

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null

    private val _isHorizontalY = MutableStateFlow(false)
    private val isHorizontalY = _isHorizontalY.asStateFlow()

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val y = it.values[1]
                    // Eje Y horizontal ≈ entre -1.5 y 1.5
                    val horizontalY = y > -1.5 && y < 1.5
                    lifecycleScope.launch {
                        _isHorizontalY.emit(horizontalY)
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    ImageScreen(isHorizontalY)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelSensor?.also {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorListener)
    }
}

@Composable
fun ImageScreen(isHorizontalY: kotlinx.coroutines.flow.StateFlow<Boolean>) {
    val context = LocalContext.current
    var counter by remember { mutableStateOf(3) }
    var wasHorizontal by remember { mutableStateOf(false) }
    var needsReload by remember { mutableStateOf(false) }

    val horizontal by isHorizontalY.collectAsState()

    // Rotación animada: 0° vertical, 90° horizontal
    val rotation by animateFloatAsState(
        targetValue = if (horizontal) 90f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "rotationAnim"
    )

    LaunchedEffect(horizontal) {
        if (horizontal && !wasHorizontal) {
            if (counter > 0) {
                // Disparo normal
                counter -= 1
                val sound = MediaPlayer.create(context, R.raw.disparo)
                sound.start()
                sound.setOnCompletionListener { it.release() }

                if (counter == 0) needsReload = true
            } else {
                // No hay balas → sonido de bloqueo
                val lockSound = MediaPlayer.create(context, R.raw.bloqueo)
                lockSound.start()
                lockSound.setOnCompletionListener { it.release() }
            }
        }

        // Vertical después de quedarse sin balas → recarga
        if (!horizontal && wasHorizontal && needsReload && counter == 0) {
            val reloadSound = MediaPlayer.create(context, R.raw.recarga)
            reloadSound.start()
            reloadSound.setOnCompletionListener { it.release() }

            counter = 3
            needsReload = false
        }

        wasHorizontal = horizontal
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(Color.White, Color(0xFFB3E5FC)))
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Imagen principal con rotación
        Image(
            painter = painterResource(id = R.drawable.img),
            contentDescription = "Imagen principal",
            modifier = Modifier
                .size(250.dp)
                .rotate(rotation)
                .padding(16.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Balas: $counter",
            fontSize = 24.sp,
            color = if (counter > 0) Color(0xFF0D47A1) else Color(0xFFB71C1C),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (horizontal) "Eje Y horizontal" else "Eje Y vertical",
            color = if (horizontal) Color(0xFF1B5E20) else Color(0xFF1565C0),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Hernandez Cabrera Aaron",
            fontSize = 16.sp,
            color = Color(0xFF1976D2).copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Santillan Balmaceda Dante" ,
            fontSize = 16.sp,
            color = Color(0xFF1976D2).copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}
