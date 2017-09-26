import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';
import {ServiceProxy} from "../../bft/tom/ServiceProxy.service";
import {TOMConfiguration} from "../../bft/config/TOMConfiguration";

@Component({
  selector: 'throughputLatency',
  templateUrl: './throughputLatency.component.html',
  styleUrls: ['./throughputLatency.component.css'],
  providers: [ServiceProxy, TOMConfiguration]
})
export class ThroughputLatency implements OnInit{

  processId: number = 1000; // Id of the first process
  threadNumber: number = 20; // Number of Threads used in the system
  numberOfOps : number = 10000; // How many operations each client executs e.g. the number of requests
  requestSize : number = 0; // Number of Bytes of content field for a message
  interval : number = 50; // Milliseconds a client waits before sending the next request
  readOnly : boolean = false; // If client should send read-only requests instead of ordered requests
  verbose : boolean = true; // Enable for additional output;
  dos : boolean = false; // For simulating a dos attack

  progress: number = 0; // For progress bar
  output;
  started: boolean = false;

  constructor(private proxy: ServiceProxy) {

  }


  ngOnInit() {

   this.output = document.getElementById('output');
   this.started = false
  }


  print(s: string) {
    this.output.innerHTML += s;
  }

  println(s: string) {
    this.output.innerHTML += s + '<br>';
  }

  startBenchmark() {
    this.started = true;
    this.println('Benchmark started with ' + this.threadNumber + ' threads. ' + 'Each thread will send ' + this.numberOfOps
    + ' requests. The request content size is set to ' + this.requestSize + ' Bytes. Each thread will wait at least ' + this.interval
      + ' ms before sending the next request. Read-only: ' + this.readOnly + ' Verbose: ' + this.verbose + ' dos: ' + this.dos +
    ' <br>Note the browser will send up to ' + this.threadNumber*1000/this.interval + ' requests per second.' );

  }
}

