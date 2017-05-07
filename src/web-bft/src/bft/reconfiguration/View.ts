import {Injectable} from '@angular/core';
import {InternetAddress} from "../config/Config";


@Injectable()
export class View {

  id: number;
  f: number;
  processes: number[];
  addresses: Map<number, InternetAddress>;

  public View(id: number, processes: number[], f: number, addresses: InternetAddress[]) {
    this.id = id;
    this.f = f;
    this.processes = processes;
    this.addresses = new Map<number, InternetAddress>();

    for (let i = 0; i < this.processes.length; i++) {
      this.addresses.set(processes[i], addresses[i]);
    }

    this.processes.sort((n1, n2) => n1 - n2);

  }

  isMember(id: number): boolean {
    for (let i = 0; i < this.processes.length; i++) {
      if (this.processes[i] === id) {
        return true;
      }
    }
    return false;
  }

  getPos(id: number): number {
    for (let i = 0; i < this.processes.length; i++) {
      if (this.processes[i] == id) {
        return i;
      }
    }
    return -1;
  }

}
