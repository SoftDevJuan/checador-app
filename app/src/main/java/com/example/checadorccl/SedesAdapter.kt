package com.example.checadorccl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.checadorccl.SedesActivity.SedeData

class SedesAdapter(
    private val lista: List<SedeData>,
    private val onSedeClick: (SedeData) -> Unit
) : RecyclerView.Adapter<SedesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNombre: TextView = view.findViewById(R.id.txt_nombre_sede)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sede, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sede = lista[position]
        holder.txtNombre.text = sede.nombre

        holder.itemView.setOnClickListener {
            onSedeClick(sede)
        }
    }

    override fun getItemCount() = lista.size
}