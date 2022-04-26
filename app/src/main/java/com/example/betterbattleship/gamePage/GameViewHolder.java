package com.example.betterbattleship.gamePage;

import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import com.example.betterbattleship.R;


public class GameViewHolder extends RecyclerView.ViewHolder {
    private ImageButton seatView;

    public GameViewHolder(@NonNull @NotNull View itemView) {
        super(itemView);
        seatView = itemView.findViewById(R.id.tileView);
    }

    public ImageButton getTileView() {
        return seatView;
    }
}