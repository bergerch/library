/**
 * Created by chris on 23.04.17.
 */
import {Injectable} from '@angular/core';
import {ReplyReceiver, TOMSender} from "./TOMSender.service";
import {HashResponseController} from "./HashResponseController.controller";
import {TOMConfiguration} from "../config/TOMConfiguration";
import {Comparator} from "./util/Comparator.interface";
import {Extractor} from "bft/tom/util/Extractor.interface";
import {ClientViewController} from "../reconfiguration/ClientViewController.controller";
import {TOMMessageType} from "./messages/TOMMessageType";
import {TOMMessage} from "./messages/TOMMessage";
import {ReplyListener} from "../communication/ReplyListener.interface";
import {View} from "../reconfiguration/View";


@Injectable()
export class ServiceProxy extends TOMSender implements ReplyReceiver {

  reqId: number = -1;
  operationId: number = -1;
  replyQuorum: number; // size of the reply quorum
  receivedReplies: number = 0; // Number of received replies
  invokeTimeout: number = 40;
  replyServer: number;
  invokeUnorderedHashedTimeout: number = 10;

  replyListener: ReplyListener; // app callback to execute on reply

  requestType: TOMMessageType;
  replies: TOMMessage[] = []; // Replies from replicas are stored here
  response: TOMMessage = null; // Reply delivered to the application

  hashResponseController: HashResponseController;

  comparator: Comparator<any>;
  extractor: Extractor;


  public constructor(TOMConfiguration: TOMConfiguration) {
    super(TOMConfiguration);

    this.comparator = (this.comparator != null) ? this.comparator : {
      compare: function (o1: any, o2: any): number {
        return JSON.stringify(o1) === JSON.stringify(o2) ? 1 : 0;
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


    let lastReceived = reply.sender;
    this.replies[lastReceived] = reply;
    let replyQuorum = this.getReplyQuorum();


    /* Handle Reconfiguration from reply */

    let viewChange = reply.viewId > this.getViewController().getCurrentViewId() ? 1 : 0;
    for (let i in this.replies) {
      if (Number(i) !== lastReceived &&
        this.replies[i].viewId === reply.viewId) {
        viewChange++
      }
    }

    if (viewChange >= replyQuorum) {
      this.reconfigureTo(reply.content);
    }


    /* Compare content with other replies,
     * compare same content for same viewId and same operationId */

    let sameContent = 1;
    for (let i in this.replies) {
        if (Number(i) !== lastReceived &&
          this.comparator.compare(this.replies[i].content, reply.content) &&
          this.replies[i].viewId === reply.viewId &&
          this.replies[i].operationId === reply.operationId &&
          this.replies[i].sequence === reply.sequence) {
            sameContent++;
        }
      }


    // When response passes quorum,
    // deliver it to application via replyListener

    if (sameContent >= replyQuorum) {
      this.response = this.extractor.extractResponse(this.replies, sameContent, lastReceived);
      this.replies = [];
      console.log('validated ', this.response);
      this.replyListener.replyReceived(this.response);
    }

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

    // Clean all statefull data to prepare for receiving next replies
    this.replies = [];
    this.receivedReplies = 0;
    this.response = null;
    this.replyListener = replyListener;
    this.replyQuorum = this.getReplyQuorum();

    // Send the request to the replicas, and get its ID
    this.reqId = this.generateRequestId(reqType);
    this.operationId = this.generateOperationId();
    this.requestType = reqType;

    this.replyServer = -1;
    this.hashResponseController = null;

    if (reqType === TOMMessageType.UNORDERED_HASHED_REQUEST) {

      this.replyServer = this.getRandomlyServerId();


      let viewController: ClientViewController = this.getViewManager();

      let hashResponseController = new HashResponseController(viewController.getCurrentViewPos(this.replyServer),
        viewController.getCurrentViewProcesses().length);

      let sm: TOMMessage = new TOMMessage(this.me, this.session, this.reqId, this.operationId, request,
        this.getViewManager().getCurrentViewId(), reqType);

      sm.setReplyServer(this.replyServer);

      this.TOMulticast(sm, this);

    } else {

      this.TOMulticastData(request, this.reqId, reqType, this.operationId, this);

    }

  }

  private getReplyQuorum(): number {
    let n: number = this.getViewController().getCurrentViewF();
    let f: number = this.getViewController().getCurrentViewN();

    // formula for quorum is [(n+f)/2]+1
    let replyQuorum = Math.ceil((n + f) / 2) + 1;
    return replyQuorum;
  }

  private getRandomlyServerId(): number {
    let numServers: number = this.getViewController().getCurrentViewProcesses().length;
    let pos = Math.floor(Math.random() * numServers);
    let id = this.getViewController().getCurrentViewProcesses()[pos];
    return id;
  }


  private reconfigureTo(view: View) {
    console.log('RECONFIG!');
    this.getViewController().setCurrentView(view);
  }

}
