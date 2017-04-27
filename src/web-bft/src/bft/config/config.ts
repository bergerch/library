/**
 * Created by bergerch on 27.04.17.
 */
import {Injectable} from '@angular/core';


export interface InternetAddress {

  address: string;
  port: number;

}

export interface Host {

  server_id: number;
  address: string;
  port: number;

}

@Injectable()
export class Config {

  /** Hosts Configurations */

  hosts: Host[] = [
    {server_id: 0, address: '127.0.0.1', port: 11000},
    {server_id: 1, address: '127.0.0.1', port: 11010},
    {server_id: 2, address: '127.0.0.1', port: 11020},
    {server_id: 3, address: '127.0.0.1', port: 11030},
    {server_id: 4, address: '127.0.0.1', port: 11040},
    {server_id: 5, address: '127.0.0.1', port: 11050},
    {server_id: 6, address: '127.0.0.1', port: 11060},
    {server_id: 7, address: '127.0.0.1', port: 11070},
  ];


  /** Reconfiguration Configurations */

    // Replicas ID for the initial view, separated by a comma.
    // The number of replicas in this parameter should be equal to that specified in 'system.servers.num'
  initial_view = [0,1,2,3];

  // The ID of the trust third party (TTP)
  ttp_id = 7002;

  // This sets if the system will function in Byzantine or crash-only mode. Set to "true" to support Byzantine faults
  bft = true;


  /** Communication Configurations */

  // MAC algorithm used to authenticate messages between processes (HmacMD5 is the default value)
  // This parameter is not currently being used being used
  authentication_hmacAlgorithm = 'HmacSHA1';

  // Specify if the communication system should use a thread to send data (true or false)
  ommunication_useSenderThread = true;

  // Force all processes to use the same public/private keys pair and secret key. This is useful when deploying experiments
  // and benchmarks, but must not be used in production systems.
  communication_defaultkeys = true;


  /** Replication Algorithm Configurations */

  // Number of servers in the group
  servers_num = 4;

  // Maximum number of faulty replicas
  servers_f = 1;

  // Timeout to asking for a client request
  totalordermulticast_timeout = 2000;


  // Maximum batch size (in number of messages)
  totalordermulticast_maxbatchsize = 400;

  // Number of nonces (for non-determinism actions) generated
  totalordermulticast_nonces = 10;

  // if verification of leader-generated timestamps are increasing
  // it can only be used on systems in which the network clocks
  // are synchronized
  totalordermulticast_verifyTimestamps = false;

  // Quantity of messages that can be stored in the receive queue of the communication system
  communication_inQueueSize = 500000;

  //  Quantity of messages that can be stored in the send queue of each replica
  communication_outQueueSize = 500000;

  // Set to 1 if SMaRt should use signatures, set to 0 if otherwise
  communication_useSignatures = 0;

  // Set to 1 if SMaRt should use MAC's, set to 0 if otherwise
  communication_useMACs = 1;

  // Set to 1 if SMaRt should use the standard output to display debug messages, set to 0 if otherwise
  debug = 0;

  // Print information about the replica when it is shutdown
  shutdownhook = true;



  /** State Transfer Configurations */

  // Activate the state transfer protocol ('true' to activate, 'false' to de-activate)
  otalordermulticast_state_transfer = true;

  // Maximum ahead-of-time message not discarded
  totalordermulticast_highMark = 10000;

  // Maximum ahead-of-time message not discarded when the replica is still on EID 0 (after which the state transfer is triggered)
  totalordermulticast_revival_highMark = 10;

  // Number of ahead-of-time messages necessary to trigger the state transfer after a request timeout occurs
  totalordermulticast_timeout_highMark = 200;


}
