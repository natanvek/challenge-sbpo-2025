package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.Inventory;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

import ilog.concert.*;
import ilog.cplex.*;

public class CHOOSE extends Heuristica {
    public CHOOSE(List<Map<Integer, Integer>> _orders, List<Map<Integer, Integer>> _aisles, int _nItems,
            int _waveSizeLB,
            int _waveSizeUB) {
        super(_orders, _aisles, _nItems, _waveSizeLB, _waveSizeUB);
    }



    public void runRanking() {
        
        rankings = new ArrayList<>();
        for (int i = 0; i <= tope; i++)
            rankings.add(new PriorityQueue<>(Comparator.comparingInt(EfficientCart::getCantItems)));

        rankings2 = new ArrayList<>();
        for (int i = 0; i <= tope; i++)
            rankings2.add(new PriorityQueue<>(Comparator.comparingInt(EfficientCart::getCantItems)));

        insertCart(rankings.get(0), new EfficientCart(), 1);

        int registerSize = calcRegisterSize(1);
        int registerSize2 = 7;
        System.out.println("registerSize: " + registerSize);

        for (Aisle p : aisles)
            for (int r = tope - 1; r >= 0; --r)
                for (EfficientCart m : rankings.get(r)) {
                    EfficientCart copia = new EfficientCart(m);
                    copia.addAisle(p);
                    fill(copia);
                    updateRta(copia);
                    insertCart(rankings.get(r + 1), copia, registerSize);
                    insertCart(rankings2.get(r + 1), copia, registerSize2);
                }

        rta.setAvailable();

        rtaAisles = new HashSet<Integer>();
        rtaOrders = new HashSet<Integer>();
        for (Aisle a : rta.my_aisles)
            rtaAisles.add(a.id);

        for (Order o : orders)
            if (rta.getCantItems() + o.size <= waveSizeUB && Inventory.checkAndRemove(o.items)) {
                rtaOrders.add(o.id);
                rta.addOrder(o);
            }

    }

    IloLinearNumExpr nAislesChoose;
    IloRange chooseRange;


