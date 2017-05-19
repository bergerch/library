/**
 * Created by chris on 23.04.17.
 */
import {Injectable} from '@angular/core';
import {TOMSender} from "./TOMSender.service";
import {HashResponseController} from "./HashResponseController.controller";
import {TOMConfiguration} from "../config/TOMConfiguration";
import {Comparator} from "./util/Comparator.interface";
import {Extractor} from "bft/tom/util/Extractor.interface";
import {ClientViewController} from "../reconfiguration/ClientViewController.controller";

export enum TOMMessageType {

  ORDERED_REQUEST = 0,
  UNORDERED_REQUEST = 1,
  REPLY = 2,
  RECONFIG = 3,
  ASK_STATUS = 4,
  STATUS_REPLY = 5,
  UNORDERED_HASHED_REQUEST = 6,

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

  public constructor(sender: number, session: number, reqId: number, operationId: number, request: any, viewId: number,
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
  replyQuorum: number = 2; // size of the reply quorum (!)TODO Load from Config
  receivedReplies: number = 0; // Number of received replies
  invokeTimeout: number = 40;
  replyServer: number;
  invokeUnorderedHashedTimeout: number = 10;

  requestType: TOMMessageType;
  replies: TOMMessage[] = []; // Replies from replicas are stored here
  response: TOMMessage = null; // Reply delivered to the application

  hashResponseController: HashResponseController;

  comparator: Comparator<any>;
  extractor: Extractor;


  public constructor(TOMConfiguration: TOMConfiguration) {
    super(TOMConfiguration);

    // FIXME Why is this still undefined?
    // this.replies = new TOMMessage[super.getViewManager().getCurrentView().getN()];

    this.comparator = (this.comparator != null) ? this.comparator : {
      compare: function (o1: any, o2: any): number {
        return JSON.stringify(o1) === JSON.stringify(o2) ? 0 : -1;
      }
    };

    this.extractor = (this.extractor != null) ? this.extractor : {
      extractResponse: function (replies: TOMMessage[], sameContent: number, lastReceived: number) {
        return replies[lastReceived];
      }
    };

  }

  public setExtractor(extractor: Extractor) {
    this.extractor = extractor;
  }

  public setComparator(comparator: Comparator<any>) {
    this.comparator = comparator;
  }

  /**
   * This is the method invoked by the client side communication system, and where the
   * code to handle the reply is to be written.
   *
   * @Overwrite TOMSender's replyReceived
   *
   * @param reply The reply delivered by the client side communication system
   */
  public replyReceived(reply: TOMMessage) {
    // TODO
  }

  public invokeOrdered(request): any {
    return this.invoke(request, TOMMessageType.ORDERED_REQUEST);
  }

  public invokeUnordered(request): any {
    return this.invoke(request, TOMMessageType.UNORDERED_REQUEST);
  }

  public invokeUnorderedHashed(request): any {
    return this.invoke(request, TOMMessageType.UNORDERED_HASHED_REQUEST);
  }

  private invoke(request, reqType: number): any {
    console.log('Service Proxy invoke() called with ', request);
    console.log('Request Type is ', reqType);
    console.log(this);

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
    this.hashResponseController = null;

    if (reqType === TOMMessageType.UNORDERED_HASHED_REQUEST) {

      this.replyServer = this.getRandomlyServerId();


      let viewController: ClientViewController = this.getViewManager();
      console.log('view controller: ', viewController);


      let hashResponseController = new HashResponseController(viewController.getCurrentViewPos(this.replyServer),
        viewController.getCurrentViewProcesses().length);
      console.log('hashResponseController: ', hashResponseController);


      /*let sm: TOMMessage = new TOMMessage(getProcessId(), getSession(), reqId, operationId, request,
       getViewManager().getCurrentViewId(), requestType);
       sm.setReplyServer(replyServer);

       TOMulticast(sm);
       } else {
       TOMulticast(request, reqId, operationId, reqType);
       }*/

    }

  }

  private getRandomlyServerId(): number {
    let numServers: number = this.getViewController().getCurrentViewProcesses().length;
    // TODO Unsure if this will actually work properly?
    let pos = Math.round(Math.random() * numServers);
    let id = this.getViewController().getCurrentViewProcesses()[pos];
    console.log('getRandomlyServerId ', id);
    return id;
  }


  private reconfigureTo(view) {
    // TODO
  }

}
