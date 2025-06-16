package org.sbpo2025.challenge.solvers; // Paquete correcto

import org.sbpo2025.challenge.Heuristica;
import org.sbpo2025.challenge.Inventory;
import org.sbpo2025.challenge.ChallengeSolution;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

import ilog.concert.*;
import ilog.cplex.*;

public class CPLEX extends Heuristica {
    public CPLEX(List<Map<Integer, Integer>> _orders, List<Map<Integer, Integer>> _aisles, int _nItems,
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

        for (int r = 1; r <= tope; ++r)
            topSizes.add(r);

        topSizes.sort((s1, s2) -> Double.compare(rankings.get(s2).peek().getCantItems() / (double) s2,
                rankings.get(s1).peek().getCantItems() / (double) s1));

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

            double mnrta = rta.getValue();
            Set<Integer> rtaOrders = new HashSet<>(), rtaAisles = new HashSet<>();

            rta.setAvailable();

            for (Aisle a : rta.my_aisles)
                rtaAisles.add(a.id);

            for (Order o : orders)
                if (rta.getCantItems() + o.size <= waveSizeUB && Inventory.checkAndRemove(o.items)) {
                    rtaOrders.add(o.id);
                    rta.addOrder(o);
                }

            IloLinearNumExpr waveSize = cplex.linearNumExpr();
            for (Order o : orders)
                waveSize.addTerm(oCP[o.id], o.size);

            cplex.addLe(waveSizeLB, waveSize);
            cplex.addLe(waveSize, waveSizeUB);

            IloLinearNumExpr nAislesCP = cplex.linearNumExpr();
            for (Aisle a : aisles)
                nAislesCP.addTerm(aCP[a.id], 1);

            IloRange topeCP = cplex.addLe(nAislesCP, nAisles);


            int changui = 3;
            double mxrta = waveSizeUB;
            
            IloLinearNumExpr obj = cplex.linearNumExpr();
            while (mnrta + 1e-3 < mxrta && getRemainingTime(stopWatch) > changui) {
                System.out.println("tiempo restante: " + getRemainingTime(stopWatch));
                double thisRta = mnrta + (mxrta - mnrta) / 2.0 ;
                System.out.println("thisRta: " + thisRta);
                obj.clear();
                obj.add(waveSize);
                for (Aisle a : aisles)
                    obj.addTerm(aCP[a.id], -thisRta);

                cplex.addMaximize(obj);

                cplex.setParam(IloCplex.Param.TimeLimit, getRemainingTime(stopWatch) - changui);

                if (cplex.solve() && cplex.getValue(obj) > 0) {

                    System.out.println(cplex.getStatus());
                    System.out.println("encontre solucion");
                    System.out.println("nAislesCP: " + cplex.getValue(nAislesCP));
                    System.out.println("obj: " + cplex.getValue(obj));
                    System.out.println("waveSize: " + cplex.getValue(waveSize));
                    mnrta = cplex.getValue(waveSize) / cplex.getValue(nAislesCP);
                    //

                    rtaAisles.clear();
                    rtaOrders.clear();
                    for (Order o : orders)
                        if (cplex.getValue(oCP[o.id]) > 0.5)
                            rtaOrders.add(o.id);

                    for (Aisle a : aisles)
                        if (cplex.getValue(aCP[a.id]) > 0.5)
                            rtaAisles.add(a.id);

                    System.out.println("nuevoTope: " + (int) (waveSizeUB / mnrta));
                    topeCP.setUB((int) (waveSizeUB / mnrta));
                } else {
                    System.out.println("no encontre solucion");
                    mxrta = thisRta;
                }
                cplex.delete(cplex.getObjective()); 
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