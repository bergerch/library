import {Injectable} from '@angular/core';
import {TOMMessage} from "./messages/TOMMessage";


@Injectable()
export class HashResponseController {

  reply: TOMMessage;
  hashReplies: Uint8Array[];
  replyServerPos: number;
  countHashReplies: number;

  public constructor(replyServerPos: number, length: number) {
    this.replyServerPos = replyServerPos;
    this.hashReplies = [];
    this.reply = null;
    this.countHashReplies = 0;
  }

  public getResponse(pos: number, tomMessage: TOMMessage): TOMMessage {

    if (this.hashReplies[pos] == null) {
      this.countHashReplies++;
    }

    if (this.replyServerPos == pos) {
      this.reply = tomMessage;
      //this.hashReplies[pos] = TOMUtil.computeHash(tomMessage.getContent());
    } else {
      this.hashReplies[pos] = tomMessage.content;
    }
    // console.log("["+this.getClass().getName()+"] hashReplies["+pos+"]="+Arrays.toString(hashReplies[pos]));

    if (this.hashReplies[this.replyServerPos] != null) {
      // let sameContent: number = 1;
    }

    // TODO

    return null;
  }


}
