import {Injectable} from '@angular/core';
import {ReplyReceiver} from "bft/tom/TOMSender.service";
import {TOMMessage} from "../tom/messages/TOMMessage";
import {ClientViewController} from "../reconfiguration/ClientViewController.controller";
import {ReplicaWSConnection} from "./ReplicaWSConnection";
import {WebsocketService} from "./Websocket.service";
import {Subject} from "rxjs/Subject";
import {View} from "../reconfiguration/View";
import {InternetAddress, TOMConfiguration} from "../config/TOMConfiguration";
import {ReplicaConnection} from "./ReplicaConnection";
import {SimpleHttpService} from "./SimpleHttp.service";
import {Http} from "@angular/http";
import {ReplicaHTTPConnection} from "./ReplicaHTTPConnection";
import {ICommunicationSystem} from "./ICommunicationSystem.interface";

/**
 * The Communication system represents the interface that connects with
 * BFT-SMaRTs ClientServerCommunicationSystemServerSide.
 */
@Injectable()
export class CommunicationSystem implements ICommunicationSystem {

  // handles view management / reconfiguration
  clientViewController: ClientViewController;

  // map handles all replica connections
  sessionTable: Map<number, ReplicaConnection> = new Map<number, ReplicaConnection>();

  // the signature engine used in the system
  // signatureEngine: Object;
  // signatureLength: number;
  closed: boolean = false;

  protected websocketService: WebsocketService;
  protected http: Http;

  private protocol: string;

  /**
   * Constructs a new CommunicationSystem, opens a connection to each replica
   *
   * @param clientId id of the Client
   * @param viewController controller for the view
   * @param TOMConfiguration TOM config object
   * @param http http service
   */
  public constructor(private clientId: number, viewController: ClientViewController, private TOMConfiguration: TOMConfiguration, http: Http) {
    this.websocketService = new WebsocketService();
    this.clientViewController = viewController;
    this.http = http;
    this.protocol = TOMConfiguration.websockets ? 'ws://' : 'http://';

    this.log('Current View is: ');
    viewController.getCurrentView().addresses.forEach((internetAddress: InternetAddress, replicaId: number) => {
      this.log('|->', internetAddress);

      let password = '' + clientId + ':' + replicaId;

      // Open connection to replica
      let connection: ReplicaConnection = this.connectTo(replicaId, password);
      this.sessionTable.set(replicaId, connection);
    });

  }

  /**
   * Establishes a connection to a specific replica
   *
   * @param replicaId id of replica
   * @param password shared secret for hmacs
   * @returns {ReplicaConnection} The established connection to the specific replica
   */
  private connectTo(replicaId: number, password?: string): ReplicaConnection {
    let internetAddress: InternetAddress = this.clientViewController.getCurrentView().addresses.get(replicaId);
    let address: string = this.protocol + internetAddress.address + ':' + internetAddress.port;
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


  /**
   * Sends a TOMMessage sm to all replicas (broadcast). The replyReceiver is a callback function that will be executed
   * when the replica's response is obtained. Also attaches hmac to message.
   *
   * @param sign not used / implemented currently
   * @param sm TOMMessage
   * @param replyReceiver
   */
  public send(sign: boolean, sm: TOMMessage, replyReceiver?: ReplyReceiver) {

    // Subscribe: When reply is received, parse JSON and execute replyListener
    this.sessionTable.forEach((connection: ReplicaConnection) => {
      connection.subscribe((reply) => this.receive(reply, replyReceiver, connection));
    });

    // Send Message to all replicas
    this.sessionTable.forEach((connection: ReplicaConnection) => {

      // Creates MAC
      let hmac = this.TOMConfiguration.useMACs ? connection.computeMAC(JSON.stringify(sm)) : '';
      let message = {data: sm, hmac: hmac};

      this.log('send messsage + hmac', message);

      connection.send(message);
    });

    this.log('send ', sm);
  }

  /**
   * Receives a reply from the replica and checks if hmac / signature is correct. If so, it will be delivered to
   * ServiceProxy
   *
   * @param reply reply from replica
   * @param replyReceiver callback function from TOMSender
   * @param connection connection object between client and replica
   */
  private receive(reply, replyReceiver: ReplyReceiver, connection: ReplicaConnection) {

    let msgReceived = reply;
    if (this.TOMConfiguration.websockets) {
      msgReceived = JSON.parse(reply.data);
    }

    // Compute and compare hmacs
    if (this.TOMConfiguration.useMACs) {

      let hmacReceived = msgReceived.hmac;
      this.log('HMAC received', hmacReceived);
      let data: string = JSON.stringify(msgReceived.data);

      if (!connection.verifyMAC(data, hmacReceived)) {
        console.error('HMACs are NOT Matching');
        console.error('HMAC received: ', hmacReceived);
        console.error('hmac computed: ', connection.computeMAC(data));
        console.error('data received: ', data);
        console.error('secret ', this.clientId + ':' + connection.getReplicaId());
        return;
      }
    }

    replyReceiver.replyReceived(msgReceived.data);
  }


  /**
   * Signs the TOMMessage. Currently not implemented
   * @param sm
   */
  public sign(sm: TOMMessage) {
    throw new Error('Not yet implemented');
  }

  /**
   * Closes all connections
   */
  public close() {
    this.sessionTable.forEach((connection: ReplicaConnection) => {
      connection.close();
    });
  }

  /**
   * Updates all connections according to the new View
   */
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
    connectionsToRemove.forEach((internetAddress: InternetAddress, key: number) => {
      this.sessionTable.get(key).close();
      this.sessionTable.delete(key);
      this.log('#### RECONFIG: Removed replica ', key);
    });

    // Establish and add new connections
    connectionsToAdd.forEach((internetAddress: InternetAddress, replicaId: number) => {
      let password = '' + this.clientId + ':' + replicaId;
      let connection: ReplicaConnection = this.connectTo(replicaId, password);
      this.sessionTable.set(replicaId, connection);
      this.log('#### RECONFIG: Added replica ', replicaId);
    });
  }


  private log(var1, var2?, var3?) {
    if (this.TOMConfiguration.debug) {
      if (var3)
        console.log(var1, var2, var3);
      else if (var2)
        console.log(var1, var2);
      else
        console.log(var1);
    }
  }

}
