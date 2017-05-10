import {Injectable} from "@angular/core";
import {ViewController} from "./ViewController.controller";
import {TOMConfiguration} from "bft/config/TOMConfiguration";

@Injectable()
export class ClientViewController extends ViewController {

  constructor(procId: number, private TOMConfiguration: TOMConfiguration) {
    super(procId, TOMConfiguration);
  }


  public updateCurrentViewFromRepository(): void {
    //this.currentView = getViewStore().readView();
  }

}
