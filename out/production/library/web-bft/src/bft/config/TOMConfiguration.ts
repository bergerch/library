/**
 * Created by bergerch on 27.04.17.
 */
import {Injectable} from '@angular/core';


export interface InternetAddress {

  address: string;
  port: number;

}

export interface Host extends InternetAddress {

  server_id: number;
  address: string;
  port: number;

}

@Injectable()
export class TOMConfiguration {


  /** Hosts Configurations */

  hosts: Host[] = [
    {server_id: 0, address: '127.0.0.1', port: 11005},
    {server_id: 1, address: '127.0.0.1', port: 11015},
    {server_id: 2, address: '127.0.0.1', port: 11025},
    {server_id: 3, address: '127.0.0.1', port: 11035}
    /*
    ,
    {server_id: 4, address: '127.0.0.1', port: 11045},
    {server_id: 5, address: '127.0.0.1', port: 11055},
    {server_id: 6, address: '127.0.0.1', port: 11065},
    {server_id: 7, address: '127.0.0.1', port: 11075},
    */
  ];


  /** Reconfiguration Configurations */

    // Replicas ID for the initial view, separated by a comma.
    // The number of replicas in this parameter should be equal to that specified in 'system.servers.num'
  initial_view = [0, 1, 2, 3];

  // The ID of the trust third party (TTP)
  ttp_id = 7002;

  // This sets if the system will function in Byzantine or crash-only mode. Set to "true" to support Byzantine faults
  bft = true;


  /** Communication Configurations */

    // MAC algorithm used to authenticate messages between processes (HmacMD5 is the default value)
    // This parameter is not currently being used being used
  hmacAlgorithm = 'HmacSHA1';

  // Specify if the communication system should use a thread to send data (true or false)
  useSenderThread = true;

  // Force all processes to use the same public/private keys pair and secret key. This is useful when deploying experiments
  // and benchmarks, but must not be used in production systems.
  defaultkeys = true;


  /** Replication Algorithm Configurations */

    // Number of servers in the group
  n = 4;

  // Maximum number of faulty replicas
  f = 1;

  // Timeout to asking for a client request
  timeout = 2000;


  // Maximum batch size (in number of messages)
  maxbatchsize = 400;

  // Number of nonces (for non-determinism actions) generated
  nonces = 10;

  // if verification of leader-generated timestamps are increasing
  // it can only be used on systems in which the network clocks
  // are synchronized
  verifyTimestamps = false;

  // Quantity of messages that can be stored in the receive queue of the communication system
  inQueueSize = 500000;

  //  Quantity of messages that can be stored in the send queue of each replica
  outQueueSize = 500000;

  // Set to 1 if SMaRt should use signatures, set to 0 if otherwise
  useSignatures = false;

  // Set to 1 if SMaRt should use MAC's, set to 0 if otherwise
  useMACs = true;

  // Set to 1 if SMaRt should use the standard output to display debug messages, set to 0 if otherwise
  debug = true;

  // Print information about the replica when it is shutdown
  shutdownhook = true;


  /** State Transfer Configurations */

    // Activate the state transfer protocol ('true' to activate, 'false' to de-activate)
  state_transfer = true;

  // Maximum ahead-of-time message not discarded
  highMark = 10000;

  // Maximum ahead-of-time message not discarded when the replica is still on EID 0 (after which the state transfer is triggered)
  revival_highMark = 10;

  // Number of ahead-of-time messages necessary to trigger the state transfer after a request timeout occurs
  timeout_highMark = 200;

}
