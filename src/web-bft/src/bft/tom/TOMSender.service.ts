/**
 * Created by chris on 23.04.17.
 */

import {Injectable} from '@angular/core';
import {TOMMessage, TOMMessageType} from "./ServiceProxy.service";
import {CommunicationSystemClientSide} from "../communication/CommunicationSystemClientSide.service";


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
  communicationSystemClientSide: CommunicationSystemClientSide; // client side comunication system
  useSignatures: boolean = false; // use MACs or signatures
  opCounter: number = 0; // Atomic counter

  public TOMSender() {

  }

  replyReceived(reply: TOMMessage) {

  }

  close() {

  };

  init(processId: number, configHome: string) {

  }

  startsCS(clientId: number) {

  }

  generateRequestId(type: TOMMessageType): number {
    return 0;
  }

  generateOperationId(): number {
    return 0;
  }


  /**
   * Multicast a TOMMessage to the group of replicas
   *
   * @param sm Message to be multicast
   */
  TOMulticastTOM(sm: TOMMessage) {
    //cs.send(useSignatures, this.viewController.getCurrentViewProcesses(), sm);
  }

  /**
   * Multicast data to the group of replicas
   *
   * @param m Data to be multicast
   * @param reqId unique integer that identifies this request
   * @param reqType TOM_NORMAL, TOM_READONLY or TOM_RECONFIGURATION
   */
  TOMulticastData(m: any, reqId: number, reqType: TOMMessageType) {

  }

  sendMessageToTargets(m: any, reqId: number, targets: number[], type: TOMMessageType, operationsId?: number) {

  }

}
