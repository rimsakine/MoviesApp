package com.example.moviapp_rimsakine;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyMovieAdapter extends RecyclerView.Adapter<MyMovieAdapter.ViewHolder>
        implements Filterable {

    private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

    private MyMovieData[]     originalMovieData;
    private List<MyMovieData> filteredMovieData;
    private final Context     context;
    private final boolean     showBadge;

    public MyMovieAdapter(MyMovieData[] myMovieData, Context context, boolean showBadge) {
        this.originalMovieData = myMovieData;
        this.filteredMovieData = new ArrayList<>(Arrays.asList(myMovieData));
        this.context           = context;
        this.showBadge         = showBadge;
    }

    // ✅ Méthode pour mettre à jour les données après chaque nouvelle page
    public void updateData(MyMovieData[] newData) {
        this.originalMovieData = newData;
        this.filteredMovieData = new ArrayList<>(Arrays.asList(newData));
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView movieImage;
        TextView  textViewName;
        TextView  textViewDate;
        TextView  textBadgeNumber;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            movieImage      = itemView.findViewById(R.id.imageview);
            textViewName    = itemView.findViewById(R.id.textName);
            textViewDate    = itemView.findViewById(R.id.textdate);
            textBadgeNumber = itemView.findViewById(R.id.textBadgeNumber);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_movie_item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final MyMovieData movieData = filteredMovieData.get(position);

        holder.textViewName.setText(movieData.getMovieName());
        holder.textViewDate.setText(movieData.getMovieDate());

        if (holder.textBadgeNumber != null) {
            holder.textBadgeNumber.setText(String.format("%02d", position + 1));
        }

        Glide.with(context)
                .load(TMDB_IMAGE_BASE_URL + movieData.getMovieImage())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_close_clear_cancel)
                .into(holder.movieImage);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra("movieId", movieData.getMovieId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return filteredMovieData.size(); }

    @Override
    public Filter getFilter() { return movieFilter; }

    private final Filter movieFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<MyMovieData> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(Arrays.asList(originalMovieData));
            } else {
                String pattern = constraint.toString().toLowerCase().trim();
                for (MyMovieData movie : originalMovieData) {
                    if (movie.getMovieName().toLowerCase().contains(pattern)) {
                        filteredList.add(movie);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredMovieData.clear();
            filteredMovieData.addAll((List<MyMovieData>) results.values);
            notifyDataSetChanged();
        }
    };
}