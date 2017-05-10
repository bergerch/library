export interface Comparator {

  /**
   * Compares two objects, returns 0 if matching or -1 if not
   * @param o1
   * @param o2
   */
  compare(o1: any, o2:any): number;

}
