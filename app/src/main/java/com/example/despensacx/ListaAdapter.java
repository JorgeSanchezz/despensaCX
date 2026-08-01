package com.example.despensacx;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListaAdapter extends RecyclerView.Adapter<ListaAdapter.ListaViewHolder> {

    public interface OnListaClickListener {
        void onClick(ListaEntity lista);
        void onLongClick(ListaEntity lista, View view);
        void onEditar(ListaEntity lista);
        void onDuplicar(ListaEntity lista);
        void onEliminar(ListaEntity lista);
    }

    private List<ListaEntity> listas = new ArrayList<>();
    private final OnListaClickListener listener;

    public ListaAdapter(OnListaClickListener listener) {
        this.listener = listener;
    }

    public void setListas(List<ListaEntity> listas) {
        this.listas = listas;
        notifyDataSetChanged();
    }

    public ListaEntity getListaAt(int position) {
        return listas.get(position);
    }

    @NonNull
    @Override
    public ListaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lista, parent, false);
        return new ListaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListaViewHolder holder, int position) {
        ListaEntity lista = listas.get(position);
        holder.bind(lista, listener);
    }

    @Override
    public int getItemCount() {
        return listas.size();
    }

    static class ListaViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvFechaCreacion, tvUltimaModificacion, tvPresupuesto;
        ImageButton btnEditar, btnDuplicar, btnEliminar;

        public ListaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreLista);
            tvFechaCreacion = itemView.findViewById(R.id.tvFechaCreacion);
            tvUltimaModificacion = itemView.findViewById(R.id.tvUltimaModificacion);
            tvPresupuesto = itemView.findViewById(R.id.tvPresupuesto);
            btnEditar = itemView.findViewById(R.id.btnEditarLista);
            btnDuplicar = itemView.findViewById(R.id.btnDuplicarLista);
            btnEliminar = itemView.findViewById(R.id.btnEliminarLista);
        }

        public void bind(ListaEntity lista, OnListaClickListener listener) {
            String title = lista.getNombre() + (lista.isArchivada() ? " (Archivada)" : "");
            tvNombre.setText(title);
            tvFechaCreacion.setText("Creado: " + lista.getFechaCreacion());
            tvUltimaModificacion.setText("Modificado: " + lista.getFechaModificacion());

            if (lista.getPresupuestoMaximo() > 0) {
                tvPresupuesto.setVisibility(View.VISIBLE);
                tvPresupuesto.setText(String.format(Locale.getDefault(), "Presupuesto: $%.2f MXN", lista.getPresupuestoMaximo()));
            } else {
                tvPresupuesto.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onClick(lista));
            itemView.setOnLongClickListener(v -> {
                listener.onLongClick(lista, v);
                return true;
            });

            btnEditar.setOnClickListener(v -> listener.onEditar(lista));
            btnDuplicar.setOnClickListener(v -> listener.onDuplicar(lista));
            btnEliminar.setOnClickListener(v -> listener.onEliminar(lista));
        }
    }
}