import {TOMMessage} from "../messages/TOMMessage";

export interface Extractor {

  extractResponse(replies: TOMMessage[], sameContent: number, lastReceived: number): TOMMessage;

}
