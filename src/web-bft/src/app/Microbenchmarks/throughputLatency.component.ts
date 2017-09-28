import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';
import {Observable} from "rxjs/Observable";
import {Subscription} from "rxjs/Subscription";

@Component({
  selector: 'throughputLatency',
  templateUrl: './throughputLatency.component.html',
  styleUrls: ['./throughputLatency.component.css']
})
export class ThroughputLatency implements OnInit{

  processId: number = 1000; // Id of the first process
  threadNumber: number = 4; // Number of Threads used in the system
  numberOfOps : number = 10000; // How many operations each client executs e.g. the number of requests
  requestSize : number = 0; // Number of Bytes of content field for a message
  interval : number = 5; // Milliseconds a client waits before sending the next request
  readOnly : boolean = false; // If client should send read-only requests instead of ordered requests
  verbose : boolean = true; // Enable for additional output;
  dos : boolean = false; // For simulating a dos attack

  progress: number = 0; // For progress bar
  output;
  started: boolean = false;

  progressObservable: Observable<any>;
  progressSubscription: Subscription;

  constructor() {

  }

  ngOnInit() {

   this.output = document.getElementById('output');
   this.started = false;



  }


  print(s: string) {
    this.output.innerHTML += s;
  }

  println(s: string) {
    this.output.innerHTML += s + '<br>';
  }

  abort() {
    this.started = false;
    this.output.innerHTML = '';

  }

  startBenchmark() {
    this.started = true;
    if (this.verbose) this.println('Benchmark started with ' + this.threadNumber + ' threads. ' + 'Each thread will send ' + this.numberOfOps
    + ' requests. The request content size is set to ' + this.requestSize + ' Bytes. Each thread will wait at least ' + this.interval
      + ' ms before sending the next request. Read-only: ' + this.readOnly + ' Verbose: ' + this.verbose + ' dos: ' + this.dos +
    ' Note the browser will send up to ' + this.threadNumber*1000/this.interval + ' requests per second.' );


    let runner = document.getElementById("runner");

    for (let i = 0; i < this.threadNumber; i++) {
      runner.click();
    }


    let i = 100;
    let n = 0;

    this.progressObservable = Observable.interval(i);
    this.progressSubscription = this.progressObservable.subscribe((num) => {

      n+= (i/this.interval);

      this.progress = n / this.numberOfOps * 100;


    });


  }



  download() {
    let a = document.body.appendChild(document.createElement("a"));
    a.download = "export.html";
    a.href = "data:text/html," + this.output.innerHTML;
    a.click();
  }


}


