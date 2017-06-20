export interface Comparator<T> {

  /**
   * Compares two objects, returns 0 if matching or -1 if not
   * @param o1
   * @param o2
   */
  compare<T>(o1: T, o2: T): number;

}
