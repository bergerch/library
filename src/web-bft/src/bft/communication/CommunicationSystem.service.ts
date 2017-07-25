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
import * as CryptoJS from "../../../node_modules/crypto-js/crypto-js.js";


export interface ICommunicationSystem {

  send(sign: boolean, targets: number[], sm: TOMMessage, replyReceiver?: ReplyReceiver);
  setReplyReceiver(trr: ReplyReceiver);
  sign(sm: TOMMessage);
  close();
  updateConnections();

}

@Injectable()
export class CommunicationSystem implements ICommunicationSystem {

  clientId: number;
  replyReceiver: ReplyReceiver;
  clientViewController: ClientViewController;
  sessionTable: Map<number, ReplicaConnection> = new Map<number, ReplicaConnection>();

  //the signature engine used in the system
  signatureEngine: Object;
  signatureLength: number;
  closed: boolean = false;

  protected websocketService: WebsocketService;


  public constructor(clientId: number, viewController: ClientViewController) {
    this.websocketService = new WebsocketService();
    this.clientViewController = viewController;

    console.log('Current View is: ');
    viewController.getCurrentView().addresses.forEach((value: InternetAddress, key: number) => {
      console.log('|->', value);

      let address: string = 'ws://' + value.address + ':' + value.port;
      let socket: Subject<any> = this.websocketService.createWebsocket(address);
      let connection: ReplicaConnection = new ReplicaConnection(socket, null, null, key);
      this.sessionTable.set(key, connection);
    });

    // Test purpose, test HMAC generation
    console.log(CryptoJS.enc.Hex.stringify(CryptoJS.HmacSHA1('Message', 'Key')));
  }

  public send(sign: boolean, targets: number[], sm: TOMMessage, replyReceiver?: ReplyReceiver) {

    // Subscribe: When reply is received, parse JSON and execute replyListener
    this.sessionTable.forEach((connection: ReplicaConnection, replicaId: number) => {
      connection.getSocket().subscribe((reply) => {
        replyReceiver.replyReceived(JSON.parse(reply.data));
      });
    });

    // Send Message to all replicas
    this.sessionTable.forEach((connection: ReplicaConnection, replicaId: number) => {
      connection.getSocket().next(sm);
    });

    console.log('send ', sm);
  }

  public setReplyReceiver(replyReceiver: ReplyReceiver) {
    this.replyReceiver = replyReceiver;
  }

  public sign(sm: TOMMessage) {

  }

  public close() {

  }

  public updateConnections() {

    if (!this.websocketService) {
      this.websocketService = new WebsocketService();
    }

    let oldView: View = this.clientViewController.lastView;
    let newView: View = this.clientViewController.currentView;

    console.log('oldView ', oldView);
    console.log('newView ', newView);

    let connectionsToAdd: Map<number, InternetAddress> = new Map(newView.addresses);
    let connectionsToRemove: Map<number, InternetAddress> = new Map();


    for (let i of newView.addresses.keys()) {
      // Connection is both in the old view and in the new view and it's the same connection
      if (oldView.addresses.get(i) && oldView.addresses.get(i) === newView.addresses.get(i)) {
        // Remove this connection from map of connections that must be established and added to session table
        connectionsToAdd.delete(i);
      }
    }

    for (let i of oldView.addresses.keys()) {
      // Connection is in the old but not in the new view
      if (!newView.addresses.get(i)) {
        console.log(oldView.addresses.get(i), '!==', newView.addresses.get(i));
        // Add this connection from map of connections that must be removed from session table
        connectionsToRemove.set(i, newView.addresses.get(i));
      }
    }

    // Remove old connections
    connectionsToRemove.forEach((value: InternetAddress, key: number) => {
      this.sessionTable.delete(key);
      console.log('#### RECONFIG: Removed replica ', key);
    });


    // Establish and add new connections
    connectionsToAdd.forEach((value: InternetAddress, key: number) => {
      let address: string = 'ws://' + value.address + ':' + value.port;
      let socket: Subject<any> = this.websocketService.createWebsocket(address);
      let connection: ReplicaConnection = new ReplicaConnection(socket, null, null, key);
      this.sessionTable.set(key, connection);
      console.log('#### RECONFIG: Added replica ', key);
    });
  }


}
