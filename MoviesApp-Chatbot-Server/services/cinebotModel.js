const films = [
  {
    tmdb_id: 72976,
    titre: "Casanegra",
    annee: "2008",
    genre: "Drame",
    humeur: ["sombre", "realiste", "urbain"],
    keywords: ["casablanca", "rue", "amis", "jeunesse", "drame", "realiste"],
    synopsis: "Deux amis de Casablanca cherchent une sortie dans une ville dure, entre combines, famille et reves bloques."
  },
  {
    tmdb_id: 57252,
    titre: "Ali Zaoua, prince de la rue",
    annee: "2000",
    genre: "Drame",
    humeur: ["emouvant", "social", "poetique"],
    keywords: ["enfants", "rue", "casablanca", "amitie", "emouvant", "social"],
    synopsis: "Des enfants des rues de Casablanca tentent d'offrir une sepulture digne a leur ami Ali."
  },
  {
    tmdb_id: 28719,
    titre: "Much Loved",
    annee: "2015",
    genre: "Drame",
    humeur: ["fort", "social", "adulte"],
    keywords: ["marrakech", "femmes", "societe", "drame", "adulte"],
    synopsis: "Portrait frontal de femmes marginalisees a Marrakech, entre survie, amitie et violence sociale."
  },
  {
    tmdb_id: 17287,
    titre: "Marock",
    annee: "2005",
    genre: "Romance",
    humeur: ["romantique", "jeunesse", "moderne"],
    keywords: ["amour", "jeunesse", "casablanca", "romance", "famille"],
    synopsis: "Une histoire d'amour adolescente a Casablanca, traversee par les tensions sociales et familiales."
  },
  {
    tmdb_id: 84351,
    titre: "Les Chevaux de Dieu",
    annee: "2012",
    genre: "Drame",
    humeur: ["intense", "social", "tragique"],
    keywords: ["sidi moumen", "casablanca", "radicalisation", "drame", "social"],
    synopsis: "Le parcours de jeunes d'un quartier pauvre de Casablanca happes par la radicalisation."
  },
  {
    tmdb_id: 318850,
    titre: "A Mile in My Shoes",
    annee: "2015",
    genre: "Drame",
    humeur: ["dur", "psychologique", "social"],
    keywords: ["traumatisme", "societe", "drame", "psychologique"],
    synopsis: "Said, marque par une enfance violente, tente de survivre dans une societe qui l'a souvent abandonne."
  },
  {
    tmdb_id: 451955,
    titre: "Razzia",
    annee: "2017",
    genre: "Drame",
    humeur: ["choral", "politique", "social"],
    keywords: ["casablanca", "destins", "societe", "liberte", "drame"],
    synopsis: "Plusieurs destins se croisent a Casablanca autour de la liberte, de la memoire et des contradictions sociales."
  },
  {
    tmdb_id: 682110,
    titre: "Adam",
    annee: "2019",
    genre: "Drame",
    humeur: ["intime", "emouvant", "feminin"],
    keywords: ["femmes", "grossesse", "boulangerie", "casablanca", "emouvant"],
    synopsis: "Une jeune femme enceinte trouve refuge chez une veuve, dans un recit delicat sur la solidarite feminine."
  },
  {
    tmdb_id: 812451,
    titre: "Haut et fort",
    annee: "2021",
    genre: "Musique",
    humeur: ["energique", "jeunesse", "musical"],
    keywords: ["rap", "musique", "jeunes", "casablanca", "energie"],
    synopsis: "Dans un centre culturel de Casablanca, des jeunes trouvent une voix par le rap et la creation."
  },
  {
    tmdb_id: 785533,
    titre: "Le Bleu du caftan",
    annee: "2022",
    genre: "Romance",
    humeur: ["delicat", "romantique", "melancolique"],
    keywords: ["amour", "artisanat", "couple", "caftan", "romance", "melancolie"],
    synopsis: "Un maitre tailleur, sa femme et un apprenti vivent une histoire d'amour pudique et bouleversante."
  },
  {
    tmdb_id: 1081676,
    titre: "La Mere de tous les mensonges",
    annee: "2023",
    genre: "Documentaire",
    humeur: ["memoire", "familial", "documentaire"],
    keywords: ["famille", "memoire", "histoire", "documentaire", "casablanca"],
    synopsis: "La realisatrice reconstruit la memoire familiale et collective a travers des figurines et des souvenirs."
  },
  {
    tmdb_id: 1147400,
    titre: "Everybody Loves Touda",
    annee: "2024",
    genre: "Drame",
    humeur: ["musical", "feminin", "ambitieux"],
    keywords: ["musique", "chant", "femme", "reve", "aita", "drame"],
    synopsis: "Touda reve de devenir cheikha reconnue et cherche une vie meilleure pour elle et son fils."
  }
];

