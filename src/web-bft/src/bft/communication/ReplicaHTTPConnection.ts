/**
 * Created by bergerch on 23.05.17.
 */
import {Injectable} from '@angular/core';
import {Replica} from "./Replica";
import {SimpleHttpService} from "./SimpleHttp.service";
import {ReplicaConnection} from "./ReplicaConnection";

@Injectable()
export class ReplicaHTTPConnection extends Replica implements ReplicaConnection {

  private httpService: SimpleHttpService;
  private subscription;

  public constructor(simpleHttpService: SimpleHttpService, replicaId, secret?: string) {

    super(replicaId, secret);
    this.httpService = simpleHttpService;

  }

  public send(message) {
    if (this.subscription)
      this.httpService.post(message).then((response) => {
        this.subscription(response);
      });
  }

  public subscribe(subscription) {
    this.subscription = subscription;
  }

  public close() {
    // As HTTP is connectionless, there is no need to do anything
  }

}



