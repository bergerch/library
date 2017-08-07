import { Injectable } from '@angular/core';
import {Http, RequestOptionsArgs} from '@angular/http';

@Injectable()
export class SimpleHttpService {

  constructor(private http: Http, private url) {

  }

  public async get(options?: RequestOptionsArgs): Promise<number> {
    const response = await this.http.get(this.url, options).toPromise();
    return response.json();
  }

  public async post(body: any, options?: RequestOptionsArgs): Promise<any> {
    const response = await this.http.post(this.url, body, options).toPromise();
    return response.json();
  }

}
