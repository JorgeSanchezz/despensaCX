package com.example.despensacx;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AnioAdapter extends RecyclerView.Adapter<AnioAdapter.AnioViewHolder> {

    public interface OnAnioClickListener {
        void onAnioClick(EstadisticaModel.AnnoModel anioModel);
    }

    private final List<EstadisticaModel.AnnoModel> listaAnios;
    private final OnAnioClickListener listener;

    public AnioAdapter(List<EstadisticaModel.AnnoModel> listaAnios, OnAnioClickListener listener) {
        this.listaAnios = listaAnios;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AnioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_estadistica_anio, parent, false);
        return new AnioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnioViewHolder holder, int position) {
        EstadisticaModel.AnnoModel item = listaAnios.get(position);
        holder.tvAnio.setText(item.getAnio());
        holder.tvTotalAnio.setText(FormatUtils.formatCurrency(item.getTotalAnio()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAnioClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaAnios != null ? listaAnios.size() : 0;
    }

    static class AnioViewHolder extends RecyclerView.ViewHolder {
        TextView tvAnio, tvTotalAnio;

        public AnioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAnio = itemView.findViewById(R.id.tvAnio);
            tvTotalAnio = itemView.findViewById(R.id.tvTotalAnio);
        }
    }
}