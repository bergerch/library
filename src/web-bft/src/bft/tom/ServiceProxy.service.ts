/**
 * Created by chris on 23.04.17.
 */
import {Injectable} from '@angular/core';

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
export class ServiceProxy {

  reqId: number = -1;
  operationId: number = -1;
  replyQuorum: number = 0; // size of the reply quorum
  receivedReplies: number = 0; // Number of received replies
  invokeTimeout: number = 40;
  replyServer: number;
  invokeUnorderedHashedTimeout: number = 10;

  requestType: TOMMessageType;
  replies: TOMMessage[] = []; // Replies from replicas are stored here
  response: TOMMessage  = null; // Reply delivered to the application

  // Comparator<byte[]> comparator;
  // Extractor extractor;
  // Random rand = new Random(System.currentTimeMillis());
  // HashResponseController hashResponseController;



  public ServiceProxy() {

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
    // TODO
  }

  private reconfigureTo(view) {
    // TODO
  }

}
