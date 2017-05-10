import {Injectable} from "@angular/core";


@Injectable()
export class Configuration {

  processId: number;
  channelsBlocking: boolean;
  DH_P: string;
  DH_G: string;
  autoConnectLimit: number;
  configs: Map<string, string>;
  hosts: HostsConfig;


  constructor(procId: number) {
    this.processId = procId;
  }

}
