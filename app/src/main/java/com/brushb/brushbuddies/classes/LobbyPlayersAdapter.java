package com.brushb.brushbuddies.classes;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.brushb.brushbuddies.R;
import com.brushb.brushbuddies.models.PlayerInfo;

import java.util.List;

public class LobbyPlayersAdapter extends RecyclerView.Adapter<LobbyPlayersAdapter.VH> {

    private List<PlayerInfo> players;

    public LobbyPlayersAdapter(List<PlayerInfo> players) {
        this.players = players;
    }

    public void updateData(List<PlayerInfo> players) {
        this.players = players;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        PlayerInfo p = players.get(i);
        h.tvName.setText(p.username + (p.isHost ? " (Хост)" : ""));
        h.tvName.setTypeface(null, p.isHost ? Typeface.BOLD : Typeface.NORMAL);
        h.tvStats.setText(
                "🎮 " + p.totalGames +
                        "  🏆 " + p.wins +
                        "  ⭐ " + p.totalScore
        );
    }

    @Override public int getItemCount() { return players.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvStats;
        VH(View item) {
            super(item);
            tvName  = item.findViewById(R.id.tvPlayerName);
            tvStats = item.findViewById(R.id.tvPlayerStats);
        }
    }
}
