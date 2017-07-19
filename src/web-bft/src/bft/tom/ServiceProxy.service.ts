/**
 * Created by chris on 23.04.17.
 */
import {Injectable} from '@angular/core';
import {ReplyReceiver, TOMSender} from "./TOMSender.service";
import {HashResponseController} from "./HashResponseController.controller";
import {Host, InternetAddress, TOMConfiguration} from "../config/TOMConfiguration";
import {Comparator} from "./util/Comparator.interface";
import {Extractor} from "bft/tom/util/Extractor.interface";
import {ClientViewController} from "../reconfiguration/ClientViewController.controller";
import {TOMMessageType} from "./messages/TOMMessageType";
import {TOMMessage} from "./messages/TOMMessage";
import {ReplyListener} from "../communication/ReplyListener.interface";
import {View} from "../reconfiguration/View";
import Queue = jasmine.Queue;


@Injectable()
export class ServiceProxy extends TOMSender implements ReplyReceiver {


  operationId: number = -1;
  reqId: number = 0;

  invokeTimeout: number = 40;
  replyServer: number;
  invokeUnorderedHashedTimeout: number = 10;

  replyListeners: Map<number, ReplyListener> = new Map();
  replies: Map<number, TOMMessage[]> = new Map();
  hashResponseControllers: Map<number, HashResponseController> = new Map();

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

    // the replica id of the reply received and its operation id
    let lastReceived: number = reply.sender;
    let sequence: number = reply.sequence;

    // Save the reply in an array for the pending request with operationId
    let replies: TOMMessage[] = this.replies.get(sequence);
    if (!replies) {
      replies = [];
      this.replies.set(sequence, replies);
    }
    replies[lastReceived] = reply;

    // Determine the current reply quorum
    let replyQuorum: number = this.getReplyQuorum();

    // Handle Reconfiguration from reply
    let currentViewId = this.getViewController().getCurrentViewId();
    let viewChange = reply.viewId > currentViewId ? 1 : 0;
    for (let i in replies) {
      if (Number(i) !== lastReceived &&
        replies[i].viewId === reply.viewId &&
        reply.viewId > currentViewId) {
        viewChange++
      }
    }

    // When client received enough reconfig messages
    if (viewChange >= replyQuorum) {
      reply.content = JSON.parse(reply.content);
      let hosts: Host[] = [];

      // Create new View and reconfigure to it
      for (let k in reply.content.addresses) {
        hosts.push({
          server_id: reply.sender,
          port: reply.content.addresses[k].inetaddress.port,
          address: reply.content.addresses[k].inetaddress.address
        })
      }

      console.log('Hosts ', hosts);
      let view: View = new View(reply.content.id, reply.content.processes, reply.content.f, hosts);
      this.reconfigureTo(view);
      return;
    }

    // Compare content with other replies, compare same content for same viewId and same operationId
    let sameContent = 1;

    for (let other of replies) {
      if (other && other.sender !== reply.sender &&
        this.comparator.compare(other.content, reply.content) &&
        other.viewId === reply.viewId &&
        other.operationId === reply.operationId &&
        other.sequence === reply.sequence) {
        sameContent++;
      }
    }

    // When response passes quorum, deliver it to application via replyListener
    if (sameContent >= replyQuorum) {
      let response = this.extractor.extractResponse(replies, sameContent, lastReceived);
      console.log('validated ', response);
      this.replies.delete(response.sequence);
      this.replyListeners.get(response.sequence).replyReceived(response);
      this.replyListeners.delete(response.sequence);
    }

  }

  public invokeOrdered(request, replyListener?: ReplyListener): any {
    return this.invoke(request, TOMMessageType.ORDERED_REQUEST, replyListener);
  }

  public invokeUnordered(request, replyListener?: ReplyListener): any {
    return this.invoke(request, TOMMessageType.UNORDERED_REQUEST, replyListener);
  }

  public invokeUnorderedHashed(request, replyListener?: ReplyListener): any {
    // TODO
    throw new Error("not yet implemented")
    //return this.invoke(request, TOMMessageType.UNORDERED_HASHED_REQUEST, replyListener);
  }


  /**
   * This method invokes a request to be send to all replicas to be executed.
   * This method works asynchronous.
   * @param request the request to be send
   * @param reqType type of request e.g. ordered, unordered
   * @param replyListener application defined callback, will be executed when response is obtained
   */
  private invoke(request, reqType: number, replyListener?: ReplyListener): any {

    // Clear all statefull data to prepare for receiving next replies


    this.replyListeners.set(this.sequence, replyListener);

    this.reqId = this.generateRequestId(reqType);

    this.operationId = this.generateOperationId();

    if (!this.replies.get(this.sequence)) {
      this.replies.set(this.sequence, []);
    }

    this.replyServer = -1;
    this.hashResponseControllers = null;

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

  /**
   * Computes the reply quorum, depends on f and n
   * @returns {number}
   */
  private getReplyQuorum(): number {
    let n: number = this.getViewController().getCurrentViewF();
    let f: number = this.getViewController().getCurrentViewN();

    // formula for quorum is [(n+f)/2]+1
    let replyQuorum = Math.ceil((n + f) / 2) + 1;
    return replyQuorum;
  }

  /**
   * Selects randomly a server id
   * @returns {number}
   */
  private getRandomlyServerId(): number {
    let numServers: number = this.getViewController().getCurrentViewProcesses().length;
    let pos = Math.floor(Math.random() * numServers);
    let id = this.getViewController().getCurrentViewProcesses()[pos];
    return id;
  }


  /**
   * Reconfigures to a nw view
   * @param view
   */
  private reconfigureTo(view: View) {
    this.getViewController().setCurrentView(view);
    this.cs.updateConnections();
  }

}
