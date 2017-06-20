import {Injectable} from "@angular/core";
import {Host} from "../../config/TOMConfiguration";

@Injectable()
export class HostsConfig {

  servers: Map<string, Host>;

}
