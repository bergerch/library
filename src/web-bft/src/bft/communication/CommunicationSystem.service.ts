/**
 * Created by chris on 23.04.17.
 */

import {Injectable} from '@angular/core';
import {ReplyReceiver} from "bft/tom/TOMSender.service";
import {TOMMessage} from "../tom/messages/TOMMessage";
import {ClientViewController} from "../reconfiguration/ClientViewController.controller";
import {ReplicaConnection} from "./ReplicaConnection";
import {WebsocketService} from "./Websocket.service";
import {Subject} from "rxjs/Subject";

export interface ICommunicationSystem {

  send(sign: boolean, targets: number[], sm: TOMMessage);
  setReplyReceiver(trr: ReplyReceiver);
  sign(sm: TOMMessage);
  close();
  updateConnections();

}

@Injectable()
export class CommunicationSystem implements ICommunicationSystem {

  clientId: number;
  trr: ReplyReceiver;
  clientViewController: Object;
  sessionTable: Map<string, ReplicaConnection> = new Map<string, ReplicaConnection>();
  reentrantReadWriteLock: Object;

  //the signature engine used in the system
  signatureEngine: Object;
  signatureLength: number;
  closed: boolean = false;

  protected socket: Subject<any>;
  protected message: any;
  protected websocketService: WebsocketService;



  public constructor(clientId: number, viewController: ClientViewController) {
    this.websocketService = new WebsocketService();
    // For testing purpose
    this.socket = this.websocketService.createWebsocket('ws://localhost:9000');
    this.socket.subscribe((message) => {
      this.message = message.data;
    });
  }

  public send(sign: boolean, targets: number[], sm: TOMMessage) {
    console.log('CommunicationSystem send() called ');
    this.socket.next(sm);
  }

  public setReplyReceiver(trr: ReplyReceiver) {

  }

  public sign(sm: TOMMessage) {

  }

  public close() {

  }

  public updateConnections() {

  }

  public reconnect() {

  }


}
