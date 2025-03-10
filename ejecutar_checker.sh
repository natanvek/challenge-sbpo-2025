#!/bin/bash

# Obtener el directorio donde está el script
SCRIPT_DIR="$(dirname "$(realpath "$0")")"

# Directorios relativos
DATASET_DIR="$SCRIPT_DIR/datasets/a"
OUTPUT_DIR="$SCRIPT_DIR/output"

BASE_DIR="$(pwd)"



if [ "$#" -gt 0 ]; then
    heuristics=("${@:1}")
    set -o pipefail
    python3 ./run_challenge.py "${BASE_DIR}" "${DATASET_DIR}" "${OUTPUT_DIR}" "${heuristics[@]}" 2>&1 | tee /dev/stderr | grep -iq "error" && exit 1
fi


# Bucle para ejecutar las instancias
for instance in "$DATASET_DIR"/*.txt; do
    instance=$(basename "$instance")
    echo "-------------------------------------"
    echo -e "\033[38;5;213mRunning $instance\033[0m"

    for solver_dir in "$OUTPUT_DIR"/*/; do 
        echo ""
        echo -e "---> \033[38;5;120mResults for $(basename "$solver_dir")\033[0m:"
        python3 checker.py "$DATASET_DIR/$instance" "$solver_dir/$instance"
    done

    echo "-------------------------------------"
    echo ""
done


