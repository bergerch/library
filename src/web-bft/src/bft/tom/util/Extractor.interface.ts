import {TOMMessage} from "../ServiceProxy.service";

export interface Extractor {

  extractResponse(replies: TOMMessage[], sameContent: number, lastReceived: number): TOMMessage;

}
