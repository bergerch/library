import {Injectable} from '@angular/core';
import {View} from "./View";
import {ViewStorage} from "./ViewStorage.interface";
import {TOMConfiguration} from "../config/TOMConfiguration";
import {DefaultViewStorage} from "./DefaultViewStorage";


@Injectable()
export class ViewController {

  lastView: View = null;
  currentView: View = null;
  viewStore: ViewStorage;

  public constructor(procId: number, TOMConfiguration: TOMConfiguration) {
    
    this.currentView = new View(0, TOMConfiguration.initial_view, TOMConfiguration.f, TOMConfiguration.hosts);
  }

  public getCurrentViewProcesses() {
    return this.getCurrentView().processes;
  }

  public getCurrentView(): View {
    if (this.currentView == null) {
       this.currentView = this.getViewStore().readView();
    }
    return this.currentView;
  }

  public getCurrentViewPos(id: number): number {
  return this.getCurrentView().getPos(id);
  }

  public getViewStore(): ViewStorage {
    if (!this.viewStore) {
      this.viewStore = new DefaultViewStorage();
    }
    return this.viewStore;
  }


}
