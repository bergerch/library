
export interface ReplicaConnection {

  send(message);
  subscribe(callback);
  getSecret(): string;
  close();

}
