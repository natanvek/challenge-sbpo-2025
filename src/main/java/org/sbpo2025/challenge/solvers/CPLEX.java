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

    List<PriorityQueue<Heuristica.EfficientCart>> rankings;

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

    Set<Integer> rtaOrders, rtaAisles;

    public void init() {

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
        System.out.println("Solucion H2: " + rta.getValue());

        rankings = new ArrayList<>();
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

    IloCplex cplex;
    IloNumVar[] aCP, oCP;
    IloLinearNumExpr waveSize, nAislesCP;
    IloRange aisleRange;
    Double mnrta;
    List<IloRange> heuristicConstraints;
    IloLinearNumExpr obj;
    IloRange haySolucion;

    public void setUpCplex() throws IloException {
        cplex.setOut(null);

        aCP = cplex.boolVarArray(nAisles);
        oCP = cplex.boolVarArray(nOrders);

        for (Order o : orders) {
            if (o.size > waveSizeUB)
                cplex.addEq(oCP[o.id], 0);
        }

        // --------------------------------------------------------
        Map<Integer, Integer> disponible = new HashMap<>();
        for (Aisle a : aisles) {
            for (Map.Entry<Integer, Integer> entry : a.items.entrySet()) {
                disponible.put(entry.getKey(), disponible.getOrDefault(entry.getKey(), 0) + entry.getValue());
            }
        }

        for (Order o : orders) {
            for (Map.Entry<Integer, Integer> entry : o.items.entrySet()) {
                if (disponible.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                    cplex.addEq(oCP[o.id], 0);
                    break;
                }
            }
        }

        for (int i = 0; i < nItems; i++) {
            IloLinearNumExpr item_i_enOrders = cplex.linearNumExpr();
            IloLinearNumExpr item_i_enAisles = cplex.linearNumExpr();
            for (Order o : orders)
                item_i_enOrders.addTerm(oCP[o.id], o.items.getOrDefault(i, 0));

            for (Aisle a : aisles)
                item_i_enAisles.addTerm(aCP[a.id], a.items.getOrDefault(i, 0));

            cplex.addLe(item_i_enOrders, item_i_enAisles);
        }

        waveSize = cplex.linearNumExpr();
        for (Order o : orders)
            waveSize.addTerm(oCP[o.id], o.size);

        cplex.addRange(waveSizeLB, waveSize, waveSizeUB);

        nAislesCP = cplex.linearNumExpr();
        for (Aisle a : aisles)
            nAislesCP.addTerm(aCP[a.id], 1);

        aisleRange = cplex.addRange(1, nAislesCP, tope);

        obj = cplex.linearNumExpr();
        haySolucion = cplex.addLe(1e-3, obj);

        cplex.setParam(IloCplex.Param.Emphasis.MIP, 1);
        cplex.setParam(IloCplex.Param.MIP.Strategy.HeuristicFreq, 3);
        cplex.setParam(IloCplex.Param.MIP.Limits.CutsFactor, 1.0);
        cplex.setParam(IloCplex.Param.MIP.Cuts.Gomory, -1); 
        cplex.setParam(IloCplex.Param.MIP.Cuts.MIRCut, -1);
        cplex.setParam(IloCplex.Param.MIP.Cuts.Implied, -1);
    }

    public boolean runCplex(StopWatch stopWatch) throws IloException {
        int changui = 20;
        if (getRemainingTime(stopWatch) <= changui)
            return false;

        cplex.setParam(IloCplex.Param.TimeLimit, getRemainingTime(stopWatch) - changui);

        if (cplex.solve()) {
            Double posiblerta = cplex.getValue(waveSize) / cplex.getValue(nAislesCP);
            System.out.println("encontre solucion nAislesCP: " + cplex.getValue(nAislesCP) + " rta: " + posiblerta);

            if (mnrta < posiblerta) {
                mnrta = posiblerta;
                rtaAisles.clear();
                rtaOrders.clear();
                for (Order o : orders)
                    if (cplex.getValue(oCP[o.id]) > 0.5)
                        rtaOrders.add(o.id);

                for (Aisle a : aisles)
                    if (cplex.getValue(aCP[a.id]) > 0.5)
                        rtaAisles.add(a.id);
            }

            aisleRange.setUB(waveSizeUB / mnrta);
            if (getRemainingTime(stopWatch) <= changui) {
                System.out.println("me quede sin tiempo y no halle solucion mejor");
                return false;
            }

            return true;
        } else {
            if (getRemainingTime(stopWatch) > changui) {
                System.out.println("la solucion hallada es optima");
                return false;
            } else {
                System.out.println("me quede sin tiempo y no halle solucion mejor");
                return false;
            }
        }
    }

    public void findOptimalSolution(StopWatch stopWatch) throws IloException {

        aisleRange.setLB(1);
        aisleRange.setUB(tope);
        while (true) {
            System.out.println("-----------------------------");
            System.out.println("tiempo restante: " + getRemainingTime(stopWatch));
            double thisRta = mnrta + 0.001;
            obj.clear();
            obj.add(waveSize);
            for (Aisle a : aisles)
                obj.addTerm(aCP[a.id], -thisRta);

            haySolucion.setExpr(obj);
            cplex.delete(cplex.getObjective());
            if (!runCplex(stopWatch))
                break;
        }

    }

    @Override
    public ChallengeSolution solve(StopWatch stopWatch) {
        init();
        mnrta = rta.getValue();

        System.out.println("nAisles inicial: " + nAisles);
        System.out.println("tope inicial: " + tope);
        System.out.println("nOrders inicial: " + nOrders);
        System.out.println("nAislesCP: " + rtaAisles.size());
        System.out.println("Solucion Ranking: " + mnrta);

        try {
            cplex = new IloCplex();
            setUpCplex();

            System.out.println("-----------------------------");
            System.out.println("BUSCANDO SULUCION OPTIMA");
            findOptimalSolution(stopWatch);

        } catch (

        Exception e) {
            e.printStackTrace(System.out);
            System.out.println("algo salio mal con CPLEX");
        }

        return new ChallengeSolution(rtaOrders, rtaAisles);

    }
}

