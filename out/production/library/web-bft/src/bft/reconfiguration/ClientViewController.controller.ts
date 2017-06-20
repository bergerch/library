import {Injectable} from "@angular/core";
import {ViewController} from "./ViewController.controller";
import {TOMConfiguration} from "bft/config/TOMConfiguration";

@Injectable()
export class ClientViewController extends ViewController {

  public constructor(procId: number, private TOMConfiguration: TOMConfiguration) {
    super(procId, TOMConfiguration);
  }


  public updateCurrentView(): void {
    this.currentView = this.getViewStore().readView();
  }

}
