import {Injectable} from '@angular/core';
import {View} from "./View";
import {ViewStorage} from "./ViewStorage.interface";
import {TOMConfiguration} from "../config/TOMConfiguration";


@Injectable()
export class ViewController {

  lastView: View = null;
  currentView: View = null;
  viewStore: ViewStorage;

  public constructor(procId: number, TOMConfiguration: TOMConfiguration) {
    // TODO Config.
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

  public getCurrentViewPos(id: number): number {
  return this.getCurrentView().getPos(id);
  }

  public getViewStore(): ViewStorage {
    if (this.viewStore == null) {


      // TODO this.viewStore = ;

    }
    return this.viewStore;
  }


}
