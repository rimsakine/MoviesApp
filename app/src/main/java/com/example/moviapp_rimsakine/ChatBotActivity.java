package com.example.moviapp_rimsakine;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ChatBotActivity extends AppCompatActivity {
    private static final String SERVER_URL = "http://192.168.160.111:3000/api/chat";
    private static final String TMDB_API_KEY = "5d4a8c0bace78149087b4d6215ef7fe3";

    private final List<ChatMessage> messages = new ArrayList<>();
    private final List<JSONObject> conversationHistory = new ArrayList<>();
    private ChatAdapter adapter;
    private RecyclerView recyclerChat;
    private EditText editChatMessage;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        recyclerChat = findViewById(R.id.recyclerChat);
        editChatMessage = findViewById(R.id.editChatMessage);
        Button buttonSendChat = findViewById(R.id.buttonSendChat);
        requestQueue = Volley.newRequestQueue(this);

        adapter = new ChatAdapter(messages, this);
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(adapter);

        addBotMessage("Bonjour ! Je suis CinéBot 🎬\nDites-moi quel type de film vous cherchez et je vous recommande les meilleurs films marocains !");
        buttonSendChat.setOnClickListener(v -> {
            String text = editChatMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                editChatMessage.setText("");
                sendMessage(text);
            }
        });
    }

    private void sendMessage(String userText) {
        try {
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", userText);
            conversationHistory.add(userMessage);
        } catch (Exception ignored) {}

        messages.add(new ChatMessage(userText, true, null, System.currentTimeMillis()));
        int typingIndex = messages.size();
        messages.add(new ChatMessage("CinéBot écrit...", false, null, System.currentTimeMillis()));
        adapter.notifyDataSetChanged();
        scrollToBottom();

        try {
            JSONArray history = new JSONArray();
            for (JSONObject item : conversationHistory) history.put(item);

            JSONObject body = new JSONObject();
            body.put("messages", history);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            body.put("userId", user == null ? "" : user.getUid());

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SERVER_URL, body,
                    response -> handleBotResponse(response, typingIndex),
                    error -> {
                        removeTypingMessage(typingIndex);
                        Toast.makeText(this, getVolleyErrorMessage(error), Toast.LENGTH_LONG).show();
                    });
            requestQueue.add(request);
        } catch (Exception e) {
            removeTypingMessage(typingIndex);
            Toast.makeText(this, "Message invalide", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleBotResponse(JSONObject response, int typingIndex) {
        removeTypingMessage(typingIndex);
        String text = response.optString("text", "");
        addAssistantHistory(text);
        addBotMessage(text);

        JSONObject filmJson = response.optJSONObject("film");
        if (filmJson != null) {
            MovieFilm film = new MovieFilm(
                    filmJson.optInt("tmdb_id"),
                    filmJson.optString("titre"),
                    filmJson.optString("annee"),
                    filmJson.optString("genre"),
                    filmJson.optString("synopsis"));
            messages.add(new ChatMessage("", false, film, System.currentTimeMillis()));
            adapter.notifyItemInserted(messages.size() - 1);
            fetchPosterPath(film);
        }
        scrollToBottom();
    }

    private void fetchPosterPath(MovieFilm film) {
        if (film.getTmdbId() <= 0) return;
        String url = "https://api.themoviedb.org/3/movie/" + film.getTmdbId()
                + "?api_key=" + TMDB_API_KEY + "&language=fr-FR";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    film.setPosterPath(response.optString("poster_path", ""));
                    adapter.notifyDataSetChanged();
                },
                error -> {});
        requestQueue.add(request);
    }

    private void addBotMessage(String text) {
        messages.add(new ChatMessage(text, false, null, System.currentTimeMillis()));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void addAssistantHistory(String text) {
        try {
            JSONObject assistantMessage = new JSONObject();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", text);
            conversationHistory.add(assistantMessage);
        } catch (Exception ignored) {}
    }

    private void removeTypingMessage(int typingIndex) {
        if (typingIndex >= 0 && typingIndex < messages.size()) {
            messages.remove(typingIndex);
            adapter.notifyItemRemoved(typingIndex);
        }
    }

    private void scrollToBottom() {
        if (!messages.isEmpty()) recyclerChat.smoothScrollToPosition(messages.size() - 1);
    }

    private String getVolleyErrorMessage(VolleyError error) {
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                String body = new String(error.networkResponse.data);
                JSONObject json = new JSONObject(body);
                String serverError = json.optString("error");
                if (!serverError.isEmpty()) return serverError;
            } catch (Exception ignored) {}
            return "Erreur serveur chatbot: HTTP " + error.networkResponse.statusCode;
        }
        if (error.getMessage() != null) return "Chatbot: " + error.getMessage();
        return "Serveur chatbot indisponible";
    }
}
