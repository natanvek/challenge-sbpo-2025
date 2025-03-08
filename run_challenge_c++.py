import os
import subprocess
import sys
import platform

# Flags de compilación y optimización
CXX = "g++"  # Puedes cambiarlo a "clang++" si usas Clang
CXX_FLAGS = "-O2 -std=c++17"
OUTPUT_EXECUTABLE = "challenge_exec"
MAX_RUNNING_TIME = "605s"

def compile_code(source_folder):
    print(f"Compiling code in {source_folder}...")
    os.chdir(source_folder)
    
    # Buscar archivo principal (.cpp)
    main_cpp = None
    for file in os.listdir(source_folder):
        if file.endswith(".cpp"):
            main_cpp = file
            break
    
    if not main_cpp:
        print("No C++ source file found!")
        return False
    
    # Comando de compilación
    cmd = f"{CXX} {CXX_FLAGS} {main_cpp} -o {OUTPUT_EXECUTABLE}"
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    
    if result.returncode != 0:
        print("Compilation failed:")
        print(result.stderr)
        return False
    
    print("Compilation successful.")
    return True

def run_benchmark(source_folder, input_folder, output_folder):
    os.chdir(source_folder)
    
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
    
    if platform.system() == "Darwin":
        timeout_command = "gtimeout"
    else:
        timeout_command = "timeout"
    
    executable_path = os.path.join(source_folder, OUTPUT_EXECUTABLE)
    
    if not os.path.exists(executable_path):
        print("Executable not found! Compilation might have failed.")
        return
    
    for filename in os.listdir(input_folder):
        if filename.endswith(".txt"):
            print(f"Running {filename}")
            input_file = os.path.join(input_folder, filename)
            output_file = os.path.join(output_folder, f"{os.path.splitext(filename)[0]}.txt")
            
            with open(input_file, "r") as inp, open(output_file, "w") as out:
                cmd = [timeout_command, MAX_RUNNING_TIME, executable_path]
                result = subprocess.run(cmd, stdin=inp, stdout=out, stderr=subprocess.PIPE, text=True)
                
                if result.returncode != 0:
                    print(f"Execution failed for {input_file}:")
                    print(result.stderr)

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python run_challenge.py <source_folder> <input_folder> <output_folder>")
        sys.exit(1)
    
    source_folder = sys.argv[1]
    input_folder = sys.argv[2]
    output_folder = sys.argv[3]
    
    if compile_code(source_folder):
        run_benchmark(source_folder, input_folder, output_folder)
