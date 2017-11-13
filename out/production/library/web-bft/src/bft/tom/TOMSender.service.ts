/**
 * Created by chris on 23.04.17.
 */

import {Injectable} from '@angular/core';
import {CommunicationSystem} from "../communication/CommunicationSystem.service";
import {ClientViewController} from "../reconfiguration/ClientViewController.controller";
import {TOMConfiguration} from "../config/TOMConfiguration";
import {TOMMessage} from "./messages/TOMMessage";
import {TOMMessageType} from "./messages/TOMMessageType";
import {Http} from "@angular/http";
import {ICommunicationSystem} from "../communication/ICommunicationSystem.interface";


export interface ReplyReceiver {

  /**
   * This is the method invoked by the client side comunication system, and where the
   * code to handle the reply is to be written
   *
   * @param reply The reply delivered by the client side comunication system
   */
  replyReceived(reply: TOMMessage);

}


export interface Closeable {

  /**
   * Closes this stream and releases any system resources associated
   * with it. If the stream is already closed then invoking this
   * method has no effect.
   */
  close();

}

@Injectable()
export abstract class TOMSender implements Closeable {

  me: number; // process id
  session: number = 0; // session id
  sequence: number = 0; // sequence number
  unorderedMessageSequence: number = 0; // sequence number for readonly messages
  cs: ICommunicationSystem; // client side communication system
  useSignatures: boolean = false; // use MACs or signatures
  opCounter: number = 0; // Atomic counter
  protected viewController: ClientViewController;


  public constructor(protected TOMConfiguration: TOMConfiguration, private http: Http) {

    this.viewController = new ClientViewController(this.me, this.TOMConfiguration);
    this.me = Math.round(Math.random() * 100000);
    this.cs = new CommunicationSystem(this.me, this.viewController, TOMConfiguration, http);

  }

  /**
   * Wrapper method
   * @returns {ClientViewController}
   */
  public getViewManager() {
    return this.getViewController();
  }

  public getViewController() {
    return this.viewController;
  }


  public close() {
    this.cs.close();
  };

  public init(processId: number, configHome: string) {
    this.viewController = new ClientViewController(processId, this.TOMConfiguration);
    this.startsCS(processId);
  }

  public startsCS(clientId: number) {

  }

  generateRequestId(type: TOMMessageType): number {
    let id;
    if (type === TOMMessageType.ORDERED_REQUEST) {
      id = this.sequence++;
    } else {
      id = this.unorderedMessageSequence++;
    }
    return id;

  }

  generateOperationId(): number {
    this.opCounter++;
    return this.opCounter;
  }

  getId() {
    return this.me;
  }

  /**
   * Multicast a TOMMessage.ts to the group of replicas
   *
   * @param sm Message to be multicast
   */
  public TOMulticast(sm: TOMMessage, replyReceiver?: ReplyReceiver) {
    this.cs.send(this.useSignatures, sm, replyReceiver);
  }

  /**
   * Multicast data to the group of replicas
   *
   * @param m Data to be multicast
   * @param reqId unique integer that identifies this request
   * @param reqType TOM_NORMAL, TOM_READONLY or TOM_RECONFIGURATION
   */
  public TOMulticastData(m: any, reqId: number, reqType: TOMMessageType, operationsId?: number, replyReceiver?: ReplyReceiver, event?: string) {
    let operatId = operationsId ? operationsId : -1;
    this.cs.send(this.useSignatures,
      new TOMMessage(this.me, this.session, reqId, operatId, m, this.getViewController().getCurrentView().id, reqType, event),
      replyReceiver);
  }


}
