package org.sbpo2025.challenge;

import org.apache.commons.lang3.time.StopWatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;

public class ChallengeSolver {
    private final long MAX_RUNTIME = 600000; // milliseconds; 10 minutes

    protected List<Map<Integer, Integer>> orders;
    protected List<Map<Integer, Integer>> aisles;
    protected int nItems;
    protected int waveSizeLB;
    protected int waveSizeUB;

    public ChallengeSolver(
            List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
    }

    public ChallengeSolution solve(StopWatch stopWatch) {
        // Implement your solution here
        int os = orders.size();
        int ps = aisles.size();
        int[] ps_copados = new int[ps];
        int[] o_size = new int[os];
        


        for(int o = 0; o < os; ++o) {
            for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet()) 
                o_size[o] += entry.getValue();
            
            for(int p = 0; p < ps; ++p) {
                int ocupa = 0;
                for (Map.Entry<Integer, Integer> entry : orders.get(o).entrySet()) {
                    int cant = entry.getValue();
                    int elem = entry.getKey();
                    ocupa += Math.min(aisles.get(p).getOrDefault(elem, 0).intValue(), cant);
                }
                if(ocupa > o_size[o] / 2) ps_copados[p]++;
                
            }   
        }

        Integer[] indices_o = new Integer[os];
        for (int i = 0; i < os; i++) indices_o[i] = i;
        Arrays.sort(indices_o, (i1, i2) -> Integer.compare(o_size[i2], o_size[i1]));
        
        Integer[] indices_p = new Integer[ps];
        for (int i = 0; i < ps; i++) indices_p[i] = i;
        Arrays.sort(indices_p, (i1, i2) -> Integer.compare(ps_copados[i2], ps_copados[i1]));

        Map<Integer, Integer> m = new HashMap<>();
        Set<Integer> rta_os = new HashSet<>();
        Set<Integer> rta_ps = new HashSet<>(), actual_ps = new HashSet<>();
        double rta_val = 0;
        for(int sol = 0; sol < ps; ++sol) {
            actual_ps.add(indices_p[sol]);
            for (Map.Entry<Integer, Integer> entry : aisles.get(indices_p[sol]).entrySet()) {
                int elem = entry.getKey(), cant = entry.getValue();
                m.merge(elem, cant, Integer::sum);
            }
            Map<Integer, Integer> copia_m = new HashMap<>(m);
            Integer mirta = 0;
            Set<Integer> actual_os = new HashSet<>();
            for(int o = 0; o < os; ++o) {
                if(mirta + o_size[indices_o[o]] > waveSizeUB) 
                    continue;
                boolean anda = true;
                for (Map.Entry<Integer, Integer> entry : orders.get(indices_o[o]).entrySet()) {
                    int elem = entry.getKey(), cant = entry.getValue();
                    if(copia_m.getOrDefault(elem, 0).intValue() < cant) anda = false;
                }
                if(!anda) continue;
                for (Map.Entry<Integer, Integer> entry : orders.get(indices_o[o]).entrySet()) {
                    int elem = entry.getKey(), cant = entry.getValue();
                    copia_m.put(elem, copia_m.get(elem) - cant);
                }
                mirta += o_size[indices_o[o]];
                actual_os.add(indices_o[o]);
            }
            if(mirta >= waveSizeLB && (double) mirta / (double)(sol + 1) > rta_val ) {
                rta_val = (double)mirta / (double)(sol + 1);
                rta_os = actual_os;
                rta_ps = actual_ps;
            }
        }
        // ChallengeSolution rta(rta_os, rta_ps);
        return new ChallengeSolution(rta_os, rta_ps);
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
            for (Map.Entry<Integer, Integer> entry : orders.get(order).entrySet()) {
                totalUnitsPicked[entry.getKey()] += entry.getValue();
            }
        }

        // Calculate total units available
        for (int aisle : visitedAisles) {
            for (Map.Entry<Integer, Integer> entry : aisles.get(aisle).entrySet()) {
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
            totalUnitsPicked += orders.get(order).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        // Calculate the number of visited aisles
        int numVisitedAisles = visitedAisles.size();

        // Objective function: total units picked / number of visited aisles
        return (double) totalUnitsPicked / numVisitedAisles;
    }
}
