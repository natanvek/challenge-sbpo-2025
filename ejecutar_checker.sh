#!/bin/bash

# Obtener el directorio donde está el script
SCRIPT_DIR="$(dirname "$(realpath "$0")")"

# Directorios relativos

DATASET_DIR="$SCRIPT_DIR/datasets/a"

if [[ " $* " == *" -d "* ]]; then
    DATASET_DIR="$SCRIPT_DIR/datasets/$2"
    shift 2
fi
OUTPUT_DIR="$SCRIPT_DIR/output"

BASE_DIR="$(pwd)"



if [ "$#" -gt 0 ]; then
    set -o pipefail
    python3 ./run_challenge.py "${BASE_DIR}" "${DATASET_DIR}" "${OUTPUT_DIR}" 2>&1 | tee /dev/stderr | grep -iq "error" && exit 1
fi


# Bucle para ejecutar las instancias
for instance in "$DATASET_DIR"/*.txt; do
    instance=$(basename "$instance")
    echo "-------------------------------------"
    echo -e "\033[38;5;213mRunning $instance\033[0m"
    python3 checker.py "$DATASET_DIR/$instance" "$OUTPUT_DIR/$instance"
    echo "-------------------------------------"
    echo ""
done


