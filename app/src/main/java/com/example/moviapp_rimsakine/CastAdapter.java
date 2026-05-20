package com.example.moviapp_rimsakine;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class CastAdapter extends RecyclerView.Adapter<CastAdapter.ViewHolder> {

    private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w185";

    private final List<CastMember> castList;
    private final Context          context;

    public CastAdapter(List<CastMember> castList, Context context) {
        this.castList = castList;
        this.context  = context;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageCast;
        TextView  textCastName;
        TextView  textCastCharacter;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCast         = itemView.findViewById(R.id.imageCast);
            textCastName      = itemView.findViewById(R.id.textCastName);
            textCastCharacter = itemView.findViewById(R.id.textCastCharacter);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_cast_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CastMember member = castList.get(position);

        holder.textCastName.setText(member.getName());
        holder.textCastCharacter.setText(member.getCharacter());

        if (member.getProfilePath() != null && !member.getProfilePath().isEmpty()) {
            Glide.with(context)
                    .load(TMDB_IMAGE_BASE_URL + member.getProfilePath())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(holder.imageCast);
        } else {
            holder.imageCast.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    @Override
    public int getItemCount() { return castList.size(); }
}