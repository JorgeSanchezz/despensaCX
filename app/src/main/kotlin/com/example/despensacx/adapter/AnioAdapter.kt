package com.example.despensacx.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.despensacx.R
import com.example.despensacx.model.EstadisticaModel
import com.example.despensacx.utils.FormatUtils

class AnioAdapter(
    private val listaAnios: List<EstadisticaModel.AnnoModel>,
    private val listener: OnAnioClickListener
) : RecyclerView.Adapter<AnioAdapter.AnioViewHolder>() {

    interface OnAnioClickListener {
        fun onAnioClick(anioModel: EstadisticaModel.AnnoModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_estadistica_anio, parent, false)
        return AnioViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnioViewHolder, position: Int) {
        val item = listaAnios[position]
        holder.tvAnio.text = item.anio
        holder.tvTotalAnio.text = FormatUtils.formatCurrency(item.totalAnio)

        holder.itemView.setOnClickListener {
            listener.onAnioClick(item)
        }
    }

    override fun getItemCount(): Int = listaAnios.size

    class AnioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAnio: TextView = itemView.findViewById(R.id.tvAnio)
        val tvTotalAnio: TextView = itemView.findViewById(R.id.tvTotalAnio)
    }
}
