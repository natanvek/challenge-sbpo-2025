package org.sbpo2025.challenge;

import org.sbpo2025.challenge.Heuristica.Aisle;
import java.util.*;

public class Inventory {
    // Arrays estáticos compartidos por todas las instancias de la clase
    private int[] available; // Array para los items disponibles
    private int[] modifiedDate; // Array para las fechas de modificación
    private int currentDate = 1; // Fecha global para todos

    // private Map<Integer, Integer> available;

    Inventory(int nItems) {
        available = new int[nItems]; // Inicializa el array solo una vez
        modifiedDate = new int[nItems]; // Inicializa el array de fechas solo una vez
    }
    
    Inventory(Inventory otro) {
        int nItems = otro.available.length;
        available = new int[nItems];
        modifiedDate = new int[nItems];
        for (int i = 0; i < nItems; ++i) {
            available[i] = otro.available[i];
            modifiedDate[i] = otro.modifiedDate[i];
        }
        currentDate = otro.currentDate;
    }

    public void reset() {
        ++currentDate;
    }

    public boolean checkAndRemove(List<Map.Entry<Integer, Integer>> m) {
        for (Map.Entry<Integer, Integer> entry : m) {
            int elem = entry.getKey(), cant = entry.getValue();
            if(modifiedDate[elem] < currentDate) {
                modifiedDate[elem] = currentDate;
                available[elem] = 0;
            }

            if (available[elem] < cant)
                return false;
        }
        for (Map.Entry<Integer, Integer> entry : m) {
            int elem = entry.getKey(), cant = entry.getValue();
            available[elem] -= cant;
        }

        return true;
    }

    public void addAisle(Aisle a) {
        for (Map.Entry<Integer, Integer> entry : a.items){
            if(modifiedDate[entry.getKey()] < currentDate) 
                available[entry.getKey()] = 0;
                
            modifiedDate[entry.getKey()] = currentDate;
            available[entry.getKey()] += entry.getValue();
        }
    }
}