import {Replica} from "./Replica";

export interface ReplicaConnection extends Replica {

  send(message);
  subscribe(callback);
  close();

}
