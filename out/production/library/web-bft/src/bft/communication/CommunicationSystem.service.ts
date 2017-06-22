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
import {View} from "../reconfiguration/View";
import {InternetAddress} from "../config/TOMConfiguration";

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
  protected message: any[] = [];
  protected websocketService: WebsocketService;


  public constructor(clientId: number, viewController: ClientViewController) {
    this.websocketService = new WebsocketService();


    console.log('Current View is: ');
    viewController.getCurrentView().addresses.forEach((value: InternetAddress, key: number) => {
      console.log('|->', value);

      let address: string = 'ws://' + value.address + ':' + value.port;
      let socket: Subject<any> = this.websocketService.createWebsocket(address);

      // For testing purpose
      socket.subscribe((data) => {
        this.message[key] = data;
        console.log('Received: ', data);
      });

      let connection: ReplicaConnection = new ReplicaConnection(socket, null, null, key);
      this.sessionTable.set(address, connection);
    });
  }

  public send(sign: boolean, targets: number[], sm: TOMMessage) {
    console.log('CommunicationSystem send() called ', sm);

    this.sessionTable.forEach((connection: ReplicaConnection, address: string) => {

      connection.getSocket().next(sm);
      console.log('Sending to ' + address);

    });

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
