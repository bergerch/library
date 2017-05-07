/**
 * Created by chris on 23.04.17.
 */

import {Injectable} from '@angular/core';
import {TOMMessage, TOMMessageType} from "./ServiceProxy.service";
import {CommunicationSystemClientSide} from "../communication/CommunicationSystemClientSide.service";
import {ViewController} from "../reconfiguration/ViewController.controller";


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
export abstract class TOMSender implements ReplyReceiver, Closeable {

  me: number; // process id
  clientViewController: Object;
  session: number = 0; // session id
  sequence: number = 0; // sequence number
  unorderedMessageSequence: number = 0; // sequence number for readonly messages
  cs: CommunicationSystemClientSide; // client side comunication system
  useSignatures: boolean = false; // use MACs or signatures
  opCounter: number = 0; // Atomic counter
  viewController: ViewController;

  public TOMSender() {

  }

  /**
   * This is the method invoked by the client side communication system, and where the
   * code to handle the reply is to be written. This method is overwritten in ServiceProxy
   *
   * @param reply The reply delivered by the client side communication system
   */
  replyReceived(reply: TOMMessage) {}

  close() {

  };

  init(processId: number, configHome: string) {

  }

  startsCS(clientId: number) {

  }

  generateRequestId(type: TOMMessageType): number {
    // TODO: if multi-threaded implement lock here
    let id;
    if (type == TOMMessageType.ORDERED_REQUEST) {
      id = this.sequence++;
    } else {
      id = this.unorderedMessageSequence++;
    }
    // TODO unlock
    return id;

  }

  generateOperationId(): number {
    // TODO: if multi-threaded implement lock here
    this.opCounter++;
    // TODO unlock
    return this.opCounter;
  }


  /**
   * Multicast a TOMMessage to the group of replicas
   *
   * @param sm Message to be multicast
   */
  TOMulticast(sm: TOMMessage) {
    this.cs.send(this.useSignatures, this.viewController.getCurrentView().processes, sm);
  }

  /**
   * Multicast data to the group of replicas
   *
   * @param m Data to be multicast
   * @param reqId unique integer that identifies this request
   * @param reqType TOM_NORMAL, TOM_READONLY or TOM_RECONFIGURATION
   */
  TOMulticastData(m: any, reqId: number, reqType: TOMMessageType, operationsId?: number) {

    let operatId = operationsId ? operationsId : -1;
    this.cs.send(this.useSignatures, this.viewController.getCurrentView().processes,
      {viewID: this.me, type: reqType, session: this.session, sequence: reqId, operationId: operatId, content: m});
  }

  sendMessageToTargets(m: any, reqId: number, targets: number[], type: TOMMessageType, operationsId?: number) {

  }

}
