/**
 * Created by chris on 23.04.17.
 */
import {Injectable} from '@angular/core';
import {TOMSender} from "./TOMSender.service";
import {HashResponseController} from "./HashResponseController.controller";
import {TOMConfiguration} from "../config/TOMConfiguration";

export enum TOMMessageType {
  ORDERED_REQUEST = 0,
  UNORDERED_REQUEST = 1,
  REPLY = 2,
  RECONFIG = 3,
  ASK_STATUS = 4,
  STATUS_REPLY = 5,
  UNORDERED_HASHED_REQUEST = 6
}

@Injectable()
export abstract class SystemMessage {

  sender: number; // ID of the process which sent the message
  authenticated: boolean;
  // set to TRUE if the message was received
  // with a (valid) mac, FALSE if no mac was given
  // note that if the message arrives with an
  // invalid MAC, it won't be delivered

  constructor(sender: number) {
    this.sender = sender;
  }

  public setSender(sender: number) {
    this.sender = sender;
  }
}

@Injectable()
export class TOMMessage extends SystemMessage {
  viewId: number;
  type: TOMMessageType; // request type: application or reconfiguration request
  session: number; // Sequence number defined by the client
  // There is a sequence number for ordered and another for unordered messages
  sequence: number;
  operationId: number; // Sequence number defined by the client
  content: any; // Content of the message

  constructor(sender: number, session: number, reqId: number, operationId: number, request: any, viewId: number,
              requestType: TOMMessageType) {
    super(sender);
    this.session = session;
    this.sequence = reqId;
    this.operationId = operationId;
    this.content = request;
    this.viewId = viewId;
    this.type = requestType;
  }
}

@Injectable()
export class ServiceProxy extends TOMSender {

  reqId: number = -1;
  operationId: number = -1;
  replyQuorum: number = 4; // size of the reply quorum
  receivedReplies: number = 0; // Number of received replies
  invokeTimeout: number = 40;
  replyServer: number;
  invokeUnorderedHashedTimeout: number = 10;

  requestType: TOMMessageType;
  replies: TOMMessage[] = []; // Replies from replicas are stored here
  response: TOMMessage = null; // Reply delivered to the application
  hashResponseController: HashResponseController;

  // Comparator<byte[]> comparator;
  // Extractor extractor;
  // Random rand = new Random(System.currentTimeMillis());


  /**
   * This is the method invoked by the client side communication system, and where the
   * code to handle the reply is to be written.
   *
   * @Overwrite TOMSender's replyReceived
   *
   * @param reply The reply delivered by the client side communication system
   */
  replyReceived(reply: TOMMessage) {
    // TODO
  }

  invokeOrdered(request): any {
    return this.invoke(request, 'TOMMessageType.ORDERED_REQUEST');
  }

  invokeUnordered(request): any {
    return this.invoke(request, 'TOMMessageType.UNORDERED_REQUEST');
  }

  invokeUnorderedHashed(request): any {
    return this.invoke(request, 'TOMMessageType.UNORDERED_HASHED_REQUEST');
  }

  invoke(request, reqType): any {
    console.log('Service Proxy invoke() called with ', request, reqType);

    // Clean all statefull data to prepare for receiving next replies
    this.replies = [];
    this.receivedReplies = 0;
    this.response = null;
    // this.replyQuorum = this.getReplyQuorum();

    // Send the request to the replicas, and get its ID
    this.reqId = this.generateRequestId(reqType);
    this.operationId = this.generateOperationId();
    this.requestType = reqType;
    this.replyServer = -1;

    if (reqType == TOMMessageType.UNORDERED_HASHED_REQUEST) {

      this.replyServer = this.getRandomlyServerId();


      //let hashResponseController = new HashResponseController(getViewManager().getCurrentViewPos(replyServer),
      //   getViewManager().getCurrentViewProcesses().length);

      /*let sm: TOMMessage = new TOMMessage(getProcessId(), getSession(), reqId, operationId, request,
       getViewManager().getCurrentViewId(), requestType);
       sm.setReplyServer(replyServer);

       TOMulticast(sm);
       } else {
       TOMulticast(request, reqId, operationId, reqType);
       }*/

    }

  }

  getRandomlyServerId(): number {
    let numServers: number = super.getViewController().getCurrentViewProcesses().length;
    // TODO Unsure if this will actually work properly?
    let pos = Math.round(Math.random() * numServers);

    return super.getViewController().getCurrentViewProcesses()[pos];
  }


  private reconfigureTo(view) {
    // TODO
  }

}
