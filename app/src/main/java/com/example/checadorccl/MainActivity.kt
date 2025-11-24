package com.example.checadorccl

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        // Forzar color negro en barra de estado
        drawerLayout.setStatusBarBackgroundColor(android.graphics.Color.BLACK)

        val navView = findViewById<NavigationView>(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        updateNavHeader(navView)

        if (savedInstanceState == null) {
            // Revisamos si la actividad se abrió desde una notificación push
            val fragmentToOpen = intent.getStringExtra("OPEN_FRAGMENT")

            if (fragmentToOpen == "NOTIFICATIONS") {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, NotificationsFragment())
                    .commit()
                navView.setCheckedItem(R.id.nav_notifications)
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, AttendanceFragment())
                    .commit()
                navView.setCheckedItem(R.id.nav_records)
            }
        }
    }

    private fun updateNavHeader(navView: NavigationView) {
        val headerView = navView.getHeaderView(0)
        val txtName = headerView.findViewById<TextView>(R.id.nav_header_name)
        val txtPosition = headerView.findViewById<TextView>(R.id.nav_header_email)
        val imgProfile = headerView.findViewById<ImageView>(R.id.nav_header_profile_image)

        val prefs = getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)
        val nombre = prefs.getString("USER_NAME", "Usuario")
        val puesto = prefs.getString("USER_POSITION", "Empleado")
        val fotoBase64 = prefs.getString("USER_PHOTO", null)

        txtName.text = nombre
        txtPosition.text = puesto

        if (imgProfile != null) {
            imgProfile.clearColorFilter()
            if (!fotoBase64.isNullOrEmpty()) {
                try {
                    val pureBase64 = if (fotoBase64.contains(",")) fotoBase64.substringAfter(",") else fotoBase64
                    val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                    val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                    if (decodedBitmap != null) {
                        imgProfile.setImageBitmap(decodedBitmap)
                        imgProfile.setPadding(0,0,0,0)
                        imgProfile.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                } catch (e: Exception) {
                    imgProfile.setImageResource(R.drawable.ic_person)
                }
            } else {
                imgProfile.setImageResource(R.drawable.ic_person)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_records -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, AttendanceFragment())
                    .commit()
            }
            R.id.nav_notifications -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, NotificationsFragment())
                    .commit()
            }
            R.id.nav_calendar -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, CalendarFragment())
                    .commit()
            }
            R.id.nav_sedes -> {
                startActivity(Intent(this, SedesActivity::class.java))
            }
            R.id.nav_request_day -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, PermitRequestFragment())
                    .addToBackStack(null)
                    .commit()
            }
            R.id.nav_logout -> {
                performLogout()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun performLogout() {
        val sharedPref = getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)

        // --- CORRECCIÓN: BORRAR SOLO DATOS DE SESIÓN ---
        sharedPref.edit {
            remove("ACCESS_TOKEN")
            remove("REFRESH_TOKEN")
            remove("USER_NAME")
            remove("USER_POSITION")
            remove("USER_PHOTO")
            // "SAVED_USER" y "SAVED_PASS" SE MANTIENEN
        }

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
    }
}