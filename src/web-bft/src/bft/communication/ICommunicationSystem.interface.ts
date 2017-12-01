import {TOMMessage} from "../tom/messages/TOMMessage";
import {ReplyReceiver} from "../tom/TOMSender.service";


/**
 * The Communication system represents the interface that connects with
 * BFT-SMaRTs ClientServerCommunicationSystemServerSide.
 */
export interface ICommunicationSystem {

  /**
   * Sends a TOMMessage sm to all replicas (broadcast). The replyReceiver is a callback function that will be executed
   * when the replica's response is obtained
   *
   * @param sign if the message should be signed
   * @param sm
   * @param replyReceiver
   */
  send(sign: boolean, sm: TOMMessage, replyReceiver?: ReplyReceiver, simulate_MAC_attack?: boolean);


  /**
   * Signs a TOMMessage
   *
   * @param sm TOMMessage
   */
  sign(sm: TOMMessage);

  /**
   * Closes all connections
   */
  close();

  /**
   * Updates all connections
   */
  updateConnections();

}
