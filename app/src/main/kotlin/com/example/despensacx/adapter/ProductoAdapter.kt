package com.example.despensacx.adapter

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.despensacx.R
import com.example.despensacx.data.ProductoEntity
import com.example.despensacx.model.ProductoConTienda
import com.example.despensacx.utils.FormatUtils
import java.util.*

class ProductoAdapter(private val listener: OnProductoActionListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    data class DisplayItem(
        val type: Int,
        val tiendaNombre: String? = null,
        val tiendaColor: String? = null,
        val subtotalTienda: Double = 0.0,
        val productoConTienda: ProductoConTienda? = null
    ) {
        constructor(tiendaNombre: String, tiendaColor: String, subtotalTienda: Double) : 
            this(TYPE_HEADER, tiendaNombre, tiendaColor, subtotalTienda)
        
        constructor(productoConTienda: ProductoConTienda) : 
            this(TYPE_ITEM, productoConTienda = productoConTienda)
    }

    interface OnProductoActionListener {
        fun onToggleSeleccion(producto: ProductoEntity, seleccionado: Boolean)
        fun onEditar(producto: ProductoEntity)
        fun onEliminar(producto: ProductoEntity)
    }

    private var items: List<DisplayItem> = emptyList()

    fun setItems(items: List<DisplayItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = items[position].type

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_header_tienda, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
            ProductoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is HeaderViewHolder -> holder.bind(item)
            is ProductoViewHolder -> item.productoConTienda?.let { holder.bind(it, listener) }
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombreTienda: TextView = itemView.findViewById(R.id.tvHeaderNombreTienda)
        private val tvSubtotalTienda: TextView = itemView.findViewById(R.id.tvHeaderSubtotal)
        private val viewIndicatorColor: View = itemView.findViewById(R.id.viewHeaderColor)

        fun bind(item: DisplayItem) {
            tvNombreTienda.text = item.tiendaNombre
            tvSubtotalTienda.text = itemView.context.getString(R.string.subtotal_tienda, FormatUtils.formatCurrency(item.subtotalTienda))
            try {
                viewIndicatorColor.setBackgroundColor(Color.parseColor(item.tiendaColor))
            } catch (e: Exception) {
                viewIndicatorColor.setBackgroundColor(Color.GRAY)
            }
        }
    }

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbSeleccionado: CheckBox = itemView.findViewById(R.id.cbProducto)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionProducto)
        private val tvDetallePrecio: TextView = itemView.findViewById(R.id.tvDetallePrecio)
        private val tvImporteTotal: TextView = itemView.findViewById(R.id.tvImporteTotal)
        private val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditarProducto)
        private val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarProducto)

        fun bind(pct: ProductoConTienda, listener: OnProductoActionListener) {
            val p = pct.producto
            tvDescripcion.text = itemView.context.getString(R.string.cantidad_descripcion, p.cantidad, p.descripcion)
            tvDetallePrecio.text = String.format(Locale.getDefault(), "P.U.: $%,.2f", p.precio)

            val total = p.precio * p.cantidad
            tvImporteTotal.text = String.format(Locale.getDefault(), "$%,.2f", total)

            cbSeleccionado.setOnCheckedChangeListener(null)
            cbSeleccionado.isChecked = p.seleccionado

            if (p.seleccionado) {
                tvDescripcion.paintFlags = tvDescripcion.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvImporteTotal.paintFlags = tvImporteTotal.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                itemView.alpha = 1.0f
            } else {
                tvDescripcion.paintFlags = tvDescripcion.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvImporteTotal.paintFlags = tvImporteTotal.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                itemView.alpha = 0.5f
            }

            cbSeleccionado.setOnCheckedChangeListener { _, isChecked ->
                listener.onToggleSeleccion(p, isChecked)
            }

            btnEditar.setOnClickListener { listener.onEditar(p) }
            btnEliminar.setOnClickListener { listener.onEliminar(p) }
        }
    }
}
