
import {Injectable} from '@angular/core';
import {ReplyReceiver} from "bft/tom/TOMSender.service";
import {TOMMessage} from "../tom/messages/TOMMessage";
import {ClientViewController} from "../reconfiguration/ClientViewController.controller";
import {ReplicaWSConnection} from "./ReplicaWSConnection";
import {WebsocketService} from "./Websocket.service";
import {Subject} from "rxjs/Subject";
import {View} from "../reconfiguration/View";
import {InternetAddress, TOMConfiguration} from "../config/TOMConfiguration";
import * as CryptoJS from "../../../node_modules/crypto-js/crypto-js.js";
import {ReplicaConnection} from "./ReplicaConnection";
import {SimpleHttpService} from "./SimpleHttp.service";
import {Http} from "@angular/http";
import {ReplicaHTTPConnection} from "./ReplicaHTTPConnection";

export interface ICommunicationSystem {

  send(sign: boolean, targets: number[], sm: TOMMessage, replyReceiver?: ReplyReceiver);
  setReplyReceiver(trr: ReplyReceiver);
  sign(sm: TOMMessage);
  close();
  updateConnections();

}

@Injectable()
export class CommunicationSystem implements ICommunicationSystem {

  replyReceiver: ReplyReceiver;
  clientViewController: ClientViewController;
  sessionTable: Map<number, ReplicaConnection> = new Map<number, ReplicaConnection>();

  // the signature engine used in the system
  // signatureEngine: Object;
  // signatureLength: number;
  closed: boolean = false;

  protected websocketService: WebsocketService;
  protected http: Http;


  public constructor(private clientId: number, viewController: ClientViewController, private TOMConfiguration: TOMConfiguration, http: Http) {
    this.websocketService = new WebsocketService();
    this.clientViewController = viewController;
    this.http = http;

    this.log('Current View is: ');
    viewController.getCurrentView().addresses.forEach((value: InternetAddress, replicaId: number) => {
      this.log('|->', value);

      let address: string = 'ws://' + value.address + ':' + value.port;
      let password = '' + clientId + ':' + replicaId;

      let connection: ReplicaConnection = this.connectTo(address, replicaId, password);
      this.sessionTable.set(replicaId, connection);
    });

  }

  private connectTo(address: string, replicaId: number, password?: string): ReplicaConnection {
    let connection: ReplicaConnection;
    if (this.TOMConfiguration.websockets) {
      let socket: Subject<any> = this.websocketService.createWebsocket(address);
      connection = new ReplicaWSConnection(socket, replicaId, password);
    } else {
      let simpleHTTPService: SimpleHttpService = new SimpleHttpService(this.http, address);
      connection = new ReplicaHTTPConnection(simpleHTTPService, replicaId, password);
    }
    return connection;
  }

  public send(sign: boolean, targets: number[], sm: TOMMessage, replyReceiver?: ReplyReceiver) {

    // Subscribe: When reply is received, parse JSON and execute replyListener
    this.sessionTable.forEach((connection: ReplicaConnection, replicaId: number) => {
      connection.subscribe( (reply) => this.receive(reply, replyReceiver, connection));
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
      connection.send(message);
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
    this.sessionTable.forEach((connection: ReplicaConnection) => {
      connection.close();
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
      this.sessionTable.get(key).close();
      this.sessionTable.delete(key);
      this.log('#### RECONFIG: Removed replica ', key);
    });


    // Establish and add new connections
    connectionsToAdd.forEach((value: InternetAddress, replicaId: number) => {
      let address: string = 'ws://' + value.address + ':' + value.port;
      let password =  '' + this.clientId + ':' + replicaId;
      let connection: ReplicaConnection = this.connectTo(address, replicaId, password);
      this.sessionTable.set(replicaId, connection);
      this.log('#### RECONFIG: Added replica ', replicaId);
    });
  }

  private log(var1, var2?, var3?) {
    if (this.TOMConfiguration.debug) {
      if(var3)
      console.log(var1, var2, var3);
      else if (var2)
        console.log(var1, var2);
      else
        console.log(var1);
    }
  }

}