function generateReply(messages) {
  const lastUserText = getLastUserText(messages);
  const normalized = normalize(lastUserText);
  const preferences = detectPreferences(normalized);

  if (isGreeting(normalized)) {
    return {
      text: "Bonjour ! Je suis CineBot, ton chatbot local pour les films marocains. Dis-moi ton humeur ou un genre : drame, romance, documentaire, musique, film social, film emouvant...",
      film: null
    };
  }

  if (needsMoreDetails(normalized)) {
    return {
      text: "Avec plaisir. Tu veux plutot un film marocain drole, romantique, social, intense, musical ou documentaire ? Tu preferes un film recent ou un classique ?",
      film: null
    };
  }

  const film = selectFilm(preferences, normalized);
  const reason = buildReason(film, preferences);
  return {
    text: `${reason}\n\nJe te recommande "${film.titre}" (${film.annee}). ${film.synopsis}`,
    film: {
      tmdb_id: film.tmdb_id,
      titre: film.titre,
      annee: film.annee,
      genre: film.genre,
      synopsis: film.synopsis
    }
  };
}

function getLastUserText(messages) {
  const reversed = [...messages].reverse();
  const found = reversed.find((message) => message.role === "user");
  return found ? String(found.content || "") : "";
}

function normalize(text) {
  return text
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^\w\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function detectPreferences(text) {
  const preferences = [];
  const dictionary = {
    comedie: ["comedie", "drole", "rire", "humour", "marrant"],
    romance: ["romance", "amour", "romantique", "couple"],
    drame: ["drame", "triste", "serieux", "intense", "fort"],
    documentaire: ["documentaire", "reel", "histoire", "memoire"],
    musique: ["musique", "musical", "rap", "chant", "aita"],
    social: ["social", "societe", "realiste", "quartier", "casablanca"],
    famille: ["famille", "mere", "enfant", "femmes"],
    recent: ["recent", "nouveau", "2024", "2023", "2022"],
    classique: ["classique", "ancien", "vieux"]
  };

  Object.entries(dictionary).forEach(([intent, words]) => {
    if (words.some((word) => text.includes(word))) preferences.push(intent);
  });
  return preferences;
}

function isGreeting(text) {
  return ["bonjour", "salut", "salam", "hello", "bonsoir"].includes(text);
}

function needsMoreDetails(text) {
  const vagueRequests = [
    "film",
    "je veux un film",
    "recommande moi",
    "recommande moi un film",
    "donne moi un film",
    "propose un film"
  ];
  return vagueRequests.includes(text);
}

function selectFilm(preferences, text) {
  let best = films[0];
  let bestScore = -1;
  films.forEach((film) => {
    let score = 0;
    const haystack = normalize([
      film.titre,
      film.genre,
      film.synopsis,
      ...film.humeur,
      ...film.keywords
    ].join(" "));

    preferences.forEach((preference) => {
      if (haystack.includes(preference)) score += 4;
      if (film.genre.toLowerCase().includes(preference)) score += 5;
      if (film.humeur.includes(preference)) score += 3;
      if (film.keywords.includes(preference)) score += 2;
    });

    text.split(" ").forEach((word) => {
      if (word.length > 3 && haystack.includes(word)) score += 1;
    });

    if (preferences.includes("recent") && Number(film.annee) >= 2020) score += 4;
    if (preferences.includes("classique") && Number(film.annee) < 2010) score += 4;

    if (score > bestScore) {
      bestScore = score;
      best = film;
    }
  });
  return best;
}

function buildReason(film, preferences) {
  if (preferences.length === 0) {
    return "Je pars sur une recommandation marocaine forte et accessible.";
  }
  return `D'apres tes preferences (${preferences.join(", ")}), ce film marocain correspond bien a ta demande.`;
}

module.exports = { generateReply };
