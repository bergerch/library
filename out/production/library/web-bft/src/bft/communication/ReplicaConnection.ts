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
  secret: string;


  public constructor(socket: Subject<any>, macSend: Mac, macReceive: Mac, replicaId, secret?: string) {

    this.socket = socket;
    this.macSend = macSend;
    this.macReceive = macReceive;
    this.replicaId = replicaId;
    this.secret = secret;

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


 public getSecret(): string {
    return this.secret;
  }

  public setSecret(value: string) {
    this.secret = value;
  }
}



