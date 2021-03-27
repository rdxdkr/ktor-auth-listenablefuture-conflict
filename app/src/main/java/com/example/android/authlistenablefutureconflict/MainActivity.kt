package com.example.android.authlistenablefutureconflict

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.android.authlistenablefutureconflict.databinding.ActivityMainBinding
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)
private const val PERMISSIONS_REQUEST_CODE = 10

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startServer()
    }

    override fun onResume() {
        super.onResume()

        binding.viewFinder.post {
            if (!hasPermissions(this)) {
                requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
            } else {
                setupCamera()
            }
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

            val preview = Preview.Builder()
                    .setTargetRotation(binding.viewFinder.display.rotation)
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

            cameraProvider.unbindAll()

            try {
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.first()) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_LONG).show()
                setupCamera()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startServer() {
        embeddedServer(Netty, 8000) {
            install(DefaultHeaders)
            install(AutoHeadResponse)
            install(StatusPages)
            install(CallLogging) {
                level = Level.INFO
            }
            install(CORS) {
                anyHost()
            }
            install(ContentNegotiation) {
                json(Json {
                    encodeDefaults = true
                    coerceInputValues = true
                    prettyPrint = true
                    isLenient = true
                })
            }
            routing {
                get("/") {
                    call.respond(mapOf("Test" to "Hello World"))
                }
            }
        }.start(wait = false)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}