package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.Inventory;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;

import org.apache.commons.lang3.time.StopWatch;

import ilog.concert.*;
import ilog.cplex.*;

public class CPLEX2 extends Heuristica {
    public CPLEX2(List<Map<Integer, Integer>> _orders, List<Map<Integer, Integer>> _aisles, int _nItems,
            int _waveSizeLB,
            int _waveSizeUB) {
        super(_orders, _aisles, _nItems, _waveSizeLB, _waveSizeUB);
    }

    public boolean insertCart(PriorityQueue<EfficientCart> queue, EfficientCart newCart, Integer regSize) {
        if (queue.size() < regSize) {
            queue.offer(newCart);
            return true;
        }

        if (queue.peek().getValue() < newCart.getValue()) {
            queue.poll();
            queue.offer(newCart);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {

        int[] pesosAisle = new int[nAisles];

        for (Order o : orders) {
            if (o.size > waveSizeUB)
                continue;
            for (Aisle p : aisles) {
                int ocupa = 0;

                for (Map.Entry<Integer, Integer> entry : o.items.entrySet())
                    ocupa += Math.min(p.items.getOrDefault(entry.getKey(), 0).intValue(), entry.getValue());

                pesosAisle[p.id] += ocupa;
            }
        }

        Arrays.sort(orders, (o1, o2) -> Integer.compare(o2.size, o1.size));
        Arrays.sort(aisles, (a1, a2) -> Integer.compare(a2.size, a1.size));
        pasada();

        Arrays.sort(aisles, (a1, a2) -> Integer.compare(pesosAisle[a2.id], pesosAisle[a1.id]));
        pasada();

        int[] idToPos = new int[nAisles];
        for (int a = 0; a < nAisles; ++a)
            idToPos[aisles[a].id] = a;

        List<PriorityQueue<Heuristica.EfficientCart>> rankings = new ArrayList<>();
        for (int i = 0; i <= tope; i++)
            rankings.add(new PriorityQueue<>(Comparator.comparingInt(EfficientCart::getCantItems)));


        insertCart(rankings.get(0), new EfficientCart(), 1);

        for (Aisle p : aisles) 
            for (int r = tope - 1; r >= 0; --r) 
                for (EfficientCart m : rankings.get(r)) {
                    EfficientCart copia = new EfficientCart(m);
                    copia.addAisle(p);
                    fill(copia);
                    updateRta(copia);
                    insertCart(rankings.get(r + 1), copia, 1);
                }
            
            

        List<Integer> topSizes = new ArrayList<>();

        for (int r = 1; r <= tope; ++r) topSizes.add(r);   
        
        topSizes.sort((s1, s2) -> Double.compare(rankings.get(s2).peek().getCantItems() / (double) s2, rankings.get(s1).peek().getCantItems() / (double) s1));
        
        try (IloCplex cplex = new IloCplex()) {
            cplex.setOut(null);

            IloNumVar[] aCP = cplex.boolVarArray(nAisles);
            IloNumVar[] oCP = cplex.boolVarArray(nOrders);

            for (int i = 0; i < nItems; i++) {
                IloLinearNumExpr item_i_enOrders = cplex.linearNumExpr();
                IloLinearNumExpr item_i_enAisles = cplex.linearNumExpr();
                for (Order o : orders)
                    item_i_enOrders.addTerm(oCP[o.id], o.items.getOrDefault(i, 0));

                for (Aisle a : aisles)
                    item_i_enAisles.addTerm(aCP[a.id], a.items.getOrDefault(i, 0));

                cplex.addLe(item_i_enOrders, item_i_enAisles);
            }

            IloLinearNumExpr waveSize = cplex.linearNumExpr();
            for (Order o : orders)
                waveSize.addTerm(oCP[o.id], o.size);

            IloLinearNumExpr nAislesCP = cplex.linearNumExpr();
            for (Aisle a : aisles)
                nAislesCP.addTerm(aCP[a.id], 1);


            double mxrta = rta.getValue();
            Set<Integer> rtaOrders = new HashSet<>(), rtaAisles = new HashSet<>();

            rta.setAvailable();
    
            for (Aisle a : rta.my_aisles)
                rtaAisles.add(a.id);
    
            for (Order o : orders)
                if (rta.getCantItems() + o.size <= waveSizeUB && Inventory.checkAndRemove(o.items)){
                    rtaOrders.add(o.id);
                    rta.addOrder(o);
                }
    

            int changui = 3;

            int topeCP = tope;
            IloRange eqPasillos = cplex.addEq(1, nAislesCP);

            cplex.addMaximize(waveSize);

            IloRange checkObj = cplex.addRange(waveSizeLB, waveSize, waveSizeUB);
            
            System.out.println("tope: " + topeCP);
            System.out.println("rta: " + mxrta);
            // cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0.05); // permite 5% de gap
            // cplex.setParam(IloCplex.Param.MIP.Strategy.Branch, 1); // Strong branching
            // cplex.setParam(IloCplex.Param.MIP.Strategy.NodeSelect, 1); // 1 = Depth First, 2 = Best Bound

            for (int i : topSizes) {
                if (getRemainingTime(stopWatch) - changui <= 0) break;
                if (i > topeCP) continue;
                eqPasillos.setBounds(i, i);
                int lbactual = Math.max((int)(Math.ceil(mxrta * i) + 0.5), waveSizeLB); 
                if(lbactual > waveSizeUB) continue;
                System.out.println("-----------------------------");
                checkObj.setLB(lbactual);
                System.out.println("tiempo restante: " + getRemainingTime(stopWatch));
                System.out.println("pasillos: " + i);
                cplex.setParam(IloCplex.Param.TimeLimit, getRemainingTime(stopWatch) - changui);


                // Set<Integer> rta_orders = new HashSet<>(), rta_aisles = new HashSet<>();

                // rankings.get(i).peek().setAvailable();

                // // Llenás los sets como ya hacías
                // for (Aisle a : rankings.get(i).peek().my_aisles)
                //     rta_aisles.add(a.id);

                // for (Order o : orders)
                //     if (rankings.get(i).peek().getCantItems() + o.size <= waveSizeUB && Inventory.checkAndRemove(o.items)) {
                //         rta_orders.add(o.id);
                //         rankings.get(i).peek().addOrder(o);
                //     }

                // // Ahora convertimos eso a mip start
                // int totalVars = nAisles + nOrders;
                // IloNumVar[] mipVars = new IloNumVar[totalVars];
                // double[] mipVals = new double[totalVars];

                // // Primero pasillos (aCP)
                // for (int j = 0; j < nAisles; j++) {
                //     mipVars[j] = aCP[j];
                //     mipVals[j] = rta_aisles.contains(j) ? 1.0 : 0.0;
                // }

                // // Luego órdenes (oCP)
                // for (int j = 0; j < nOrders; j++) {
                //     mipVars[nAisles + j] = oCP[j];
                //     mipVals[nAisles + j] = rta_orders.contains(j) ? 1.0 : 0.0;
                // }

                // // Agregás el MIP start al modelo
                // cplex.addMIPStart(mipVars, mipVals);

                if (cplex.solve()) {
                    System.out.println(cplex.getStatus());
                    System.out.println("encontre solucion");
                    System.out.println("rta: " + cplex.getValue(waveSize) / i);

                    rtaAisles.clear();
                    rtaOrders.clear();
                    for (Order o : orders)
                        if (cplex.getValue(oCP[o.id]) > 0.5)
                            rtaOrders.add(o.id);

                    for (Aisle a : aisles)
                        if (cplex.getValue(aCP[a.id]) > 0.5)
                            rtaAisles.add(a.id);

                    System.out.println("nAislesCP: " + cplex.getValue(nAislesCP));
                    mxrta = Math.max(mxrta, cplex.getValue(waveSize) / i);
                    topeCP = (int) (waveSizeUB / mxrta);
                    System.out.println("tope: " + topeCP);
                } else {
                    System.out.println("no encontre solucion");
                }
            }

            return new ChallengeSolution(rtaOrders, rtaAisles);

        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.out.println("pajaaaa");
            return null;
        }

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