    public void setUpHCplex() throws IloException {
        cplex.setOut(null);

        Set<Aisle> aislesVanAux = new HashSet<>();
        Set<Aisle> aislesVanComplemento = new HashSet<>();
        List<Integer> topSizes = bestSizes(4);

        // ordena el topsizes
        topSizes.sort((s1, s2) -> Integer.compare(s1, s2));
        System.out.println("topSizes: " + topSizes);
        int minTopSize = Math.max(topSizes.get(0) - 2, 1);
        int maxTopSize = Math.min(topSizes.get(topSizes.size() - 1) + 2, tope);
        for (int k = minTopSize; k <= maxTopSize; ++k) {
            for (EfficientCart m : rankings2.get(k))
                for (Aisle a : m.my_aisles)
                    aislesVanAux.add(a);
        }

        Set<Order> ordersVanAux = new HashSet<>();

        // for (Aisle a : aisles) {
        // if (!aislesVanAux.contains(a)) {
        // aislesVanComplemento.add(a);
        // aislesVanAux.add(a);
        // }
        // }
        // --------------------------------------------------------

        // --------------------------------------------------------
        aislesVan = new ArrayList<>(aislesVanAux);

        Map<Integer, Integer> disponible = new HashMap<>();
        for (Aisle a : aislesVan) {
            for (Map.Entry<Integer, Integer> entry : a.items.entrySet()) {
                disponible.put(entry.getKey(), disponible.getOrDefault(entry.getKey(), 0) + entry.getValue());
            }
        }

        for (Order o : orders) {
            ordersVanAux.add(o);
            for (Map.Entry<Integer, Integer> entry : o.items.entrySet()) {
                if (disponible.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                    ordersVanAux.remove(o);
                    break;
                }
            }
        }

        for (Order o : orders) {
            if (o.size > waveSizeUB)
                ordersVanAux.remove(o);
        }

        ordersVan = new ArrayList<>(ordersVanAux);

        aCP = cplex.boolVarArray(aislesVan.size());
        oCP = cplex.boolVarArray(ordersVan.size());

        int pos = 0;
        for (Aisle a : aislesVan)
            a.pos = pos++;

        pos = 0;
        for (Order o : ordersVan)
            o.pos = pos++;

        System.out.println("aislesVan: " + aislesVan.size() + " ordersVan: " + ordersVan.size());

        for (int i = 0; i < nItems; i++) {
            IloLinearNumExpr item_i_enOrders = cplex.linearNumExpr();
            IloLinearNumExpr item_i_enAisles = cplex.linearNumExpr();
            for (Order o : ordersVan) {
                if (o.items.getOrDefault(i, 0) > 0) {
                    item_i_enOrders.addTerm(oCP[o.pos], o.items.getOrDefault(i, 0));
                }
            }

            for (Aisle a : aislesVan) {
                if (a.items.getOrDefault(i, 0) > 0) {
                    item_i_enAisles.addTerm(aCP[a.pos], a.items.getOrDefault(i, 0));
                }
            }

            cplex.addLe(item_i_enOrders, item_i_enAisles);
        }

        waveSize = cplex.linearNumExpr();
        for (Order o : ordersVan)
            waveSize.addTerm(oCP[o.pos], o.size);

        cplex.addRange(waveSizeLB, waveSize, waveSizeUB);

        nAislesChoose = cplex.linearNumExpr();
        for (Aisle a : aislesVanComplemento)
            nAislesChoose.addTerm(aCP[a.pos], 1);

        // aisleRange = cplex.addRange(1, nAislesCP, tope);

        chooseRange = cplex.addRange(0, nAislesChoose, 0);

        nAislesCP = cplex.linearNumExpr();
        for (Aisle a : aislesVan)
            nAislesCP.addTerm(aCP[a.pos], 1);

        // aisleRange = cplex.addRange(1, nAislesCP, tope);

        aisleRange = cplex.addRange(minTopSize, nAislesCP, maxTopSize);

        // imprimir range
        System.out.println("aisleRange: " + aisleRange.getLB() + " <= nAislesCP <= " + aisleRange.getUB());

        haySolucion = cplex.addLe(1e-3, obj);

        // // === EMPHASIZE FEASIBILITY OVER OPTIMALITY ===
        cplex.setParam(IloCplex.Param.Emphasis.MIP, 1); // 1 = feasibility, 0 = optimality

        cplex.setParam(IloCplex.Param.MIP.Limits.CutsFactor, 1.0); // Reduce cantidad de cortes

        cplex.setParam(IloCplex.Param.MIP.Cuts.MIRCut, -1); // Let CPLEX decide
        cplex.setParam(IloCplex.Param.MIP.Cuts.Gomory, -1);
        cplex.setParam(IloCplex.Param.MIP.Cuts.Implied, -1);


    }

    int changui = 3;



    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        init();
        runRanking();
        mnrta = rta.getValue();

        System.out.println("nAisles inicial: " + nAisles);
        System.out.println("tope inicial: " + tope);
        System.out.println("nOrders inicial: " + nOrders);
        System.out.println("nAislesCP: " + rtaAisles.size());
        System.out.println("Solucion Ranking: " + mnrta);

        try {
            cplex = new IloCplex();
            setUpHCplex();

            System.out.println("-----------------------------");
            System.out.println("BUSCANDO SULUCION HEURISTICA");
            findOptimalSolution(stopWatch);
            // int radio = 1;
            // while(aisleRange.getLB() > 1 || aisleRange.getUB() < tope) {
            // radio *= 2;
            // aisleRange.setLB(Math.max(1, aisleRange.getLB() - radio));
            // aisleRange.setUB(Math.min(tope, aisleRange.getUB() + radio));
            // }

            // aisleRange.setBounds(rtaAisles.size()-1, rtaAisles.size());

            // chooseRange.setBounds(4, 4);

            // System.out.println("BUSCANDO SULUCION 1 CHOOSE");
            // findOptimalSolution(stopWatch);

            // cplex = new IloCplex();
            // setUpCplex();
            // changui = 3; // tiempo de espera para la solucion heuristica

            // System.out.println("-----------------------------");
            // System.out.println("BUSCANDO SULUCION OPTIMA");
            // findOptimalSolution(stopWatch);

        } catch (

        Exception e) {
            e.printStackTrace(System.out);
            System.out.println("pajaaaa");
        }

        return new ChallengeSolution(rtaOrders, rtaAisles);

    }
}

/*
 * resticciones:
 * 1. ordenes deben ser atendidas en su totalidad
 * 2. pasillos limitados por el stock de items
 * 3. lb <= waveSize <= ub
 * 4. pasillos contienen wave items
 * 
 */