package org.sbpo2025.challenge;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public abstract class Heuristica extends ChallengeSolver {

    protected static class Order {
        public int id, size, pos;
        public Map<Integer, Integer> items;

        public Order(int _id, Map<Integer, Integer> _items, int _size) {
            this.id = _id;
            this.items = _items;
            this.size = _size;
        }
    }

    protected static class Aisle {
        public int id, size, pos;
        public Map<Integer, Integer> items;

        public Aisle(int _id, Map<Integer, Integer> _items, int _size) {
            this.id = _id;
            this.items = _items;
            this.size = _size;
        }
    }

    protected Order[] orders;
    protected Aisle[] aisles;

    protected int nOrders;
    protected int nAisles;
    protected int nItems;
    protected int tope;
    protected EfficientCart rta;

    public Heuristica(List<Map<Integer, Integer>> _orders, List<Map<Integer, Integer>> _aisles, int _nItems,
            int waveSizeLB, int waveSizeUB) {
        super(_orders, _aisles, _nItems, waveSizeLB, waveSizeUB);

        aisles = new Aisle[_aisles.size()];
        nItems = _nItems;

        Inventory.initializeStaticVar(nItems);

        for (int a = 0; a < _aisles.size(); a++) {
            aisles[a] = new Aisle(a, new HashMap<>(_aisles.get(a)), 0);
            for (Map.Entry<Integer, Integer> entry : _aisles.get(a).entrySet())
                aisles[a].size += entry.getValue();
        }

        List<Order> ordersh_aux = new ArrayList<>();

        for (int o = 0; o < _orders.size(); o++) {
            int orderSize = 0;
            boolean anda = true;
            for (Map.Entry<Integer, Integer> entry : _orders.get(o).entrySet()) {
                Integer elem = entry.getKey();
                Integer cant = entry.getValue();
                orderSize += cant;
                int hay = 0;
                for (Aisle a : aisles)
                    hay += Math.min(a.items.getOrDefault(elem, 0), cant);

                if (hay < cant)
                    anda = false;
            }

            if (orderSize <= waveSizeUB && anda)
                ordersh_aux.add(new Order(o, _orders.get(o), orderSize));

        }

        orders = ordersh_aux.toArray(new Order[0]);

        nAisles = aisles.length;
        tope = nAisles;
        nOrders = orders.length;

        rta = new EfficientCart();
    }

    protected class EfficientCart { // deberia crear una clase padre o mismo extender

        public Set<Aisle> my_aisles = new HashSet<>();
        public int cantItems = 0;

        public int aisleCount() {
            return my_aisles.size();
        }

        public boolean hasAisle(Aisle a) {
            return my_aisles.contains(a);
        }

        public Set<Aisle> getAisles() {
            return my_aisles;
        }

        public void copy(EfficientCart otro) {
            my_aisles.clear();
            my_aisles.addAll(otro.my_aisles);
            cantItems = otro.cantItems;
        }

        public EfficientCart(EfficientCart otro) {
            copy(otro);
        }

        public EfficientCart() {
        }

        public double getValue() {
            if (aisleCount() == 0)
                return 0.0;
            return (double) cantItems / aisleCount();
        }

        public int getCantItems() {
            return cantItems;
        }

        public int getTope() {
            if (getValue() < 1e-5)
                return nAisles;
            return Math.min((int) Math.floor(waveSizeUB / getValue()), nAisles);
        }

        public boolean update(EfficientCart otro) {
            if (getValue() >= otro.getValue())
                return false;

            cantItems = otro.cantItems;
            my_aisles = otro.my_aisles;
            return true;
        }

        public void addAisle(Aisle a) {
            my_aisles.add(a);
        }

        public void addOrder(Order o) {
            cantItems += o.size;
        }

        public void setAvailable() {
            Inventory.reset();
            cantItems = 0;

            for (Aisle a : my_aisles)
                Inventory.addAisle(a);
        }

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

    protected List<PriorityQueue<Heuristica.EfficientCart>> rankings, rankings2;

    public List<Integer> bestSizes(int cuantos) {
        PriorityQueue<Heuristica.EfficientCart> bestSizes = new PriorityQueue<>(
                Comparator.comparingDouble(EfficientCart::getValue));

        for (int r = 1; r <= tope; ++r) {
            PriorityQueue<Heuristica.EfficientCart> actual = new PriorityQueue<>(
                    Comparator.comparingInt(EfficientCart::getCantItems));
            for (EfficientCart m : rankings.get(r))
                if (m.getCantItems() >= waveSizeLB)
                    insertCart(actual, m, 1);

            if (!actual.isEmpty())
                insertCart(bestSizes, actual.peek(), cuantos);
        }

        List<Integer> bestSizesList = new ArrayList<>();
        for (EfficientCart m : bestSizes) {
            bestSizesList.add(m.aisleCount());
        }
        return bestSizesList;
    }

    public void updateRta(EfficientCart ec) {
        EfficientCart copy = new EfficientCart(ec);
        if (ec.cantItems >= waveSizeLB) {
            rta.update(copy);
            tope = Math.min(tope, rta.getTope());
        }
    }

    public void fill(EfficientCart ec) {
        ec.setAvailable();
        for (Order o : orders)
            if (ec.getCantItems() + o.size <= waveSizeUB && Inventory.checkAndRemove(o.items))
                ec.addOrder(o);
    }

    protected void pasada() {
        EfficientCart actual = new EfficientCart();
        for (int sol = 0; sol < tope; ++sol) {
            actual.addAisle(aisles[sol]);
            fill(actual);
            updateRta(actual);
        }
    }


    public void printElapsedTime(StopWatch stopWatch) {
        long elapsedMillis = stopWatch.getNanoTime() / 1_000_000;
        long hours = elapsedMillis / (1000 * 60 * 60);
        long minutes = (elapsedMillis / (1000 * 60)) % 60;
        long seconds = (elapsedMillis / 1000) % 60;
        System.out.println(String.format("Tiempo transcurrido: %02d:%02d:%02d", hours, minutes, seconds));
    }

    protected IloCplex cplex;
    protected IloLinearNumExpr waveSize, nAislesCP;
    protected IloRange aisleRange;
    protected Double mnrta;
    protected IloNumVar[] aCP, oCP;
    protected IloNumExpr obj;
    protected IloRange haySolucion;

    protected List<Aisle> aislesVan;

    // pasar de Set<Order> a array fijo
    protected List<Order> ordersVan;
    protected int changui = 7;
    protected Set<Integer> rtaOrders, rtaAisles;

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

    }

    public void setUpCplex() throws IloException {

        cplex.setOut(null);

        Set<Aisle> aislesVanAux = new HashSet<>();

        for (Aisle a : aisles)
            aislesVanAux.add(a);

        Set<Order> ordersVanAux = new HashSet<>();

        // --------------------------------------------------------

        // --------------------------------------------------------
        aislesVan = new ArrayList<>(aislesVanAux);
        Map<Integer, Integer> disponible = new HashMap<>();
        for (Aisle a : aislesVan)
            for (Map.Entry<Integer, Integer> entry : a.items.entrySet())
                disponible.put(entry.getKey(), disponible.getOrDefault(entry.getKey(), 0) + entry.getValue());

        for (Order o : orders) {
            ordersVanAux.add(o);
            for (Map.Entry<Integer, Integer> entry : o.items.entrySet()) {
                if (disponible.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                    ordersVanAux.remove(o);
                    break;
                }
            }
        }

        for (Order o : orders)
            if (o.size > waveSizeUB)
                ordersVanAux.remove(o);

        ordersVan = new ArrayList<>(ordersVanAux);

        aCP = cplex.boolVarArray(aislesVan.size());
        oCP = cplex.boolVarArray(ordersVan.size());

        for (int i = 0; i < aislesVan.size(); ++i)
            aislesVan.get(i).pos = i;

        for (int i = 0; i < ordersVan.size(); ++i)
            ordersVan.get(i).pos = i;

        for (int i = 0; i < nItems; i++) {
            IloLinearNumExpr item_i_enOrders = cplex.linearNumExpr();
            IloLinearNumExpr item_i_enAisles = cplex.linearNumExpr();
            for (Order o : ordersVan)
                if (o.items.getOrDefault(i, 0) > 0)
                    item_i_enOrders.addTerm(oCP[o.pos], o.items.getOrDefault(i, 0));

            for (Aisle a : aislesVan)
                if (a.items.getOrDefault(i, 0) > 0)
                    item_i_enAisles.addTerm(aCP[a.pos], a.items.getOrDefault(i, 0));

            cplex.addLe(item_i_enOrders, item_i_enAisles);
        }

        waveSize = cplex.linearNumExpr();
        for (Order o : ordersVan)
            waveSize.addTerm(oCP[o.pos], o.size);

        cplex.addRange(waveSizeLB, waveSize, waveSizeUB);

        nAislesCP = cplex.linearNumExpr();
        for (Aisle a : aislesVan)
            nAislesCP.addTerm(aCP[a.pos], 1);

        aisleRange = cplex.addRange(1, nAislesCP, tope);

        // imprimir range

        obj = cplex.numExpr();
        haySolucion = cplex.addLe(1e-3, obj);

    }

    public boolean runCplex(StopWatch stopWatch) throws IloException {
        if (getRemainingTime(stopWatch) <= changui)
            return false;

        cplex.setParam(IloCplex.Param.TimeLimit, getRemainingTime(stopWatch) - changui);

        if (cplex.solve()) {
            Double posiblerta = cplex.getValue(waveSize) / cplex.getValue(nAislesCP);

            if (mnrta < posiblerta) {
                mnrta = posiblerta;
                rtaAisles.clear();
                rtaOrders.clear();

                for (Order o : ordersVan)
                    if (cplex.getValue(oCP[o.pos]) > 0.5)
                        rtaOrders.add(o.id);

                for (Aisle a : aislesVan)
                    if (cplex.getValue(aCP[a.pos]) > 0.5)
                        rtaAisles.add(a.id);

                if (Math.floor(waveSizeUB / mnrta + 1e-5) < Math.floor(aisleRange.getUB() + 1e-5))
                    aisleRange.setUB(Math.floor(waveSizeUB / mnrta + 1e-4));
            }

            if (getRemainingTime(stopWatch) <= changui) {
                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    public void findOptimalSolution(StopWatch stopWatch) throws IloException {

        do {
            double thisRta = mnrta + 0.005;
            haySolucion.setExpr(cplex.sum(waveSize, cplex.prod(-thisRta, nAislesCP)));
        } while (runCplex(stopWatch));

    }

    public int calcRegisterSize(double minutos) {
        StopWatch stopWatch = StopWatch.createStarted();
        long ti = stopWatch.getNanoTime(), tiempo_iterando = 0, iteraciones = 0;
        while (tiempo_iterando < 5e9) {
            for (int i = 0; i < 100; ++i) {
                EfficientCart simulatingBest = new EfficientCart();

                List<Aisle> perm = new ArrayList<>();
                for (Aisle a : aisles)
                    perm.add(a);
                Collections.shuffle(perm);

                for (int r = 0; r < tope; ++r) {
                    EfficientCart estimatingRegisterSize = new EfficientCart(simulatingBest);
                    estimatingRegisterSize.addAisle(perm.get(r));
                    fill(estimatingRegisterSize);
                    simulatingBest = estimatingRegisterSize;
                }
            }
            tiempo_iterando = (stopWatch.getNanoTime() - ti);
            iteraciones += 100;
        }
        double tope_por_fill = (double) tiempo_iterando / (iteraciones * 1e6);
        return Math.max(1, Math.min(1000, (int) ((minutos * 1000.0 * 60.0) / (tope_por_fill * nAisles))));
    }

}
