import {Injectable} from '@angular/core';
import {View} from "./View";
import {ViewStorage} from "./ViewStorage.interface";
import {TOMConfiguration} from "../config/TOMConfiguration";
import {DefaultViewStorage} from "./DefaultViewStorage";
import {log} from "util";


@Injectable()
export class ViewController {

  lastView: View = null;
  currentView: View = null;
  viewStore: ViewStorage;

  public constructor(procId: number, TOMConfiguration: TOMConfiguration) {

    this.currentView = new View(0, TOMConfiguration.initial_view, TOMConfiguration.f, TOMConfiguration.hosts);
    this.viewStore = new DefaultViewStorage();
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

  public getCurrentViewId() {
    return this.currentView.id;
  }

  public getViewStore(): ViewStorage {
    if (!this.viewStore) {
      this.viewStore = new DefaultViewStorage();
    }
    return this.viewStore;
  }

  public getCurrentViewF() {
    return this.currentView.f;
  }

  public getCurrentViewN() {
    return this.currentView.processes.length;
  }

  public setCurrentView(view: View) {
    console.log('current View', view);
    this.lastView = this.currentView;
    this.currentView = view;
    this.viewStore.storeView(view);
  }

}