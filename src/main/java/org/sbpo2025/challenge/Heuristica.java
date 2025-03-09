package org.sbpo2025.challenge;

import org.apache.commons.lang3.time.StopWatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;

public abstract class Heuristica extends ChallengeSolver {
    private final long MAX_RUNTIME = 600000; // milliseconds; 10 minutes

    protected static class Order {
        public int id;
        public Map<Integer, Integer> items;
        public int size;

        // Constructor para inicializar Order
        public Order(int _id, Map<Integer, Integer> _items, int _size) {
            this.id = _id;
            this.items = _items;
            this.size = _size;
        }
    }

    protected static class Aisle {
        public int id;
        public Map<Integer, Integer> items;
        public int size;

        // Constructor para inicializar Order
        public Aisle(int _id, Map<Integer, Integer> _items, int _size) {
            this.id = _id;
            this.items = _items;
            this.size = _size;
        }
    }

    protected Order[] ordersh;
    protected Aisle[] aislesh;


    protected class Cart {
        public Set<Integer> my_orders = new HashSet<>();
        public Set<Integer> my_aisles = new HashSet<>();;
        public Map<Integer, Integer> available = new HashMap<>();
        public int nItems = 0;

        public Cart() {}
        private boolean compare(Cart otro) {
            if(otro.my_aisles.size() == 0) return false;
            if(my_aisles.size() == 0) return true;

            return ((double) otro.nItems / otro.my_aisles.size() > (double) nItems / my_aisles.size()); 
        }

        public void update(Cart otro) {
            if(compare(otro)){
                my_orders.clear(); my_orders.addAll(otro.my_orders);
                my_aisles.clear(); my_orders.addAll(otro.my_aisles);
                nItems = otro.nItems;
            }
        }

        public void addAisle(int i) {
            my_aisles.add(aislesh[i].id);
            for (Map.Entry<Integer, Integer> entry : aislesh[i].items.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                available.merge(elem, cant, Integer::sum);
            }  
        }


        public boolean tryFill(int order) {
            for (Map.Entry<Integer, Integer> entry : ordersh[order].items.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                if(available.getOrDefault(elem, 0).intValue() < cant) return false;
            }
            for (Map.Entry<Integer, Integer> entry : ordersh[order].items.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                available.put(elem, available.get(elem) - cant);
            }
            return true;
        }
    }

    // protected record Order(int id, Map<Integer, Integer> items, int size) {}

    public Heuristica(List<Map<Integer, Integer>> _orders, List<Map<Integer, Integer>> _aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        super(_orders, _aisles, nItems, waveSizeLB, waveSizeUB);
        ordersh = new Order[_orders.size()];
        for(int o = 0; o < _orders.size(); o++) {
            ordersh[o] = new Order(o, _orders.get(o), 0);
            for (Map.Entry<Integer, Integer> entry : _orders.get(o).entrySet()) {
                ordersh[o].size += entry.getValue();
            }     
        }

        aislesh = new Aisle[_aisles.size()];
        for(int a = 0; a < _aisles.size(); a++) {
            aislesh[a] = new Aisle(a, _aisles.get(a), 0);
            for (Map.Entry<Integer, Integer> entry : _aisles.get(a).entrySet()) {
                aislesh[a].size += entry.getValue();
            }     
        }

    }


    protected boolean tryFill(Map<Integer, Integer> toFill, Map<Integer, Integer> available) {
        for (Map.Entry<Integer, Integer> entry : toFill.entrySet()) {
            int elem = entry.getKey(), cant = entry.getValue();
            if(available.getOrDefault(elem, 0).intValue() < cant) return false;
        }
        for (Map.Entry<Integer, Integer> entry : toFill.entrySet()) {
            int elem = entry.getKey(), cant = entry.getValue();
            available.put(elem, available.get(elem) - cant);
        }
        return true;
    }
    /*
     * Get the remaining time in seconds
     */
    protected long getRemainingTime(StopWatch stopWatch) {
        return Math.max(
                TimeUnit.SECONDS.convert(MAX_RUNTIME - stopWatch.getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
                0);
    }

    protected boolean isSolutionFeasible(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return false;
        }

        int[] totalUnitsPicked = new int[nItems];
        int[] totalUnitsAvailable = new int[nItems];

        // Calculate total units picked
        for (int order : selectedOrders) {
            for (Map.Entry<Integer, Integer> entry : ordersh[order].items.entrySet()) {
                totalUnitsPicked[entry.getKey()] += entry.getValue();
            }
        }

        // Calculate total units available
        for (int aisle : visitedAisles) {
            for (Map.Entry<Integer, Integer> entry : aislesh[aisle].items.entrySet()) {
                totalUnitsAvailable[entry.getKey()] += entry.getValue();
            }
        }

        // Check if the total units picked are within bounds
        int totalUnits = Arrays.stream(totalUnitsPicked).sum();
        if (totalUnits < waveSizeLB || totalUnits > waveSizeUB) {
            return false;
        }

        // Check if the units picked do not exceed the units available
        for (int i = 0; i < nItems; i++) {
            if (totalUnitsPicked[i] > totalUnitsAvailable[i]) {
                return false;
            }
        }

        return true;
    }

    protected double computeObjectiveFunction(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return 0.0;
        }
        int totalUnitsPicked = 0;

        // Calculate total units picked
        for (int order : selectedOrders) {
            totalUnitsPicked += ordersh[order].items.values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        // Calculate the number of visited aisles
        int numVisitedAisles = visitedAisles.size();

        // Objective function: total units picked / number of visited aisles
        if (numVisitedAisles == 0) return 0;

        return (double) totalUnitsPicked / numVisitedAisles;
    }
}
