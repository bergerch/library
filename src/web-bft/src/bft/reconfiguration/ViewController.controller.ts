import {Injectable} from '@angular/core';
import {View} from "./View";
import {ViewStorage} from "./ViewStorage.interface";
import {TOMConfiguration} from "../config/TOMConfiguration";


@Injectable()
export class ViewController {

  lastView: View = null;
  currentView: View = null;
  viewStore: ViewStorage;

  constructor(procId: number, TOMConfiguration: TOMConfiguration) {
  //Config.
  }

  public getCurrentViewProcesses() {
    return this.getCurrentView().processes;
  }

  public getCurrentView(): View {
    if (this.currentView == null) {
      // this.currentView = getViewStore().readView();
    }
    return this.currentView;
  }


}
