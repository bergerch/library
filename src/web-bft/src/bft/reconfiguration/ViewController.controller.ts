import {Injectable} from '@angular/core';
import {View} from "./View";

export interface  ViewStorage {

  toreView(View: View): boolean;
  readView(): View;

}

@Injectable()
export class ViewController {

  lastView: View = null;
  currentView: View = null;
  viewStore: ViewStorage;

  public ViewController() {
    // TODO
  }


  getCurrentView(): View {
    if (this.currentView == null) {
      // this.currentView = getViewStore().readView();
    }
    return this.currentView;
  }


}
