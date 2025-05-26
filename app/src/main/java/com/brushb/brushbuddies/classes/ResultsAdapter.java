package com.brushb.brushbuddies.classes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.brushb.brushbuddies.R;

import java.util.List;

public class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ResultViewHolder> {
    private List<PlayerResult> results;
    public final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(PlayerResult result);
    }

    public ResultsAdapter(List<PlayerResult> results, OnItemClickListener listener) {
        this.results = results;
        this.listener = listener;
    }

    public void updateData(List<PlayerResult> newResults) {
        this.results = newResults;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        PlayerResult result = results.get(position);
        holder.bind(result, position);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(result));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        private final TextView textRank, textPlayerName, textAverageScore;

        public ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            textRank = itemView.findViewById(R.id.textRank);
            textPlayerName = itemView.findViewById(R.id.textPlayerName);
            textAverageScore = itemView.findViewById(R.id.textAverageScore);
        }

        public void bind(PlayerResult result, int position) {
            textRank.setText(String.valueOf(position + 1));
            textPlayerName.setText(result.getPlayerName());
            textAverageScore.setText(String.format("Score: %.1f", result.getAverageScore()));

            if (position == 0) textRank.setTextColor(0xFFFFD700);
            else if (position == 1) textRank.setTextColor(0xFFC0C0C0);
            else if (position == 2) textRank.setTextColor(0xFFCD7F32);
            else textRank.setTextColor(0xFFFFFFFF);

        }
    }
}