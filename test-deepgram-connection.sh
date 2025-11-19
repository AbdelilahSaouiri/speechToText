#!/bin/bash

# Script de test pour vérifier la connexion Deepgram

echo "🔍 Test de connexion Deepgram"
echo "================================"

# Vérifier si la clé API est définie
if [ -z "$DEEPGRAM_API_KEY" ]; then
    echo "❌ ERREUR: DEEPGRAM_API_KEY n'est pas définie"
    echo ""
    echo "Pour définir la clé API:"
    echo "  export DEEPGRAM_API_KEY='votre_cle_api'"
    echo ""
    echo "Ou ajoutez-la dans le service systemd:"
    echo "  sudo systemctl edit mon-app-spring.service"
    echo "  # Ajoutez: Environment=\"DEEPGRAM_API_KEY=votre_cle_api\""
    exit 1
fi

echo "✅ DEEPGRAM_API_KEY est définie"
echo "   Longueur: ${#DEEPGRAM_API_KEY} caractères"
echo "   Préfixe: ${DEEPGRAM_API_KEY:0:4}..."
echo ""

# Test de connexion avec curl (pour vérifier que la clé est valide)
echo "🔗 Test de connexion à l'API Deepgram REST..."

# Test avec l'API REST (plus simple pour vérifier la clé)
response=$(curl -s -w "\n%{http_code}" \
    -X POST "https://api.deepgram.com/v1/listen" \
    -H "Authorization: Token $DEEPGRAM_API_KEY" \
    -H "Content-Type: audio/wav" \
    --data-binary "@/dev/null" 2>&1)

http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

echo "   Code HTTP: $http_code"

if [ "$http_code" = "400" ]; then
    echo "   ✅ La clé API est valide (400 est attendu sans données audio)"
    echo "   Message: $body"
elif [ "$http_code" = "401" ] || [ "$http_code" = "403" ]; then
    echo "   ❌ ERREUR: Clé API invalide ou non autorisée"
    echo "   Message: $body"
    exit 1
else
    echo "   Réponse: $body"
fi

echo ""
echo "🔗 Test de connexion WebSocket Deepgram..."
echo "   URL: wss://api.deepgram.com/v1/listen?token=***&model=nova-2&language=fr&encoding=linear16&sample_rate=16000&channels=1&interim_results=true"
echo ""
echo "   Pour tester manuellement avec wscat (si installé):"
echo "   wscat -c 'wss://api.deepgram.com/v1/listen?token=$DEEPGRAM_API_KEY&model=nova-2&language=fr&encoding=linear16&sample_rate=16000&channels=1&interim_results=true'"
echo ""

echo "✅ Test terminé"
echo ""
echo "Si la clé API est valide, le problème pourrait venir de:"
echo "  1. L'encodage de l'URL dans le code Java"
echo "  2. Les paramètres de l'URL WebSocket (encoding=linear16 vs pcm16)"
echo "  3. La bibliothèque Java-WebSocket qui n'envoie pas correctement les en-têtes"
echo ""
echo "Vérifiez les logs du service Spring Boot pour voir:"
echo "  - Si la clé API est chargée (🔑 Clé API Deepgram chargée)"
echo "  - L'URL complète utilisée (🔗 URL)"
echo "  - Les détails de l'erreur 400"

