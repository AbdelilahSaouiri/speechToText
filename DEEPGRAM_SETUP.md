# Configuration Deepgram pour la transcription en temps réel

Ce projet utilise Deepgram pour la transcription vocale en temps réel via WebSocket.

## Configuration de la clé API

### 1. Obtenir une clé API Deepgram

1. Créez un compte sur [Deepgram](https://www.deepgram.com/)
2. Accédez à votre [console Deepgram](https://console.deepgram.com/)
3. Créez une nouvelle clé API dans la section "API Keys"

### 2. Configurer la variable d'environnement

Définissez la variable d'environnement `DEEPGRAM_API_KEY` avec votre clé API :

**Linux/Mac:**
```bash
export DEEPGRAM_API_KEY="votre_cle_api_deepgram"
```

**Windows (PowerShell):**
```powershell
$env:DEEPGRAM_API_KEY="votre_cle_api_deepgram"
```

**Windows (CMD):**
```cmd
set DEEPGRAM_API_KEY=votre_cle_api_deepgram
```

### 3. Lancer l'application

Une fois la variable d'environnement configurée, lancez l'application Spring Boot :

```bash
cd backend
mvn spring-boot:run
```

L'application utilisera automatiquement la clé API depuis la variable d'environnement.

## Endpoint WebSocket

L'endpoint WebSocket pour la transcription en temps réel est disponible à :

```
ws://localhost:8080/ws/audio/stream
```

## Paramètres de transcription

Les paramètres de transcription Deepgram sont configurés dans `DeepgramStreamingService.java` :

- **Modèle**: `nova-2` (modèle le plus récent et performant)
- **Langue**: `fr` (français)
- **Résultats intermédiaires**: Activés (`interim_results=true`)
- **Ponctuation**: Activée (`punctuate=true`)
- **Format intelligent**: Activé (`smart_format=true`)

Pour modifier ces paramètres, éditez la ligne 37 dans `DeepgramStreamingService.java`.

## Test

Pour tester la transcription en temps réel :

1. Assurez-vous que le backend est démarré
2. Lancez l'application Flutter frontend
3. Accédez à l'écran "Transcription en temps réel"
4. Cliquez sur "Démarrer" et parlez dans le microphone
5. La transcription apparaîtra en temps réel

## Dépannage

### Erreur "Impossible de se connecter à Deepgram"

- Vérifiez que la variable d'environnement `DEEPGRAM_API_KEY` est bien définie
- Vérifiez que votre clé API est valide et active
- Vérifiez votre connexion Internet
- Consultez les logs du serveur pour plus de détails

### Pas de transcription reçue

- Vérifiez que le microphone a les permissions nécessaires
- Vérifiez que l'audio est bien envoyé (consultez les logs)
- Vérifiez que la connexion WebSocket est établie

