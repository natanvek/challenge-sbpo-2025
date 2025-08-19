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

            print("nOrders:", '\033[38;5;208m' + str(f"{o}") + "\033[0m", end=" | ")
            print("nItems:", '\033[38;5;208m' + str(f"{i}") + "\033[0m", end=" | ")
            print("nAisles:", '\033[38;5;208m' + str(f"{a}") + "\033[0m")

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

    
if __name__ == "__main__":
    import sys
    if len(sys.argv) != 2:
        print("Usage: python checker.py <input_file> <output_file>")
        sys.exit(1)

    wave_order_picking = WaveOrderPicking()
    wave_order_picking.read_input(sys.argv[1])
 
        