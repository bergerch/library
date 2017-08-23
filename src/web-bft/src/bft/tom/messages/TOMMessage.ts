import {SystemMessage} from "./SystemMessage";
import {Injectable} from "@angular/core";
import {TOMMessageType} from "./TOMMessageType";

@Injectable()
export class TOMMessage extends SystemMessage {

  viewId: number;
  type: TOMMessageType; // request type: application or reconfiguration request
  session: number; // Sequence number defined by the client
  // There is a sequence number for ordered and another for unordered messages
  sequence: number;
  operationId: number; // Sequence number defined by the client
  content: any; // Content of the message

  event: string;
  replyServer: number = -1;

  public constructor(sender: number, session: number, reqId: number, operationId: number, request: any, viewId: number,
                     requestType: TOMMessageType, event?: string) {
    super(sender);
    this.session = session;
    this.sequence = reqId;
    this.operationId = operationId;
    this.content = request;
    this.viewId = viewId;
    this.type = requestType;

    if (event) {
      this.event = event;
    }
  }


  public getReplyServer(): number {
    return this.replyServer;
  }


  public setReplyServer(replyServer: number) {
    this.replyServer = replyServer;
  }

}
