package benrusza.gatekepper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.text
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import benrusza.gatekepper.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyMainView()
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MyMainView() {
    MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

            var text by remember { mutableStateOf("") }
            val clipboardManager = LocalClipboardManager.current
            val context = LocalContext.current

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Aplicamos el padding del Scaffold y el nuestro
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        modifier = Modifier.weight(1f),
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Introduce tu URL") },
                        trailingIcon = {
                            TextButton(onClick = {
                                clipboardManager.getText()?.let {
                                    text = it.text
                                }
                            }) {
                                Text("Pegar")
                            }
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(

                        onClick = {
                        val intent = Intent(context, Activity::class.java).apply {
                            putExtra("url", text)
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = "Ir"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(64.dp))
                Text("Para poder usar la app es necesario configurar unos ajustes")
                Spacer(modifier = Modifier.height(16.dp))
                Text("En Ajustes → Abrir de forma predeterminada → Abrir enlaces compatibles (En algunos móviles com xiaomi esta opción no esta)")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Ahi selecciona los enlaces que deseas abrir con GateKeeper")
                Spacer(modifier = Modifier.height(16.dp))
                OpenAppSettingsButton()
            }
        }
    }
}

@Composable
fun OpenAppSettingsButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        },
        modifier = modifier
    ) {
        Text(text = "Abrir ajustes de la app")
    }
}