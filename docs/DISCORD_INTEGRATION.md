# Discord Project Feed Integration

Rot Client can use the companion Discord bot as a local development service.
The bot publishes the living roadmap, Git commits, completed GitHub Actions
runs, and verified release assets to dedicated Discord channels.

## Local setup

The integration never stores Discord credentials in this repository. Configure
and authenticate the bot in its own checkout, then point Rot Client at that
checkout with either of these local-only options:

1. Set `ROT_CLIENT_DISCORD_BOT_PATH` to the bot repository directory.
2. Create an ignored `rotclient-dev.local.json` in the Rot Client root:

```json
{
  "discordBotPath": "path/to/your/discord-bot"
}
```

Run the idempotent bootstrap from the Rot Client root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/ensure-discord-project-feed.ps1
```

The command uses the bot's local health file to avoid duplicate processes. When
the bot is offline, it starts the existing bot checkout in a hidden background
process. It does not read, print, copy, or forward Discord or GitHub tokens.

The feed polls the local roadmap while development is in progress. Git commit,
Actions, and release messages appear after the corresponding GitHub event is
available. Release uploads are accepted only when the bot can verify a single
playable JAR and its declared SHA-256 digest.
