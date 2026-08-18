package com.example.despensacx.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.despensacx.R
import com.example.despensacx.data.TiendaEntity

class TiendaAdapter(private val listener: OnTiendaActionListener) :
    RecyclerView.Adapter<TiendaAdapter.TiendaViewHolder>() {

    interface OnTiendaActionListener {
        fun onEditar(tienda: TiendaEntity)
        fun onEliminar(tienda: TiendaEntity)
    }

    private var tiendas: List<TiendaEntity> = emptyList()

    fun setTiendas(tiendas: List<TiendaEntity>) {
        this.tiendas = tiendas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TiendaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tienda, parent, false)
        return TiendaViewHolder(view)
    }

    override fun onBindViewHolder(holder: TiendaViewHolder, position: Int) {
        holder.bind(tiendas[position], listener)
    }

    override fun getItemCount(): Int = tiendas.size

    class TiendaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreTienda)
        private val viewColorChip: View = itemView.findViewById(R.id.viewColorChip)
        private val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditarTienda)
        private val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarTienda)

        fun bind(tienda: TiendaEntity, listener: OnTiendaActionListener) {
            tvNombre.text = tienda.nombre
            try {
                viewColorChip.setBackgroundColor(Color.parseColor(tienda.color))
            } catch (e: Exception) {
                viewColorChip.setBackgroundColor(Color.GRAY)
            }

            btnEditar.setOnClickListener { listener.onEditar(tienda) }
            btnEliminar.setOnClickListener { listener.onEliminar(tienda) }
        }
    }
}
