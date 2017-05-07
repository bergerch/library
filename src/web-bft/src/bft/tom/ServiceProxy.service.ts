/**
 * Created by chris on 23.04.17.
 */
import {Injectable} from '@angular/core';
import {TOMSender} from "./TOMSender.service";

export enum TOMMessageType {
  ORDERED_REQUEST = 0,
  UNORDERED_REQUEST = 1,
  REPLY = 2,
  RECONFIG = 3,
  ASK_STATUS = 4,
  STATUS_REPLY = 5,
  UNORDERED_HASHED_REQUEST = 6
}

export interface TOMMessage {
  viewID: number;
  type: TOMMessageType; // request type: application or reconfiguration request
  session: number; // Sequence number defined by the client
  // There is a sequence number for ordered and another for unordered messages
  sequence: number;
  operationId: number; // Sequence number defined by the client
  content: any; // Content of the message
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

  // Comparator<byte[]> comparator;
  // Extractor extractor;
  // Random rand = new Random(System.currentTimeMillis());
  // HashResponseController hashResponseController;


  public ServiceProxy() {

  }

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

  invokeOrdered(request) {
    return this.invoke(request, 'TOMMessageType.ORDERED_REQUEST');
  }

  invokeUnordered(request) {
    return this.invoke(request, 'TOMMessageType.UNORDERED_REQUEST');
  }

  invokeUnorderedHashed(request) {
    return this.invoke(request, 'TOMMessageType.UNORDERED_HASHED_REQUEST');
  }

  invoke(request, reqType) {
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

    // TODO

  }

  private reconfigureTo(view) {
    // TODO
  }

}
