#!/bin/bash

# ==========================================
# CONFIGURAZIONE DEFAULT
# ==========================================
DEFAULT_URL="http://localhost:8080/api/itinerary"
NUM_REQUESTS=200
GEO_ID_1=1
GEO_ID_2=2

# File di Log
SUCCESS_LOG="success.log"
ERROR_LOG="errors.log"

# ==========================================
# 1. CHECK PRELIMINARI
# ==========================================
if ! command -v curl &> /dev/null; then
    echo "❌ Errore: 'curl' non è installato o non è nel PATH."
    echo "   Installalo per proseguire."
    exit 1
fi

# ==========================================
# 2. GESTIONE PARAMETRI (URL DINAMICO)
# ==========================================
# Se l'utente passa un argomento, usalo come URL. Altrimenti usa il default.
TARGET_URL=${1:-$DEFAULT_URL}

echo "🚀 Inizio Stress Test ($NUM_REQUESTS richieste)"
echo "🎯 Target: $TARGET_URL"
echo "---------------------------------------------------"

# Pulisce i log
> "$SUCCESS_LOG"
> "$ERROR_LOG"

# Funzione Genera JSON
generate_json() {
  local req_id=$1
  cat <<EOF
{
  "title": "Stress Trip $req_id",
  "locations": [
    { "geoId": $GEO_ID_1, "orderIndex": 0, "currentStop": true },
    { "geoId": $GEO_ID_2, "orderIndex": 1, "currentStop": false }
  ]
}
EOF
}

start_time=$(date +%s%3N)
if [[ "$start_time" == *"N"* ]]; then
  start_time=$(date +%s000)
fi


# ==========================================
# 3. LOOP DI ESECUZIONE
# ==========================================
for ((i=1; i<=NUM_REQUESTS; i++)); do
  (
    
    # Esegue la richiesta
    response=$(curl -s -w "\n%{http_code}" -X POST \
      --connect-timeout 5 \
      --max-time 10 \
      -H "Content-Type: application/json" \
      -d "$(generate_json $i)" \
      "$TARGET_URL")

    # Parsing risposta
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" -eq 202 ]; then
      echo "--- Req #$i (202 Accepted) ---" >> "$SUCCESS_LOG"
      echo "$body" >> "$SUCCESS_LOG"
      echo -e "\n----------------" >> "$SUCCESS_LOG"
    else
      echo -e "\e[31m❌ Req #$i FALLITA! Status: $http_code\e[0m"
      echo "--- Req #$i (Status: $http_code) ---" >> "$ERROR_LOG"
      echo "$body" >> "$ERROR_LOG"
      echo -e "\n----------------" >> "$ERROR_LOG"
    fi
  ) &
done

wait

end_time=$(date +%s%3N)
if [[ "$end_time" == *"N"* ]]; then
  end_time=$(date +%s000)
fi

# Fallback per date che non supportano %N (es. alcune versioni Mac vecchie)
if [ -z "$end_time" ]; then end_time=$(date +%s000); fi 
if [ -z "$start_time" ]; then start_time=$(date +%s000); fi

duration=$((end_time - start_time))

# ==========================================
# 4. REPORT
# ==========================================
success_count=$(grep -c "202 Accepted" "$SUCCESS_LOG")
# Se grep fallisce (file vuoto), ritorna 0
if [ -z "$success_count" ]; then success_count=0; fi

error_count=$(grep -c "Status:" "$ERROR_LOG")
if [ -z "$error_count" ]; then error_count=0; fi

echo ""
echo "🏁 COMPLETATO in ~${duration}ms"
echo "✅ Successi: $success_count (vedi $SUCCESS_LOG)"
if [ "$error_count" -gt 0 ]; then
    echo -e "\e[31m❌ Errori:   $error_count (vedi $ERROR_LOG)\e[0m"
else
    echo "🎉 Nessun errore!"
fi