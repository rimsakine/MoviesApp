# MoviesApp Chatbot Server

Serveur Express local pour MoviesApp. Il contient un moteur CineBot cree dans ce projet Node.js, sans API Claude/OpenAI obligatoire.

Le chatbot analyse les messages, detecte les preferences de l'utilisateur et recommande uniquement des films marocains depuis une base locale.

## Structure

```text
MoviesApp-Chatbot-Server/
├── server.js
├── routes/
│   └── chat.js
├── services/
│   └── cinebotModel.js
├── .env
├── .env.example
├── package.json
└── README.md
```

## Demarrage

1. Installer les dependances :

```bash
npm install
```

2. Lancer en developpement :

```bash
npm run dev
```

3. Lancer en production :

```bash
npm start
```

## Tests

Route sante :

```bash
curl http://localhost:3000/api/health
```

Route chatbot :

```bash
curl -X POST http://localhost:3000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"messages":[{"role":"user","content":"Je veux un film marocain romantique"}]}'
```

Reponse attendue :

```json
{
  "text": "D'apres tes preferences...",
  "film": {
    "tmdb_id": 785533,
    "titre": "Le Bleu du caftan",
    "annee": "2022",
    "genre": "Romance",
    "synopsis": "..."
  },
  "timestamp": 123456789
}
```

## Android

L'application Android appelle :

```text
POST /api/chat
```

Depuis un emulateur Android, utilise :

```text
http://10.0.2.2:3000/api/chat
```

Depuis un telephone reel, utilise l'adresse IP locale du PC :

```text
http://192.168.x.x:3000/api/chat
```

Le telephone et le PC doivent etre sur le meme reseau Wi-Fi.
