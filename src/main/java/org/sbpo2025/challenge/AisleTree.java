package org.sbpo2025.challenge;

import org.sbpo2025.challenge.Heuristica.Aisle;
import org.sbpo2025.challenge.Heuristica.Order;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class AisleTree extends AisleCollection { // deberia crear una clase padre o mismo extender
    private int pasillos = 0;
    public Aisle myAisle;
    protected AisleTree padre;
    public int capacity = 0;

    private AisleTree() {};

    private static final AisleTree NULL_AISLE_TREE = new AisleTree() {

        @Override
        public boolean hasAisle(Aisle a) {
            return false;
        }

        @Override
        public Set<Aisle> getAisles() {
            return new HashSet<>();
        }

        @Override
        public void copy(AisleTree otro) {
            throw new UnsupportedOperationException("Cannot copy into NULL_AISLE_TREE");
        }

        @Override
        public void setAvailable() {
            throw new UnsupportedOperationException("NULL_AISLE_TREE cannot modify availability");
        }
    };

    public static AisleTree nullAisleTree() {
        return NULL_AISLE_TREE;
    }

    public AisleTree(AisleTree _padre, Aisle a) {
        padre = _padre;
        myAisle = a;
        pasillos = _padre.aisleCount() + 1;
        // capacity = padre.capacity + a.size;
    }

    public int aisleCount() {
        return pasillos;
    }

    public boolean hasAisle(Aisle a) {
        if (padre == null)
            return false;
        return a == myAisle || padre.hasAisle(a);
    }

    public Set<Aisle> getAisles() { // no testeado
        Set<Aisle> rta = padre.getAisles();
        rta.add(myAisle);
        return rta;
    }

    public void copy(AisleTree otro) {
        myAisle = otro.myAisle;
        pasillos = otro.pasillos;
        cantItems = otro.cantItems;
        padre = otro.padre;
        // capacity = otro.capacity;
    }

    public double getValue() {
        if (pasillos == 0)
            return 0.0;
        return (double) cantItems / pasillos;
    }

    public int getCapacity() {
        return capacity;
    }

    public void addAisle(Aisle a) {
        throw new IllegalStateException("AisleTree Instance can't add Aisles");
    }

    @Override
    public Iterator<Aisle> iterator() {
        return new Iterator<Aisle>() {
            private AisleTree current = AisleTree.this;

            @Override
            public boolean hasNext() {
                return current.aisleCount() > 0;
            }

            @Override
            public Aisle next() {
                Aisle rta = current.myAisle;
                current = current.padre;
                return rta;
            }
        };
    }
}
