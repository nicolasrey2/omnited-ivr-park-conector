package coop.bancocredicoop.omnited.utils;


public class TimeUtils {
  private TimeUtils() {
    // Evita instanciación
  }

  public static <T extends Comparable<T>> boolean isBetween(T actual, T inicio, T fin) {
    if (inicio.compareTo(fin) < 0) {
      // rango normal
      return actual.compareTo(inicio) >= 0 && actual.compareTo(fin) <= 0;
    } else {
      // rango invertido (ej: cruza medianoche)
      return actual.compareTo(inicio) >= 0 || actual.compareTo(fin) <= 0;
    }
  }

}
