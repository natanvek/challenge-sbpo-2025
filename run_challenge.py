import os
import subprocess
import sys
import argparse
import platform

# Paths to the libraries
CPLEX_PATH = "/opt/ibm/ILOG/CPLEX_Studio2212/opl/bin/x86-64_linux"
OR_TOOLS_PATH = "$HOME/Documents/or-tools/build/lib/"

USE_CPLEX = True
USE_OR_TOOLS = False

MAX_RUNNING_TIME = "1605s"

def compile_code(source_folder):
    print(f"Compiling code in {source_folder}...")
    # Change to the source folder
    os.chdir(source_folder)

    # Run Maven compile
    result = subprocess.run(["mvn", "clean", "package"], capture_output=True, text=True)


    print("STDOUT:\n", result.stdout)
    print("STDERR:\n", result.stderr)
    
    if result.returncode != 0:
        print("Maven compilation failed:")
        print(result.stderr)
        return False

    print("Maven compilation successful.")
    return True


def run_benchmark(source_folder, input_folder, output_folder, heuristicas):
    # Change to the source folder
    os.chdir(source_folder)

    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    # Set the library path (if needed)
    if USE_CPLEX and USE_OR_TOOLS:
        libraries = f"{OR_TOOLS_PATH}:{CPLEX_PATH}"
    elif USE_CPLEX:
        libraries = CPLEX_PATH
    elif USE_OR_TOOLS:
        libraries = OR_TOOLS_PATH

    if platform.system() == "Darwin":
        timeout_command = "gtimeout"
    else:
        timeout_command = "timeout"

    for filename in os.listdir(input_folder):
        if filename.endswith(".txt"):
            print(f"Running {filename}")
            input_file = os.path.join(input_folder, filename)

            for heuristica in heuristicas:
                heuristica_output_folder = os.path.join(output_folder, heuristica)
                if not os.path.exists(heuristica_output_folder):
                    os.makedirs(heuristica_output_folder)

                output_file = os.path.join(heuristica_output_folder, f"{os.path.splitext(filename)[0]}.txt")
                with open(output_file, "w") as out:
                    # Main Java command
                    cmd = [timeout_command, MAX_RUNNING_TIME, "java", "-Xmx16g", "-jar", "target/ChallengeSBPO2025-1.0.jar",
                        input_file,
                        output_file, 
                        heuristica]
                    if USE_CPLEX or USE_OR_TOOLS:
                        cmd.insert(3, f"-Djava.library.path={libraries}")

                    result = subprocess.run(cmd, stderr=subprocess.PIPE, text=True)
                    if result.returncode != 0:
                        print(f"Execution failed for {input_file}:")
                        print(result.stderr)


if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python run_challenge.py <source_folder> <input_folder> <output_folder> <heuristica1> <...>")
        sys.exit(1)

    source_folder = sys.argv[1]
    input_folder = sys.argv[2]
    output_folder = sys.argv[3]

    parser = argparse.ArgumentParser(description="Ejecutar heurísticas.")
    parser.add_argument(
        'heuristics', 
        nargs='+',  # Esto permite múltiples heurísticas como argumentos
        help="Lista de heurísticas a ejecutar (por ejemplo, Heuristica1 Heuristica2)"
    )

    # Recoger las heurísticas a partir de argv[4] en adelante
    args = parser.parse_args(sys.argv[4:])

    if compile_code(source_folder):
        run_benchmark(source_folder, input_folder, output_folder, args.heuristics)
