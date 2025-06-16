package org.sbpo2025.challenge;

import org.sbpo2025.challenge.Heuristica.Aisle;
import java.util.*;

public class Inventory {
    // Arrays estáticos compartidos por todas las instancias de la clase
    private static int[] available; // Array para los items disponibles
    private static int[] modifiedDate; // Array para las fechas de modificación
    private static int currentDate = 1; // Fecha global para todos

    // private Map<Integer, Integer> available;

    public static void initializeStaticVar(int nItems) {
        available = new int[nItems]; // Inicializa el array solo una vez
        modifiedDate = new int[nItems]; // Inicializa el array de fechas solo una vez
    }

    public static void reset() {
        ++currentDate;
    }

    public static boolean checkAndRemove(Map<Integer, Integer> m) {

        for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
            int elem = entry.getKey(), cant = entry.getValue();
            if(modifiedDate[elem] < currentDate) {
                modifiedDate[elem] = currentDate;
                available[elem] = 0;
            }

            if (available[elem] < cant)
                return false;
        }
        for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
            int elem = entry.getKey(), cant = entry.getValue();
            available[elem] -= cant;
        }

        return true;
    }

    public static void addAisle(Aisle a) {
        for (Map.Entry<Integer, Integer> entry : a.items.entrySet()){
            if(modifiedDate[entry.getKey()] < currentDate) 
                available[entry.getKey()] = 0;
                
            modifiedDate[entry.getKey()] = currentDate;
            available[entry.getKey()] += entry.getValue();
        }
    }
}