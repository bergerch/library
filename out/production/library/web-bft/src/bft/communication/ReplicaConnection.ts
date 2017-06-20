/**
 * Created by bergerch on 23.05.17.
 */
import {Injectable} from '@angular/core';
import {Subject, Observer, Observable} from 'rxjs/Rx';
import {Mac} from "./util/Mac";

@Injectable()
export class ReplicaConnection {

  socket: Subject<any>;
  macSend: Mac;
  macReceive: Mac;
  replicaId: number;


  public constructor(socket: Subject<any>, macSend: Mac, macReceive: Mac, replicaId) {

    this.socket = socket;
    this.macSend = macSend;
    this.macReceive = macReceive;
    this.replicaId = replicaId;

  }

  public getMacReceive(): Mac {
    return this.macReceive;
  }


  public getMacSend(): Mac {
    return this.macSend;
  }


  public getSocket(): Subject<any> {
    return this.socket;
  }

  public getReplicaId(): number {
    return this.replicaId;
  }

}



