import {TOMMessage} from "../tom/messages/TOMMessage";

export interface ReplyListener {

   replyReceived(sm: TOMMessage);

}
