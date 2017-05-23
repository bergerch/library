/**
 * Created by chris on 23.04.17.
 */

import {Injectable} from '@angular/core';
import {ReplyReceiver} from "bft/tom/TOMSender.service";
import {TOMMessage} from "../tom/messages/TOMMessage";
import {ClientViewController} from "../reconfiguration/ClientViewController.controller";
import {ReplicaConnection} from "./ReplicaConnection";

export interface ICommunicationSystemClientSide {

  send(sign: boolean, targets: number[], sm: TOMMessage);
  setReplyReceiver(trr: ReplyReceiver);
  sign(sm: TOMMessage);
  close();
  updateConnections();

}

@Injectable()
export class CommunicationSystemClientSide implements ICommunicationSystemClientSide {

  clientId: number;
  trr: ReplyReceiver;
  clientViewController: Object;
  sessionTable: Map<string, ReplicaConnection> = new Map<string, ReplicaConnection>();
  reentrantReadWriteLock: Object;

  //the signature engine used in the system
  signatureEngine: Object;
  ignatureLength: number;
  closed: boolean = false;

  public constructor(clientId: number, viewController: ClientViewController) {
    // TODO
  }

  public send(sign: boolean, targets: number[], sm: TOMMessage) {
    console.log('CommunicationSystem send() called ');
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
