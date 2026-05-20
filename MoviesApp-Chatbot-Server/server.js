require("dotenv").config();

const express = require("express");
const cors = require("cors");
const bodyParser = require("body-parser");
const chatRouter = require("./routes/chat");

const app = express();
const port = process.env.PORT || 3000;

app.use(cors());
app.use(bodyParser.json({ limit: "1mb" }));

app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl}`);
  next();
});

app.get("/api/health", (req, res) => {
  res.json({ status: "ok" });
});

app.use("/api/chat", chatRouter);

app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ error: "Erreur serveur" });
});

app.listen(port, () => {
  console.log(`MoviesApp Chatbot Server running on port ${port}`);
});
