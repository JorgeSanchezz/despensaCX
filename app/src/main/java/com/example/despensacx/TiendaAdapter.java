package com.example.despensacx;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class TiendaAdapter extends RecyclerView.Adapter<TiendaAdapter.TiendaViewHolder> {

    public interface OnTiendaActionListener {
        void onEditar(TiendaEntity tienda);
        void onEliminar(TiendaEntity tienda);
    }

    private List<TiendaEntity> tiendas = new ArrayList<>();
    private final OnTiendaActionListener listener;

    public TiendaAdapter(OnTiendaActionListener listener) {
        this.listener = listener;
    }

    public void setTiendas(List<TiendaEntity> tiendas) {
        this.tiendas = tiendas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TiendaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tienda, parent, false);
        return new TiendaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TiendaViewHolder holder, int position) {
        holder.bind(tiendas.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return tiendas.size();
    }

    static class TiendaViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        View viewColorChip;
        ImageButton btnEditar, btnEliminar;

        public TiendaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreTienda);
            viewColorChip = itemView.findViewById(R.id.viewColorChip);
            btnEditar = itemView.findViewById(R.id.btnEditarTienda);
            btnEliminar = itemView.findViewById(R.id.btnEliminarTienda);
        }

        public void bind(TiendaEntity tienda, OnTiendaActionListener listener) {
            tvNombre.setText(tienda.getNombre());
            try {
                viewColorChip.setBackgroundColor(Color.parseColor(tienda.getColor()));
            } catch (Exception e) {
                viewColorChip.setBackgroundColor(Color.GRAY);
            }

            btnEditar.setOnClickListener(v -> listener.onEditar(tienda));
            btnEliminar.setOnClickListener(v -> listener.onEliminar(tienda));
        }
    }
}