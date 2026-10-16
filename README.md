# Nametags

Paper 1.21.8+ (Java 21) plugin that shows custom nametags above players' heads.

- Money (`1.12K`, `1K`, `1M`), hearts, kills and deaths, each on its own row
- Small caps white name, no bold
- `/nametag` (alias `/nametags`) opens a dialog screen to toggle what you see above other players's heads, applied instantly
- Money uses Vault (optional)

## Build

```
mvn package
```

The jar is created in `target/`. Pushing to GitHub also builds it automatically; download it from the Actions tab under Artifacts.
