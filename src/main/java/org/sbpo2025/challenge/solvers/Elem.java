package org.sbpo2025.challenge.solvers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface Elem {

    public class Aisle {
        public int aisleIdx;
        public double delta;
        public Map<Integer, Integer> orderElems;
        
        Aisle(int aisleIdx, double delta, Map<Integer, Integer> orderElems) {
            this.aisleIdx = aisleIdx;
            this.delta = delta;
            this.orderElems = orderElems;
        };
    }
    
    public class Order {
        public boolean valid;
        public int orderIdx;
        public double delta;
        public int nElems;
        public Set<Aisle> aisles;

        Order(boolean valid, int orderIdx, double delta, int nElems, Set<Aisle> aisles) {
            this.valid = valid;
            this.orderIdx = orderIdx;
            this.delta = delta;
            this.nElems = nElems;
            this.aisles = aisles;
        }

        Set<Integer> aisleIdxs() {
            Set<Integer> idxs = new HashSet<>();
            for (Aisle aisle : aisles) {
                idxs.add(aisle.aisleIdx);
            }
            return idxs;
        }

        int intersectingAislesCount(Set<Integer>  idxs) {
            Set<Integer> ownIdxs = aisleIdxs();
            ownIdxs.retainAll(idxs);
            return ownIdxs.size();
        }

        void replace(Order other) {
            this.valid = other.valid;
            this.orderIdx = other.orderIdx;
            this.delta = other.delta;
            this.nElems = other.nElems;
            this.aisles = other.aisles; // shallow
        }
    }
}
