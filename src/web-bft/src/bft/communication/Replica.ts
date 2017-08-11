import {Injectable} from "@angular/core";
import {MAC} from "../crypto/MAC";
@Injectable()
export class Replica {

  replicaId: number;
  mac: MAC;

  public constructor(replicaId: number, secret?: string) {
    this.replicaId = replicaId;
    if (secret) {
      this.mac = new MAC(secret);
    }

  }

  public getReplicaId(): number {
    return this.replicaId;
  }

  public computeMAC(message: string): string {
    return this.mac ? this.mac.compute(message) : '';
  }

  public verifyMAC(message: string, macReceived: string): boolean {
    return this.mac ? this.mac.validate(message, macReceived) : false;
  }

}
