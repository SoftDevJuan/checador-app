package com.example.checadorccl

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.checadorccl.api.ApiClient
import com.example.checadorccl.api.models.GoogleLoginRequest
import com.example.checadorccl.api.models.LoginRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    // 1. Launcher para Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                performGoogleLogin(idToken)
            } else {
                Toast.makeText(this, "Error: Google no devolvió token", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("LOGIN_GOOGLE", "Fallo Google Sign-In code:${e.statusCode}", e)
            Toast.makeText(this, "Fallo Google Sign-In", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Launcher para Permisos
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.entries.all { it.value || it.key !in CRITICAL_PERMISSIONS }
        if (!allGranted) {
            Toast.makeText(this, "Se requieren permisos para el correcto funcionamiento.", Toast.LENGTH_LONG).show()
        }
    }

    private val CRITICAL_PERMISSIONS = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase manualmente si falla el auto-init
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.e("LOGIN_FCM", "Error init Firebase: ${e.message}")
        }

        // Si ya hay sesión, entrar directo
        if (getToken() != null) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        // Forzar barra negra
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.BLACK

        requestPermissions()
        obtenerTokenFirebase()

        // CONFIGURAR GOOGLE CLIENT
        // Asegúrate de que este ID sea el de la Web (OAuth 2.0 Client ID)
        val webClientId = "mikey"

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Vincular Vistas
        val usernameEditText = findViewById<TextInputEditText>(R.id.edit_text_username)
        val passwordEditText = findViewById<TextInputEditText>(R.id.edit_text_password)
        val loginButton = findViewById<Button>(R.id.btn_login)
        val googleButton = findViewById<Button>(R.id.btn_google_signin)

        // Llenar credenciales guardadas si existen
        val prefs = getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)
        usernameEditText.setText(prefs.getString("SAVED_USER", ""))
        passwordEditText.setText(prefs.getString("SAVED_PASS", ""))

        // Listeners
        loginButton.setOnClickListener {
            val user = usernameEditText.text.toString().trim()
            val pass = passwordEditText.text.toString().trim()
            performLogin(user, pass)
        }

        googleButton.setOnClickListener {
            // --- CORRECCIÓN: FORZAR CIERRE DE SESIÓN DE GOOGLE ANTES DE ENTRAR ---
            // Esto borra la "memoria" de la última cuenta usada y fuerza el selector
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }
    }

    // --- LÓGICA LOGIN CON GOOGLE ---
    private fun performGoogleLogin(idToken: String) {
        val btnGoogle = findViewById<Button>(R.id.btn_google_signin)
        btnGoogle.isEnabled = false
        btnGoogle.text = "Verificando..."

        lifecycleScope.launch {
            try {
                val response = ApiClient.service.googleLogin(GoogleLoginRequest(idToken))

                if (response.isSuccessful && response.body() != null) {
                    val loginData = response.body()!!

                    // Guardar sesión
                    saveSession(
                        access = loginData.accessToken,
                        refresh = loginData.refreshToken,
                        nombre = loginData.empleado?.nombreCompleto,
                        puesto = loginData.empleado?.puesto,
                        fotoUrl = loginData.empleado?.fotoUrl
                    )

                    // Registrar dispositivo para notificaciones
                    registrarDispositivo(loginData.accessToken)
                } else {
                    Toast.makeText(this@LoginActivity, "Cuenta Google no vinculada o inactiva", Toast.LENGTH_LONG).show()
                    btnGoogle.isEnabled = true
                    btnGoogle.text = "Iniciar sesión con Google"
                }
            } catch (e: Exception) {
                Log.e("LOGIN_GOOGLE", "Error API", e)
                Toast.makeText(this@LoginActivity, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
                btnGoogle.isEnabled = true
                btnGoogle.text = "Iniciar sesión con Google"
            }
        }
    }

    // --- LÓGICA LOGIN NORMAL ---
    private fun performLogin(username: String, contrasena: String) {
        if (username.isBlank() || contrasena.isBlank()) {
            Toast.makeText(this, "Ingresa usuario y contraseña.", Toast.LENGTH_SHORT).show()
            return
        }

        val btnLogin = findViewById<Button>(R.id.btn_login)
        btnLogin.isEnabled = false
        btnLogin.text = "Cargando..."

        lifecycleScope.launch {
            try {
                val request = LoginRequest(username = username, password = contrasena)
                val response = ApiClient.service.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginData = response.body()!!

                    // Guardar credenciales para autocompletar futuro
                    val prefs = getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)
                    prefs.edit {
                        putString("SAVED_USER", username)
                        putString("SAVED_PASS", contrasena)
                    }

                    // Guardar sesión activa
                    saveSession(
                        access = loginData.accessToken,
                        refresh = loginData.refreshToken,
                        nombre = loginData.empleado?.nombreCompleto,
                        puesto = loginData.empleado?.puesto,
                        fotoUrl = loginData.empleado?.fotoUrl
                    )

                    registrarDispositivo(loginData.accessToken)

                } else {
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_LONG).show()
                    btnLogin.isEnabled = true
                    btnLogin.text = "Iniciar Sesión"
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error de conexión", Toast.LENGTH_LONG).show()
                e.printStackTrace()
                btnLogin.isEnabled = true
                btnLogin.text = "Iniciar Sesión"
            }
        }
    }

    // --- UTILIDADES ---

    private fun requestPermissions() {
        val permissionsToRequest = CRITICAL_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (permissionsToRequest.isNotEmpty()) requestPermissionLauncher.launch(permissionsToRequest)
    }

    private fun obtenerTokenFirebase() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("LOGIN_FCM", "Falló token FCM local", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                // Guardamos token temporalmente
                getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE).edit { putString("FCM_TOKEN", token) }
            }
        } catch (e: Exception) {
            Log.e("LOGIN_FCM", "Error FCM", e)
        }
    }

    private fun registrarDispositivo(accessToken: String) {
        // Pedimos el token fresco a Firebase antes de enviarlo
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                // Si falla, entramos igual a la app pero sin notificaciones
                finalizarLogin()
                return@addOnCompleteListener
            }

            val fcmToken = task.result

            // Enviamos al servidor en segundo plano
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    ApiClient.service.registerDevice("Bearer $accessToken", fcmToken)
                } catch (e: Exception) {
                    Log.e("LOGIN_FCM", "Error enviando token al servidor", e)
                }

                // Pase lo que pase, entramos a la app
                withContext(Dispatchers.Main) {
                    finalizarLogin()
                }
            }
        }
    }

    private fun finalizarLogin() {
        Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
        goToMain()
    }

    private fun saveSession(access: String, refresh: String, nombre: String?, puesto: String?, fotoUrl: String?) {
        val sharedPref = getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("ACCESS_TOKEN", access)
            putString("REFRESH_TOKEN", refresh)
            putString("USER_NAME", nombre ?: "Usuario")
            putString("USER_POSITION", puesto ?: "Empleado")
            putString("USER_PHOTO", fotoUrl)
            apply()
        }
    }

    private fun getToken(): String? {
        val sharedPref = getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("ACCESS_TOKEN", null)
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}