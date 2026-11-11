package com.example.resumen_modulos1y2

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme

class MainActivity : ComponentActivity() {

    // Estado "clásico": un campo de la Activity se pierde al recrearla,
    // por eso se guarda a mano en onSaveInstanceState y se lee en onCreate.
    private var recreaciones = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // savedInstanceState es null en el primer arranque y no-null cuando la Activity se recrea
        recreaciones = savedInstanceState?.getInt(KEY_RECREACIONES)?.plus(1) ?: 0
        Log.d(TAG, "onCreate (savedInstanceState=${savedInstanceState != null}, recreaciones=$recreaciones)")

        enableEdgeToEdge()
        setContent {
            ResumenModulos1y2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RecreacionScreen(
                        recreaciones = recreaciones,
                        // recreate() destruye y vuelve a crear la Activity igual que una rotación
                        onSimularRotacion = { recreate() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_RECREACIONES, recreaciones)
        Log.d(TAG, "onSaveInstanceState (recreaciones=$recreaciones)")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy (isChangingConfigurations=$isChangingConfigurations)")
    }

    private companion object {
        const val TAG = "MainActivity"
        const val KEY_RECREACIONES = "recreaciones"
    }
}
