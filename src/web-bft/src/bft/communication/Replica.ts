import {Injectable} from "@angular/core";
@Injectable()
export class Replica {

  replicaId: number;
  secret: string;


  public constructor(replicaId: number, secret?: string) {
    this.replicaId = replicaId;
    this.secret = secret;
  }

  public getReplicaId(): number {
    return this.replicaId;
  }


  public getSecret(): string {
    return this.secret;
  }

  public setSecret(value: string) {
    this.secret = value;
  }

}
