import {View} from "./View";

export interface  ViewStorage {

  storeView(View: View): boolean;
  readView(): View;

}
