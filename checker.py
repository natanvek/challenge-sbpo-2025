import os
import numpy as np

class WaveOrderPicking:
    def __init__(self):
        self.orders = None
        self.aisles = None
        self.wave_size_lb = None
        self.wave_size_ub = None

    def read_input(self, input_file_path):
        with open(input_file_path, 'r') as file:
            lines = file.readlines()
            first_line = lines[0].strip().split()
            o, i, a = int(first_line[0]), int(first_line[1]), int(first_line[2])

            # Read orders
            self.orders = []
            for j in range(o):
                order_line = lines[j + 1].strip().split()
                d = int(order_line[0])
                order_map = {int(order_line[2 * k + 1]): int(order_line[2 * k + 2]) for k in range(d)}
                self.orders.append(order_map)

            # Read aisles
            self.aisles = []
            for j in range(a):
                aisle_line = lines[j + o + 1].strip().split()
                d = int(aisle_line[0])
                aisle_map = {int(aisle_line[2 * k + 1]): int(aisle_line[2 * k + 2]) for k in range(d)}
                self.aisles.append(aisle_map)

            # Read wave size bounds
            bounds = lines[o + a + 1].strip().split()
            self.wave_size_lb = int(bounds[0])
            self.wave_size_ub = int(bounds[1])

            # print("LB = " + str(self.wave_size_lb))
            # print("UB = " + str(self.wave_size_ub))

    def read_output(self, output_file_path):

        with open(output_file_path, 'r') as file:
            lines = file.readlines()
            if not lines:  # Verifica si la lista está vacía
                print("\033[38;5;196mSolution does not exist\033[0m")
                sys.exit(1)
                
            num_orders = int(lines[0].strip())
            selected_orders = [int(lines[i + 1].strip()) for i in range(num_orders)]
            num_aisles = int(lines[num_orders + 1].strip())
            visited_aisles = [int(lines[num_orders + 2 + i].strip()) for i in range(num_aisles)]
            execution_time = lines[num_orders + num_aisles + 2] if num_orders + num_aisles + 2 < len(lines) else 0

        selected_orders = list(set(selected_orders))
        visited_aisles = list(set(visited_aisles))
        return selected_orders, visited_aisles, execution_time

    def is_solution_feasible(self, selected_orders, visited_aisles):
        total_units_picked = 0

        for order in selected_orders:
            if not (order < len(self.orders)):
                print("\033[38;5;196mSolution to other dataset\033[0m")
                return False
            total_units_picked += np.sum(list(self.orders[order].values()))


        if(total_units_picked == 0) : 
            print("\033[38;5;196mSolution was not found\033[0m")
            return False
        
        # Check if total units picked are within bounds
        if not (self.wave_size_lb <= total_units_picked):
            print("\033[38;5;196mSolution does not respect lower boundary\033[0m")
            return False

        if not (total_units_picked <= self.wave_size_ub):
            print("\033[38;5;196mSolution does not respect upper boundary\033[0m")
            return False

        # Compute all items that are required by the selected orders
        required_items = set()
        for order in selected_orders:
            required_items.update(self.orders[order].keys())

        # Check if all required items are available in the visited aisles
        for item in required_items:
            total_required = sum(self.orders[order].get(item, 0) for order in selected_orders)
            total_available = sum(self.aisles[aisle].get(item, 0) for aisle in visited_aisles)
            if total_required > total_available:
                print("\033[38;5;196mThe Orders do not fit in the aisles chosen\033[0m")
                return False

        return True

    def compute_objective_function(self, selected_orders, visited_aisles):
        total_units_picked = 0

        # Calculate total units picked
        for order in selected_orders:
            total_units_picked += np.sum(list(self.orders[order].values()))

        # Calculate the number of visited aisles
        num_visited_aisles = len(visited_aisles)

        # Objective function: total units picked / number of visited aisles
        if(num_visited_aisles == 0) : return 0.0
        
        return total_units_picked / num_visited_aisles

if __name__ == "__main__":
    import sys
    if len(sys.argv) != 3:
        print("Usage: python checker.py <input_file> <output_file>")
        sys.exit(1)

    wave_order_picking = WaveOrderPicking()
    wave_order_picking.read_input(sys.argv[1])
    if not os.path.exists(sys.argv[2]):
        print("\033[38;5;196mSolution does not exist\033[0m")
        sys.exit(1)


    selected_orders, visited_aisles, execution_time = wave_order_picking.read_output(sys.argv[2])

    is_feasible = wave_order_picking.is_solution_feasible(selected_orders, visited_aisles)

    if is_feasible:
        objective_value = wave_order_picking.compute_objective_function(selected_orders, visited_aisles)
        print("Value:", '\033[38;5;226m' + str(f"{objective_value:.2f}") + "\033[0m", end=" | ")
        print("Aisles:", '\033[38;5;111m' + str(len(visited_aisles)) + "\033[0m",end=" | ")
        print("Execution Time:", '\033[38;5;10m' + str(execution_time) + "\033[0m"+"min")
        