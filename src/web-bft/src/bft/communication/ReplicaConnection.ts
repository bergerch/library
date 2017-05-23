/**
 * Created by bergerch on 23.05.17.
 */
import {Injectable} from '@angular/core';
import {Subject, Observer, Observable} from 'rxjs/Rx';
import {Mac} from "./util/Mac";

@Injectable()
export class ReplicaConnection {

  channel: Subject<MessageEvent>;
  macSend: Mac;
  macReceive: Mac;
  replicaId: number;


  public constructor(channel: Subject<MessageEvent>, macSend: Mac, macReceive: Mac, replicaId) {

    this.channel = channel;
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


  public getChannel(): Subject<MessageEvent> {
    return this.channel;
  }

  public getReplicaId(): number {
    return this.replicaId;
  }

}



