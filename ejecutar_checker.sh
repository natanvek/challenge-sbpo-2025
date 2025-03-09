#!/bin/bash

# Obtener el directorio donde está el script
SCRIPT_DIR="$(dirname "$(realpath "$0")")"

# Directorios relativos
DATASET_DIR="$SCRIPT_DIR/datasets/a"
OUTPUT_DIR="$SCRIPT_DIR/output"

BASE_DIR="$(pwd)"


if [ "$#" -eq 0 ]; then
    # Si no se pasaron heurísticas, se asigna un valor por defecto
    heuristics=("Heuristica2" "Heuristica3")  # Valor por defecto
else
    # Si hay más de tres argumentos, tomamos los que siguen desde el cuarto
    heuristics=("${@:1}")
fi

echo "${heuristics[@]}"

set -o pipefail
python3 ./run_challenge.py "${BASE_DIR}" "${DATASET_DIR}" "${OUTPUT_DIR}" "${heuristics[@]}" 2>&1 | tee /dev/stderr | grep -iq "error" && exit 1

# Bucle para ejecutar las instancias
for instance in "$DATASET_DIR"/*.txt; do
    instance=$(basename "$instance")
    echo "-------------------------------------"
    echo "Running $instance\n"

    for solver_dir in "$OUTPUT_DIR"/*/; do 
        echo "---> Results for $(basename "$solver_dir"):"
        python3 checker.py "$DATASET_DIR/$instance" "$solver_dir/$instance"
        echo ""
    done

    echo "-------------------------------------"
    echo ""
done

# for solver_dir in "$OUTPUT_DIR"/*/; do
#     echo "-------------------------------------"
#     echo "Running $(basename "$solver_dir")"
#     echo "-------------------------------------"
#     for instance in "$solver_dir"*.txt; do
#         instance=$(basename "$instance")
#         echo "-------------------------------------"
#         echo "Running $instance"
#         echo "-------------------------------------"
#         # Ejecuta el checker para cada archivo
#         python checker.py "$DATASET_DIR/$instance" "$solver_dir/$instance"
#         echo ""
#     done
# done


