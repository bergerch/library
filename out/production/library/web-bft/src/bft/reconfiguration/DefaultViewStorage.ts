import {ViewStorage} from "./ViewStorage.interface";
import {View} from "./View";
import {Injectable} from "@angular/core";

@Injectable()
export class DefaultViewStorage implements ViewStorage {


  public storeView(view: View): void {

    console.log('DEBUG storeView()');
    if (view !== this.readView()) {
      localStorage.setItem('view', JSON.stringify(view));
      console.log('View Storage: Set View to ', view);
    }
  }


  public readView(): View {

    console.log('DEBUG readView()');
    if (localStorage.getItem('view')) {
      console.log('View Storage: Read view ', localStorage.getItem('view'));
      return JSON.parse(localStorage.getItem('view'));
    } else {
      console.log('View Storage: Failed reading view!');
      return null;
    }

  }

}
