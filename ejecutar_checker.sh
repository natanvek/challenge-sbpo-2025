#!/bin/bash

# Obtener el directorio donde está el script
SCRIPT_DIR="$(dirname "$(realpath "$0")")"

# Directorios relativos
DATASET_DIR="$SCRIPT_DIR/datasets/a"
OUTPUT_DIR="$SCRIPT_DIR/output"

BASE_DIR="$(pwd)"
set -o pipefail
python3 ./run_challenge.py "${BASE_DIR}" "${DATASET_DIR}" "${OUTPUT_DIR}" 2>&1 | tee /dev/stderr | grep -iq "error" && exit 1

# Bucle para ejecutar las instancias
for i in $(seq -w 1 20); do
    echo "-------------------------------------"
    echo "Running instance_00${i}.txt"
    echo "-------------------------------------"
    
    python checker.py "$DATASET_DIR/instance_00${i}.txt" \
                      "$OUTPUT_DIR/instance_00${i}.txt"
    
    echo ""
done



