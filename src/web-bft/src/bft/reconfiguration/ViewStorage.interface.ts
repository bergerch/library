import {View} from "./View";

export interface  ViewStorage {

  storeView(View: View): void;
  readView(): View;

}
