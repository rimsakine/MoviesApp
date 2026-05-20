const express = require("express");
const { generateReply } = require("../services/cinebotModel");

const router = express.Router();

router.post("/", async (req, res) => {
  try {
    const { messages, userId } = req.body || {};
    if (!Array.isArray(messages)) {
      return res.status(400).json({ error: "Le champ messages doit etre un tableau" });
    }

    const safeMessages = messages
      .map((message) => ({
        role: message.role === "assistant" ? "assistant" : "user",
        content: String(message.content || "")
      }))
      .filter((message) => message.content.trim().length > 0);

    if (safeMessages.length === 0) {
      return res.status(400).json({ error: "Aucun message valide" });
    }

    console.log(`Local CineBot request userId=${userId || "anonymous"} messages=${safeMessages.length}`);
    const reply = generateReply(safeMessages);

    return res.json({
      text: reply.text,
      film: reply.film,
      timestamp: Date.now()
    });
  } catch (error) {
    console.error("Local CineBot error:", error);
    return res.status(500).json({ error: "Erreur du moteur CineBot local" });
  }
});

module.exports = router;
