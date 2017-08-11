import {Injectable} from "@angular/core";
import * as CryptoJS from "../../../node_modules/crypto-js/crypto-js.js";
@Injectable()
export class MAC {

  private secret: string;

  public constructor(secret: string) {
    this.secret = secret;
  }

  public compute(message: string): string {
    return CryptoJS.enc.Hex.stringify(CryptoJS.HmacSHA1(message, this.secret));
  }

  public validate(message: string, hmacReceived: string): boolean {
    let hmacComputed = CryptoJS.enc.Hex.stringify(CryptoJS.HmacSHA1(message, this.secret));
    return hmacComputed == hmacReceived
  }
}
