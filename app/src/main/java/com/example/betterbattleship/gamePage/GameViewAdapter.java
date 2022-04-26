package com.example.betterbattleship.gamePage;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import com.example.betterbattleship.R;

public class GameViewAdapter extends RecyclerView.Adapter<GameViewHolder> {

    private int size;
    private int currentPosition;
    private int selectedTile;
    private Context context;
    private final int FULL_OPACITY = 255;
    private final int QUARTER_OPACITY = 64;
    private final boolean DEFAULT_ENABLED = true;
    private final int width = 5 ;

    public GameViewAdapter(Context context, int size, int currentPosition) {
        this.size = size;
        this.currentPosition = currentPosition;
        selectedTile = -1;
        this.context = context;
    }

    @NonNull
    @NotNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.game_item_card,parent,false);
        return new GameViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onBindViewHolder(@NonNull @NotNull GameViewHolder holder, int position) {
        ImageButton tile = holder.getTileView();
        tile.setEnabled(DEFAULT_ENABLED);
        tile.setImageAlpha(FULL_OPACITY);
        if(position == currentPosition) {
            tile.setEnabled(!DEFAULT_ENABLED);
            tile.setImageAlpha(QUARTER_OPACITY);
            tile.setImageDrawable(context.getResources().getDrawable(R.drawable.reserved_seat));
        }
        else if (position == selectedTile){
            tile.setImageDrawable(context.getResources().getDrawable(R.drawable.selected_seat));
        }
        else {
            tile.setImageDrawable(context.getResources().getDrawable(R.drawable.available_seat));
        }

        holder.getTileView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(position == currentPosition -1 ||
                        position == currentPosition + 1 ||
                        position == currentPosition - width ||
                        position == currentPosition + width ||
                        position == currentPosition - width -1 ||
                        position == currentPosition + width -1 ||
                        position == currentPosition - width +1 ||
                        position == currentPosition + width +1
                ) {
                    selectedTile = position;
                }
                notifyDataSetChanged();
            }
        });
    }

    public int getSelectedTile() {
        return selectedTile;
    }

    @Override
    public int getItemCount() {
        return size;
    }
}