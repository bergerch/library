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
import {InternetAddress, TOMConfiguration} from "../config/TOMConfiguration";
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


  public constructor(clientId: number, viewController: ClientViewController, private TOMConfiguration: TOMConfiguration) {
    this.websocketService = new WebsocketService();
    this.clientViewController = viewController;

    this.log('Current View is: ');
    viewController.getCurrentView().addresses.forEach((value: InternetAddress, key: number) => {
      this.log('|->', value);

      let address: string = 'ws://' + value.address + ':' + value.port;
      let socket: Subject<any> = this.websocketService.createWebsocket(address);
      let password = '' + clientId + ':' + key;
      let connection: ReplicaConnection = new ReplicaConnection(socket, null, null, key, password);
      this.sessionTable.set(key, connection);
    });


  }

  public send(sign: boolean, targets: number[], sm: TOMMessage, replyReceiver?: ReplyReceiver) {

    // Subscribe: When reply is received, parse JSON and execute replyListener
    this.sessionTable.forEach((connection: ReplicaConnection, replicaId: number) => {
      connection.getSocket().subscribe( (reply) => this.receive(reply, replyReceiver, connection));
    });

    // Send Message to all replicas
    this.sessionTable.forEach((connection: ReplicaConnection, replicaId: number) => {

      // Creates MAC
      let hmac = '';
      if (this.TOMConfiguration.useMACs) {

        let secret: string = connection.getSecret();
        let message: string = JSON.stringify(sm);

        this.log('MESSAGE ', message);

        hmac = CryptoJS.enc.Hex.stringify(CryptoJS.HmacSHA1(message, secret));
      }

      let message = {data: sm, hmac: hmac};
      this.log('send messsage + hmac', message);
      connection.getSocket().next(message);
    });

    this.log('send ', sm);
  }

  private receive(reply, replyReceiver: ReplyReceiver, connection: ReplicaConnection) {

      let msgReceived = JSON.parse(reply.data);

      // Compute and compare hmacs
      if (this.TOMConfiguration.useMACs) {

        let hmacReceived = msgReceived.hmac;
        this.log('RECEIVED HMAC ', hmacReceived);
        let secret: string = connection.getSecret();
        let data: string = JSON.stringify(msgReceived.data);

        this.log('DATA ', data);

        let hmacComputed = CryptoJS.enc.Hex.stringify(CryptoJS.HmacSHA1(data, secret));

        this.log('COMPUTED HMAC ', hmacComputed);

        if (hmacComputed == hmacReceived) {
          this.log(hmacComputed + ' === ' + hmacReceived);
        } else {
          this.log(hmacComputed + ' =/= ' + hmacReceived);
          // Do NOT deliver message to ServiceProxy when MAC is invalid
          return;
        }
      }

      replyReceiver.replyReceived(msgReceived.data);

  }


  public setReplyReceiver(replyReceiver: ReplyReceiver) {
    this.replyReceiver = replyReceiver;
  }

  public sign(sm: TOMMessage) {

    throw new Error('Not yet implemented');

  }

  public close() {

    this.sessionTable.forEach((connection: ReplicaConnection, replicaId: number) => {
      connection.getSocket().complete();
    });

  }

  public updateConnections() {

    if (!this.websocketService) {
      this.websocketService = new WebsocketService();
    }

    let oldView: View = this.clientViewController.lastView;
    let newView: View = this.clientViewController.currentView;

    this.log('oldView ', oldView);
    this.log('newView ', newView);

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
        this.log(oldView.addresses.get(i), '!==', newView.addresses.get(i));
        // Add this connection from map of connections that must be removed from session table
        connectionsToRemove.set(i, newView.addresses.get(i));
      }
    }

    // Remove old connections
    connectionsToRemove.forEach((value: InternetAddress, key: number) => {
      this.sessionTable.delete(key);
      this.log('#### RECONFIG: Removed replica ', key);
    });


    // Establish and add new connections
    connectionsToAdd.forEach((value: InternetAddress, key: number) => {
      let address: string = 'ws://' + value.address + ':' + value.port;
      let socket: Subject<any> = this.websocketService.createWebsocket(address);
      let connection: ReplicaConnection = new ReplicaConnection(socket, null, null, key);
      this.sessionTable.set(key, connection);
      this.log('#### RECONFIG: Added replica ', key);
    });
  }

  private log(var1, var2?, var3?) {
    if (this.TOMConfiguration.debug)
      console.log(var1, var2, var3);
  }

}
