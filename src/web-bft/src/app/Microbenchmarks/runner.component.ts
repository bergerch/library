import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';
import {ServiceProxy} from "../../bft/tom/ServiceProxy.service";
import {TOMConfiguration} from "../../bft/config/TOMConfiguration";
import {Observable} from "rxjs/Observable";
import {Subscription} from "rxjs/Subscription";
import {ReplyListener} from "../../bft/communication/ReplyListener.interface";
import {TOMMessage} from "../../bft/tom/messages/TOMMessage";
import {Router} from "@angular/router";

@Component({
  selector: 'runner',
  templateUrl: './runner.component.html',
  styleUrls: ['./runner.component.css'],
  providers: [ServiceProxy, TOMConfiguration]
})
export class Runner implements OnInit, ReplyListener {


  numberOfOps: number = 40000000; // How many operations each client executs e.g. the number of requests
  requestSize: number = 4000; // Number of Bytes of content field for a message
  interval: number = 0; // Milliseconds a client waits before sending the next request
  readOnly: boolean = false; // If client should send read-only requests instead of ordered requests
  dos: boolean = false; // For simulating a dos attack

  progress: number = 0; // For progress bar
  output;
  started: boolean = false;
  opsPerSecond: number = 0;
  lastTime = window.performance.now();

  measureLatency: boolean = false;

  requestObservable: Observable<any>;
  requestSubscription: Subscription;

  requestSent = 0;
  requestReceived = 0;

  requestsSentTime: Map<number, number> = new Map();
  requestsReceivedTime: Map<number, number> = new Map();

  averageLatencyAll: number = 0;
  maxLatency: number = 0;
  minLatency: number = 0;
  averageLatency_1st_decile: number = 0;
  averageLatency_10nd_decile: number = 0;
  standard_deviation: number = 0;

  statisticComputed: boolean = false;

  constructor(private proxy: ServiceProxy, router: Router) {
    let url = router.url.toString();
    this.measureLatency = url.charAt(url.length - 1) == 'l';
    console.log(this.measureLatency);
  }

  ngOnInit() {
    this.output = document.getElementById('output');
    this.started = true;
    if (this.measureLatency) {
      /*
      this.interval = JSON.parse(localStorage.getItem('Benchmark-interval'));
      this.numberOfOps = JSON.parse(localStorage.getItem('Benchmark-numberOfOps'));
      this.requestSize = JSON.parse(localStorage.getItem('Benchmark-requestSize'));
      this.readOnly = JSON.parse(localStorage.getItem('Benchmark-readOnly'));*/
      this.numberOfOps = 5000;
    }
    console.log(this.interval, this.numberOfOps, this.requestSize, this.readOnly);
    let request = this.createDummyJSON(this.requestSize);
    this.requestObservable = Observable.interval(this.interval);
    this.requestSubscription = this.requestObservable.subscribe(() => {
      if (this.requestSent < this.numberOfOps) {
        this.requestSent++;
        let sequence;
        if (this.readOnly) {
          sequence = this.proxy.invokeUnordered({test: request}, this);
        } else {
          sequence = this.proxy.invokeOrdered({test: request}, this);
        }
        if (this.requestSent % 100 == 0) {
          let newTime = window.performance.now();
          let diff = newTime - this.lastTime;
          let time1Req = diff / 100;
          let numReq = 1000 / time1Req;
          this.opsPerSecond = Math.floor(numReq);
          this.lastTime = window.performance.now();
          this.progress = this.requestSent / this.numberOfOps * 100;

        }
        if (this.requestSent >= this.numberOfOps / 2) {
          this.requestsSentTime.set(sequence, window.performance.now());
        }
      } else {
        this.requestSubscription.unsubscribe();
      }
    });

  }

  replyReceived(sm: TOMMessage) {
    this.requestReceived++;
    if (this.requestReceived >= this.numberOfOps / 2) {
      this.requestsReceivedTime.set(sm.sequence, window.performance.now());
    }
    // Last reply arrived
    if (sm.sequence == this.numberOfOps - 1) {
      console.log('compute Statistic');
      this.computeStatistic();

    }

  }

  computeStatistic() {

    let latencies: Map<number, number> = new Map();
    // Compute all latencies
    let k = 0;
    for (let i = this.numberOfOps / 2; i < this.numberOfOps / 2 + this.requestsReceivedTime.size; i++) {
      if (this.requestsReceivedTime.get(i)) {
        // latency[request_i] = T_req_Received[i] - T_req_Sent[i]
        let latency = this.requestsReceivedTime.get(i) - this.requestsSentTime.get(i);
        latency = Math.round(latency * 1000) / 1000;
        latencies.set(k, latency);
        k++;
        //console.log(latency);
      }
    }

    // Compute average Latency of all latencies
    let s = 0;
    for (let i = 0; i < latencies.size; i++) {
      s += latencies.get(i);
    }
    this.averageLatencyAll = s / latencies.size;
    this.averageLatencyAll = Math.round(this.averageLatencyAll * 100) / 100;


    // Compute max Latency
    let max = 0;
    for (let i = 0; i < latencies.size; i++) {
      if (latencies.get(i) > max)
        max = latencies.get(i);
    }
    this.maxLatency = max;

    // Compute min Latency
    let min = 99999999;
    for (let i = 0; i < latencies.size; i++) {
      if (latencies.get(i) < min)
        min = latencies.get(i);
    }
    this.minLatency = min;


    let latenciesAsc = [];

    for (let i = 0; i < latencies.size; i++) {
      latenciesAsc[i] = latencies.get(i);
    }
    latenciesAsc.sort((a,b) => {return a-b});
    console.log(latenciesAsc);

    // Compute averages latency of fastest 10%
    s = 0;
    for (let i = 0; i < latenciesAsc.length / 10; i++) {
      s += latenciesAsc[i];
    }
    this.averageLatency_1st_decile = s / (latenciesAsc.length / 10);
    this.averageLatency_1st_decile = Math.round(this.averageLatency_1st_decile * 100) / 100;

    // Compute averages latency of slowest 10%
    s = 0;
    for (let i = latenciesAsc.length - latenciesAsc.length / 10; i < latenciesAsc.length; i++) {
      s += latenciesAsc[i];
    }
    this.averageLatency_10nd_decile = s / (latenciesAsc.length / 10);
    this.averageLatency_10nd_decile = Math.round(this.averageLatency_10nd_decile * 100) / 100;

    // Compute standard deviation
    s = 0;
    let n = latencies.size;
    for (let i = 0; i < n; i++) {
      s += (latencies.get(i) - this.averageLatencyAll) * (latencies.get(i) - this.averageLatencyAll);
    }
    this.standard_deviation = Math.sqrt(s / n);
    this.standard_deviation = Math.round(this.standard_deviation * 100) / 100;


    this.statisticComputed = true;
  }

  createDummyJSON(size: number): string {


    let x = '';

    for (let i = 0; i < size; i++) {
      x += 'x';
    }

    return x;

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
    a.href = "data:text/html," + document.getElementById('output').innerHTML;
    a.click();
  }


}
