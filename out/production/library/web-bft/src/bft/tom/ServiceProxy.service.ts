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
import {TOMMessageType} from "./messages/TOMMessageType";
import {TOMMessage} from "./messages/TOMMessage";
import {WebsocketService} from "../communication/Websocket.service";
import {ReplyListener} from "../communication/ReplyListener.interface";


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
    // this.replies = new TOMMessage.ts[super.getViewManager().getCurrentView().getN()];

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
    console.log('REPLY ', reply);
  }

  public invokeOrdered(request, replyListener?: ReplyListener): any {
    return this.invoke(request, TOMMessageType.ORDERED_REQUEST, replyListener);
  }

  public invokeUnordered(request, replyListener?: ReplyListener): any {
    return this.invoke(request, TOMMessageType.UNORDERED_REQUEST, replyListener);
  }

  public invokeUnorderedHashed(request, replyListener?: ReplyListener): any {
    return this.invoke(request, TOMMessageType.UNORDERED_HASHED_REQUEST, replyListener);
  }

  private invoke(request, reqType: number, replyListener?: ReplyListener): any {
    //console.log('Service Proxy invoke() called with ', request);
    //console.log('Request Type is ', reqType);
    //console.log(this);

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
      //console.log('view controller: ', viewController);


      let hashResponseController = new HashResponseController(viewController.getCurrentViewPos(this.replyServer),
        viewController.getCurrentViewProcesses().length);
      //console.log('hashResponseController: ', hashResponseController);

      let sm: TOMMessage = new TOMMessage(this.me, this.session, this.reqId, this.operationId, request,
        this.getViewManager().getCurrentViewId(), reqType);

      //console.log('TOMMessage.ts: ', sm);

      sm.setReplyServer(this.replyServer);

      this.TOMulticast(sm, replyListener);
    } else {

      console.log(reqType);
       this.TOMulticastData(request, this.reqId, reqType, this.operationId, replyListener);

    }


  }

  private getRandomlyServerId(): number {
    let numServers: number = this.getViewController().getCurrentViewProcesses().length;
    let pos = Math.floor(Math.random() * numServers);
    let id = this.getViewController().getCurrentViewProcesses()[pos];
    //console.log('getRandomlyServerId ', id);
    return id;
  }


  private reconfigureTo(view) {
    // TODO
  }

}
