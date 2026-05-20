package com.example.moviapp_rimsakine;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private static final int TYPE_USER = 1;
    private static final int TYPE_BOT = 2;
    private static final int TYPE_FILM = 3;
    private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

    private final List<ChatMessage> messages;
    private final Context context;

    public ChatAdapter(List<ChatMessage> messages, Context context) {
        this.messages = messages;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.getFilm() != null) return TYPE_FILM;
        return message.isUser() ? TYPE_USER : TYPE_BOT;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout root = new LinearLayout(context);
        root.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(6), 0, dp(6));
        return new ChatViewHolder(root);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.root.removeAllViews();
        if (getItemViewType(position) == TYPE_FILM) {
            bindFilm(holder.root, message.getFilm());
        } else {
            bindBubble(holder.root, message);
        }
    }

    private void bindBubble(LinearLayout root, ChatMessage message) {
        TextView bubble = new TextView(context);
        bubble.setText(message.getText());
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(15);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setBackgroundColor(message.isUser() ? Color.parseColor("#2E75B6") : Color.parseColor("#1F1B2E"));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = message.isUser() ? Gravity.END : Gravity.START;
        params.leftMargin = message.isUser() ? dp(64) : 0;
        params.rightMargin = message.isUser() ? 0 : dp(64);
        bubble.setLayoutParams(params);
        root.addView(bubble);
    }

    private void bindFilm(LinearLayout root, MovieFilm film) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackgroundColor(Color.parseColor("#1F1B2E"));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, dp(48), 0);
        card.setLayoutParams(cardParams);

        ImageView poster = new ImageView(context);
        poster.setLayoutParams(new LinearLayout.LayoutParams(dp(70), dp(104)));
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (film.getPosterPath() != null && !film.getPosterPath().isEmpty()) {
            Glide.with(context).load(TMDB_IMAGE_BASE_URL + film.getPosterPath()).into(poster);
        } else {
            poster.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        card.addView(poster);

        LinearLayout textBox = new LinearLayout(context);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(dp(12), 0, 0, 0);
        textBox.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView title = new TextView(context);
        title.setText(film.getTitre() + " (" + film.getAnnee() + ")");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        textBox.addView(title);

        TextView genre = new TextView(context);
        genre.setText(film.getGenre());
        genre.setTextColor(Color.parseColor("#CCCCCC"));
        genre.setTextSize(13);
        textBox.addView(genre);

        TextView synopsis = new TextView(context);
        synopsis.setText(film.getSynopsis());
        synopsis.setTextColor(Color.parseColor("#FFFFFF"));
        synopsis.setTextSize(13);
        synopsis.setMaxLines(3);
        textBox.addView(synopsis);

        card.addView(textBox);
        card.setOnClickListener(v -> {
            Intent intent = new Intent(context, MovieDetailActivity.class);
            intent.putExtra("movieId", film.getTmdbId());
            context.startActivity(intent);
        });
        root.addView(card);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout root;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            root = (LinearLayout) itemView;
        }
    }
}
