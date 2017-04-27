/**
 * Created by chris on 23.04.17.
 */

import {Injectable} from '@angular/core';
import {TOMMessage} from "../tom/ServiceProxy.service";
import {ReplyReceiver} from "bft/tom/TOMSender.service";

export interface ICommunicationSystemClientSide {

  send(sign: boolean, targets: number[], sm: TOMMessage);
  setReplyReceiver(trr: ReplyReceiver);
  sign(sm: TOMMessage);
  close();
  updateConnections();

}

@Injectable()
export class CommunicationSystemClientSide implements ICommunicationSystemClientSide{

  clientId: number;
  trr: ReplyReceiver;
  clientViewController: Object;
  sessionTable = new Map<string, string>();
  reentrantReadWriteLock: Object;

  //the signature engine used in the system
  signatureEngine: Object;
  ignatureLength: number;
  closed: boolean = false;

  public CommunicationSystemClientSide() {

  }

  send(sign: boolean, targets: number[], sm: TOMMessage) {

  }

  setReplyReceiver(trr: ReplyReceiver) {

  }

  sign(sm: TOMMessage) {

  }

  close() {

  }

  updateConnections() {

  }

  reconnect() {

  }



}
