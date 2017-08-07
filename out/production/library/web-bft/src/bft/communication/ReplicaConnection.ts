
export interface ReplicaConnection {

  send(message);
  subscribe(callback);
  close();

}
