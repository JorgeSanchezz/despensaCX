package com.example.despensacx.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.despensacx.R
import com.example.despensacx.model.EstadisticaModel
import com.example.despensacx.utils.FormatUtils

class MesAdapter(private val listaMeses: List<EstadisticaModel.MesModel>) :
    RecyclerView.Adapter<MesAdapter.MesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_estadistica_mes, parent, false)
        return MesViewHolder(view)
    }

    override fun onBindViewHolder(holder: MesViewHolder, position: Int) {
        val item = listaMeses[position]
        holder.tvMesNombre.text = item.mesNombre
        holder.tvTotalMes.text = FormatUtils.formatCurrency(item.totalMes)

        // Desglose por Tiendas
        holder.containerTiendas.removeAllViews()
        val gastosTienda = item.gastosPorTienda

        if (gastosTienda.isNotEmpty()) {
            for ((key, value) in gastosTienda) {
                val row = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_desglose_tienda, holder.containerTiendas, false)

                val tvNombreTienda = row.findViewById<TextView>(R.id.tvNombreTienda)
                val tvMontoTienda = row.findViewById<TextView>(R.id.tvMontoTienda)

                tvNombreTienda.text = key
                tvMontoTienda.text = FormatUtils.formatCurrency(value)

                holder.containerTiendas.addView(row)
            }
        }
    }

    override fun getItemCount(): Int = listaMeses.size

    class MesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMesNombre: TextView = itemView.findViewById(R.id.tvMesNombre)
        val tvTotalMes: TextView = itemView.findViewById(R.id.tvTotalMes)
        val containerTiendas: LinearLayout = itemView.findViewById(R.id.containerTiendas)
    }
}
