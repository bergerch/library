import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';
import {ServiceProxy} from "../../bft/tom/ServiceProxy.service";
import {TOMConfiguration} from "../../bft/config/TOMConfiguration";
import { WebWorkerService } from '../../../node_modules/angular2-web-worker/web-worker';

@Component({
  selector: 'runner',
  templateUrl: './runner.component.html',
  styleUrls: ['./runner.component.css'],
  providers: [ServiceProxy, TOMConfiguration]
})
export class Runner implements OnInit{

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
    this.started = true;

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
