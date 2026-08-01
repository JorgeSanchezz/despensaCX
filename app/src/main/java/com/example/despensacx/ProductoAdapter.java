package com.example.despensacx;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public static class DisplayItem {
        public int type;
        public String tiendaNombre;
        public String tiendaColor;
        public double subtotalTienda;
        public ProductoConTienda productoConTienda;

        public DisplayItem(String tiendaNombre, String tiendaColor, double subtotalTienda) {
            this.type = TYPE_HEADER;
            this.tiendaNombre = tiendaNombre;
            this.tiendaColor = tiendaColor;
            this.subtotalTienda = subtotalTienda;
        }

        public DisplayItem(ProductoConTienda productoConTienda) {
            this.type = TYPE_ITEM;
            this.productoConTienda = productoConTienda;
        }
    }

    public interface OnProductoActionListener {
        void onToggleSeleccion(ProductoEntity producto, boolean seleccionado);
        void onEditar(ProductoEntity producto);
        void onEliminar(ProductoEntity producto);
    }

    private List<DisplayItem> items = new ArrayList<>();
    private final OnProductoActionListener listener;

    public ProductoAdapter(OnProductoActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<DisplayItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header_tienda, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_producto, parent, false);
            return new ProductoViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item);
        } else if (holder instanceof ProductoViewHolder) {
            ((ProductoViewHolder) holder).bind(item.productoConTienda, listener);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreTienda, tvSubtotalTienda;
        View viewIndicatorColor;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreTienda = itemView.findViewById(R.id.tvHeaderNombreTienda);
            tvSubtotalTienda = itemView.findViewById(R.id.tvHeaderSubtotal);
            viewIndicatorColor = itemView.findViewById(R.id.viewHeaderColor);
        }

        public void bind(DisplayItem item) {
            tvNombreTienda.setText(item.tiendaNombre);
            tvSubtotalTienda.setText(String.format(Locale.getDefault(), "Subtotal: $%.2f", item.subtotalTienda));
            try {
                viewIndicatorColor.setBackgroundColor(Color.parseColor(item.tiendaColor));
            } catch (Exception e) {
                viewIndicatorColor.setBackgroundColor(Color.GRAY);
            }
        }
    }

    static class ProductoViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSeleccionado;
        TextView tvDescripcion, tvDetallePrecio, tvImporteTotal;
        ImageButton btnEditar, btnEliminar;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSeleccionado = itemView.findViewById(R.id.cbProducto);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionProducto);
            tvDetallePrecio = itemView.findViewById(R.id.tvDetallePrecio);
            tvImporteTotal = itemView.findViewById(R.id.tvImporteTotal);
            btnEditar = itemView.findViewById(R.id.btnEditarProducto);
            btnEliminar = itemView.findViewById(R.id.btnEliminarProducto);
        }

        public void bind(ProductoConTienda pct, OnProductoActionListener listener) {
            ProductoEntity p = pct.getProducto();
            tvDescripcion.setText(p.getCantidad() + "x " + p.getDescripcion());
            tvDetallePrecio.setText(String.format(Locale.getDefault(), "P.U.: $%.2f", p.getPrecio()));

            double total = p.getPrecio() * p.getCantidad();
            tvImporteTotal.setText(String.format(Locale.getDefault(), "$%.2f", total));

            // Evitar disparar listener al rebind
            cbSeleccionado.setOnCheckedChangeListener(null);
            cbSeleccionado.setChecked(p.isSeleccionado());

            if (p.isSeleccionado()) {
                tvDescripcion.setPaintFlags(tvDescripcion.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvImporteTotal.setPaintFlags(tvImporteTotal.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                itemView.setAlpha(1.0f);
            } else {
                tvDescripcion.setPaintFlags(tvDescripcion.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvImporteTotal.setPaintFlags(tvImporteTotal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                itemView.setAlpha(0.5f);
            }

            cbSeleccionado.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onToggleSeleccion(p, isChecked);
            });

            btnEditar.setOnClickListener(v -> listener.onEditar(p));
            btnEliminar.setOnClickListener(v -> listener.onEliminar(p));
        }
    }
}