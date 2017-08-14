/**
 * Created by bergerch on 23.05.17.
 */
import {Injectable} from '@angular/core';
import {Subject} from 'rxjs/Rx';
import {Replica} from "./Replica";
import {ReplicaConnection} from "./ReplicaConnection";


@Injectable()
export class ReplicaWSConnection extends Replica implements ReplicaConnection {

  socket: Subject<any>;

  public constructor(socket: Subject<any>, replicaId, secret?: string) {

    super(replicaId, secret);
    this.socket = socket;

  }

  public send(message) {
    this.socket.next(message);
  }

  public subscribe(subscription) {
    this.socket.subscribe(subscription);
  }

  public close() {
    this.socket.complete();
  }
}



