import {Injectable} from '@angular/core';
import {Headers, Http, RequestOptions, RequestOptionsArgs} from '@angular/http';


@Injectable()
export class SimpleHttpService {

  constructor(private http: Http, private url) {}

  /**
   * GET-Request to a HTTP Server
   *
   * @param options
   * @returns {Promise<any|Promise<any>>}
   */
  public async get(options?: RequestOptionsArgs): Promise<number> {
    const response = await this.http.get(this.url, options).toPromise();
    return response.json();
  }


  /**
   * POST-Request to a HTTP Server
   *
   * @param options
   * @returns {Promise<any|Promise<any>>}
   */
  public async post(body: any, options?: RequestOptionsArgs): Promise<any> {
    try {

      if (!options) {
        let headers = new Headers({'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'});
        options = new RequestOptions({headers: headers});
      }
      const response = await this.http.post(this.url, body, options).toPromise();
      return response.json();
    }
    catch (error) {
      console.error(error);
    }
  }

}
