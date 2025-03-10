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
    protected int os;
    protected int as;

    protected class Cart {
        public Set<Integer> my_orders = new HashSet<>();
        public Set<Integer> my_aisles = new HashSet<>();;
        public Map<Integer, Integer> available = new HashMap<>();
        public int cantItems = 0;

        public Cart() {}

        public double getValue() {
            if(my_aisles.size() == 0) return 0.0;
            return (double) cantItems / my_aisles.size();
        }

        public int getTope() {
            if (my_aisles.size() == 0) return as;
            return Math.min((int) Math.floor(waveSizeUB / getValue()), as);
        }

        public void update(Cart otro) {
            if (otro.getValue() > getValue()) {
                my_orders.clear();
                my_orders.addAll(otro.my_orders);
                my_aisles.clear();
                my_aisles.addAll(otro.my_aisles);
                cantItems = otro.cantItems;
            }
        }

        public void addAisle(int i) {
            my_aisles.add(aislesh[i].id);
            for (Map.Entry<Integer, Integer> entry : aislesh[i].items.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                available.merge(elem, cant, Integer::sum);
            }
        }

        public boolean removeRequestIfPossible(Map<Integer, Integer> m) {
            for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                if (available.getOrDefault(elem, 0).intValue() < cant)
                    return false;
            }
            for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                available.computeIfPresent(elem, (k, tengo) -> tengo - cant);
            }
            return true;
        }

        public void fill() {
            for (int o = 0; o < os; ++o) {
                if (cantItems + ordersh[o].size <= waveSizeUB
                        && removeRequestIfPossible(ordersh[o].items)) {
                    cantItems += ordersh[o].size;
                    my_orders.add(ordersh[o].id);
                }
            }
        }
    }

    public Heuristica(List<Map<Integer, Integer>> _orders, List<Map<Integer, Integer>> _aisles, int nItems,
            int waveSizeLB, int waveSizeUB) {
        super(_orders, _aisles, nItems, waveSizeLB, waveSizeUB);
        ordersh = new Order[_orders.size()];
        for (int o = 0; o < _orders.size(); o++) {
            ordersh[o] = new Order(o, _orders.get(o), 0);
            for (Map.Entry<Integer, Integer> entry : _orders.get(o).entrySet()) {
                ordersh[o].size += entry.getValue();
            }
        }

        aislesh = new Aisle[_aisles.size()];
        for (int a = 0; a < _aisles.size(); a++) {
            aislesh[a] = new Aisle(a, _aisles.get(a), 0);
            for (Map.Entry<Integer, Integer> entry : _aisles.get(a).entrySet()) {
                aislesh[a].size += entry.getValue();
            }
        }
        as = aisles.size();
        os = orders.size();

    }

    protected Cart pasada(Order[] ordersh, Aisle[] aislesh, int tope) {
        Cart rta = new Cart();
        for (int sol = 0; sol < tope; ++sol) {
            Cart actual = new Cart();
            for (int p = 0; p <= sol; ++p)
                actual.addAisle(p);

            for (int o = 0; o < os; ++o) {
                if (actual.cantItems + ordersh[o].size <= waveSizeUB
                        && actual.removeRequestIfPossible(ordersh[o].items)) {
                    actual.cantItems += ordersh[o].size;
                    actual.my_orders.add(ordersh[o].id);
                }
            }

            for (int p = sol; p >= 0; --p) {
                if (actual.my_aisles.contains(aislesh[p].id) && actual.removeRequestIfPossible(aislesh[p].items)) {
                    actual.my_aisles.remove(aislesh[p].id);
                }
            }

            if (actual.cantItems >= waveSizeLB)
                rta.update(actual);

            tope = Math.min(tope, rta.getTope());
            

        }
        return rta;
    }

    protected boolean tryFill(Map<Integer, Integer> toFill, Map<Integer, Integer> available) {
        for (Map.Entry<Integer, Integer> entry : toFill.entrySet()) {
            int elem = entry.getKey(), cant = entry.getValue();
            if (available.getOrDefault(elem, 0).intValue() < cant)
                return false;
        }
        for (Map.Entry<Integer, Integer> entry : toFill.entrySet()) {
            int elem = entry.getKey(), cant = entry.getValue();
            available.computeIfPresent(elem, (k, tengo) -> tengo - cant);
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
        if (numVisitedAisles == 0)
            return 0;

        return (double) totalUnitsPicked / numVisitedAisles;
    }
}
