
import {Injectable} from "@angular/core";
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

