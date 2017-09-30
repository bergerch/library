import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';
import {ServiceProxy} from "../../bft/tom/ServiceProxy.service";
import {TOMConfiguration} from "../../bft/config/TOMConfiguration";
import { WebWorkerService } from '../../../node_modules/angular2-web-worker/web-worker';
import {Observable} from "rxjs/Observable";
import {Subscription} from "rxjs/Subscription";
import {ReplyListener} from "../../bft/communication/ReplyListener.interface";
import {TOMMessage} from "../../bft/tom/messages/TOMMessage";

@Component({
  selector: 'runner',
  templateUrl: './runner.component.html',
  styleUrls: ['./runner.component.css'],
  providers: [ServiceProxy, TOMConfiguration]
})
export class Runner implements OnInit, ReplyListener {


  numberOfOps : number = 100000; // How many operations each client executs e.g. the number of requests
  requestSize : number = 0; // Number of Bytes of content field for a message
  interval : number = 1; // Milliseconds a client waits before sending the next request
  readOnly : boolean = false; // If client should send read-only requests instead of ordered requests
  dos : boolean = false; // For simulating a dos attack

  progress: number = 0; // For progress bar
  output;
  started: boolean = false;
  opsPerSecond: number = 0;
  lastTime =  new Date().getTime();


  requestObservable: Observable<any>;
  requestSubscription: Subscription;

  requestSent = 0;

  constructor(private proxy: ServiceProxy) {

  }

  ngOnInit() {

    this.output = document.getElementById('output');
    this.started = true;

    this.interval = JSON.parse(localStorage.getItem('Benchmark-interval'));
    this.numberOfOps = JSON.parse(localStorage.getItem('Benchmark-numberOfOps'));
    this.requestSize = JSON.parse(localStorage.getItem('Benchmark-requestSize'));
    this.readOnly = JSON.parse(localStorage.getItem('Benchmark-readOnly'));


    console.log(this.interval, this.numberOfOps, this.requestSize, this.readOnly);


    let request = this.createDummyJSON(this.requestSize);



    this.requestObservable = Observable.interval(this.interval);
    this.requestSubscription = this.requestObservable.subscribe( () => {

      if (this.readOnly) {
        this.proxy.invokeUnordered(request, this);
      } else {
        this.proxy.invokeOrdered(request, this);
      }
      this.requestSent++;
      if (this.requestSent % 100 == 0) {
        let newTime = new Date().getTime();
        let diff = newTime - this.lastTime;
        let time1Req = diff/100;
        let numReq = 1000/time1Req;
        this.opsPerSecond = Math.floor(numReq);
        this.lastTime = new Date().getTime();
        this.progress=this.requestSent/this.numberOfOps*100;

      }

    });

  }


  replyReceived(sm: TOMMessage) {

    if (this.requestSent % 100 == 0) {

    }

     // localStorage.setItem('Benchmark-progress', JSON.stringify(this.requestSent));


    if (this.requestSent > this.numberOfOps) {
      this.requestSubscription.unsubscribe();
      this.started = false;
    }
  }

  createDummyJSON(size: number) {


    let x = '';

    for (let i = 0; i<size/2; i++) { // Node UTF-16 Encoding, 2 Bytes per character are used
      x += 'x';
    }
    return JSON.stringify(x);

  }

  print(s: string) {
    this.output.innerHTML += s;
  }

  println(s: string) {
    this.output.innerHTML += s + '<br>';
  }


  download() {
    let a = document.body.appendChild(document.createElement("a"));
    a.download = "export.html";
    a.href = "data:text/html," + this.output.innerHTML;
    a.click();
  }


}
