package com.example.checadorccl

import android.location.Location
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.checadorccl.utils.LocationHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class SedesActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var txtNearest: TextView
    private lateinit var recyclerSedes: RecyclerView
    private lateinit var locationHelper: LocationHelper

    // Guardamos referencia a los marcadores para poder abrir su info window
    private val markersMap = mutableMapOf<String, Marker?>()

    // Datos Fijos (Data Class debe ser pública o estar fuera si se usa en Adapter externo)
    data class SedeData(val nombre: String, val lat: Double, val lng: Double)

    private val listaSedes = listOf(
        SedeData("Zapopan", 20.773554047581225, -103.45487896622687),
        SedeData("Puerto Vallarta", 20.671918288678185, -105.25118774314542),
        SedeData("Lagos de Moreno", 21.37179065673159, -101.92242834103243),
        SedeData("Ocotlan", 20.366957391023114, -102.77005553473813),
        SedeData("Zapotlan el Grande", 19.646749163540985, -103.50563059412207),
        SedeData("Autlan de Navarro", 19.705681111950565, -104.34542983281973),
        SedeData("Colotlan", 22.099188233702726, -103.2696526588588),
        SedeData("Tlajomulco", 20.657176823404974, -103.34839579336644),
        SedeData("Ameca", 20.539337828989805, -104.04256199120103),
        SedeData("Tepatitlan", 20.793374077905895, -102.76933507753606)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sedes)

        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.BLACK

        txtNearest = findViewById(R.id.txt_nearest_sede)
        recyclerSedes = findViewById(R.id.recycler_sedes)
        locationHelper = LocationHelper(this)

        // Configurar Lista
        recyclerSedes.layoutManager = LinearLayoutManager(this)
        recyclerSedes.adapter = SedesAdapter(listaSedes) { sede ->
            // ACCIÓN AL TOCAR UN ELEMENTO DE LA LISTA
            moverCamaraASede(sede)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        markersMap.clear()

        val boundsBuilder = LatLngBounds.Builder()

        // 1. Poner Marcadores
        for (sede in listaSedes) {
            val pos = LatLng(sede.lat, sede.lng)
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(sede.nombre)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            // Guardar referencia para abrirlo luego
            markersMap[sede.nombre] = marker
            boundsBuilder.include(pos)
        }

        // 2. Ubicación Usuario
        try {
            mMap.isMyLocationEnabled = true
            locationHelper.getCurrentLocation { lat, lng ->
                if (lat != 0.0 && lng != 0.0) {
                    calcularSedeMasCercana(lat, lng)
                } else {
                    txtNearest.text = "Ubicación no disponible"
                    // Si no hay GPS, mostrar todo el mapa de Jalisco
                    try {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
                    } catch (e: Exception) {}
                }
            }
        } catch (e: SecurityException) {
            txtNearest.text = "Sin permisos"
        }
    }

    private fun moverCamaraASede(sede: SedeData) {
        if (::mMap.isInitialized) {
            val pos = LatLng(sede.lat, sede.lng)
            // Zoom animado a la sede
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))

            // Mostrar el globito con el nombre en el mapa
            markersMap[sede.nombre]?.showInfoWindow()
        }
    }

    private fun calcularSedeMasCercana(userLat: Double, userLng: Double) {
        var sedeMasCercana: SedeData? = null
        var distanciaMinima = Float.MAX_VALUE

        for (sede in listaSedes) {
            val resultados = FloatArray(1)
            Location.distanceBetween(userLat, userLng, sede.lat, sede.lng, resultados)
            val distancia = resultados[0]

            if (distancia < distanciaMinima) {
                distanciaMinima = distancia
                sedeMasCercana = sede
            }
        }

        if (sedeMasCercana != null) {
            val distTxt = if (distanciaMinima > 1000) String.format("%.1f km", distanciaMinima / 1000) else "${distanciaMinima.toInt()} m"
            txtNearest.text = "${sedeMasCercana.nombre} ($distTxt)"

            // Opcional: Centrar mapa en usuario y sede cercana al inicio
            val builder = LatLngBounds.Builder()
            builder.include(LatLng(userLat, userLng))
            builder.include(LatLng(sedeMasCercana.lat, sedeMasCercana.lng))
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 200))
            } catch (e: Exception){}
        }
    }
}