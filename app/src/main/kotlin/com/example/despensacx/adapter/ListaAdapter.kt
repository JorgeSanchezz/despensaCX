package com.example.despensacx.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.despensacx.R
import com.example.despensacx.data.ListaEntity
import java.util.*

class ListaAdapter(private val listener: OnListaClickListener) :
    RecyclerView.Adapter<ListaAdapter.ListaViewHolder>() {

    interface OnListaClickListener {
        fun onClick(lista: ListaEntity)
        fun onLongClick(lista: ListaEntity, view: View)
        fun onEditar(lista: ListaEntity)
        fun onDuplicar(lista: ListaEntity)
        fun onEliminar(lista: ListaEntity)
    }

    private var listas: List<ListaEntity> = emptyList()

    fun setListas(listas: List<ListaEntity>) {
        this.listas = listas
        notifyDataSetChanged()
    }

    fun getListaAt(position: Int): ListaEntity = listas[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lista, parent, false)
        return ListaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListaViewHolder, position: Int) {
        val lista = listas[position]
        holder.bind(lista, listener)
    }

    override fun getItemCount(): Int = listas.size

    class ListaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombreLista)
        private val tvFechaCreacion: TextView = itemView.findViewById(R.id.tvFechaCreacion)
        private val tvUltimaModificacion: TextView = itemView.findViewById(R.id.tvUltimaModificacion)
        private val tvPresupuesto: TextView = itemView.findViewById(R.id.tvPresupuesto)
        private val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditarLista)
        private val btnDuplicar: ImageButton = itemView.findViewById(R.id.btnDuplicarLista)
        private val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarLista)

        fun bind(lista: ListaEntity, listener: OnListaClickListener) {
            val title = lista.nombre + if (lista.archivada) itemView.context.getString(R.string.archivada_suffix) else ""
            tvNombre.text = title
            tvFechaCreacion.text = itemView.context.getString(R.string.creado_con_fecha, lista.fechaCreacion)
            tvUltimaModificacion.text = "Modificado: ${lista.fechaModificacion}"

            if (lista.presupuestoMaximo > 0) {
                tvPresupuesto.visibility = View.VISIBLE
                tvPresupuesto.text = String.format(Locale.getDefault(), "Presupuesto: $%,.2f MXN", lista.presupuestoMaximo)
            } else {
                tvPresupuesto.visibility = View.GONE
            }

            itemView.setOnClickListener { listener.onClick(lista) }
            itemView.setOnLongClickListener {
                listener.onLongClick(lista, it)
                true
            }

            btnEditar.setOnClickListener { listener.onEditar(lista) }
            btnDuplicar.setOnClickListener { listener.onDuplicar(lista) }
            btnEliminar.setOnClickListener { listener.onEliminar(lista) }
        }
    }
}
