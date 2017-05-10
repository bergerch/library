import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';
import {WebsocketService} from './websocket.service';
import {Subject, Observable, Subscription} from 'rxjs/Rx';
import {ServiceProxy} from "../bft/tom/ServiceProxy.service";
import {TOMConfiguration} from "../bft/config/TOMConfiguration";
import {Comparator} from "../bft/tom/util/Comparator.interface";
import {Extractor} from "../bft/tom/util/Extractor.interface";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  providers: [ServiceProxy, TOMConfiguration]
})
export class AppComponent implements OnInit {
  @Input() count = 0;
  @Output() countChange = new EventEmitter<number>();

  title = 'Web-BFT Demo!';

  socket: Subject<any>;
  counterSubscription: Subscription;
  message: string;
  sentMessage: string;
  counterValue: number = 0;
  iterations: number = 1000;
  counter: Observable<any>;
  color = 'accent';
  bft_enabled = false;
  reply: any;

  constructor(websocketService: WebsocketService, private counterProxy: ServiceProxy) {
    this.socket = websocketService.createWebsocket('wss://echo.websocket.org');
  }

  ngOnInit() {
    this.socket.subscribe((message) => {
      this.message = message.data;
    });
  }

  launchCounter() {
    if (this.bft_enabled) {
      // Counter already initialized
      if (this.counterSubscription) {
        this.counterSubscription.unsubscribe();
      }
      this.counter = Observable.interval(2000);
      this.counterSubscription = this.counter.subscribe((num) => {
        this.countChange.emit(this.counterValue);
        this.sentMessage = 'Websocket Message ' + this.counterValue;
        this.reply = this.counterProxy.invokeOrdered({counter: this.counterValue, iterations: this.iterations});
        console.log('application called counterProxy.invokeOrdered() with ', {
          counter: this.counterValue,
          iterations: this.iterations
        });
      });

    } else {
      // Counter already initialized
      if (this.counterSubscription) {
        this.counterSubscription.unsubscribe();
      }

      this.counter = Observable.interval(1000);
      this.counterSubscription = this.counter.subscribe((num) => {
        this.counterValue++;
        this.countChange.emit(this.counterValue);
        this.sentMessage = 'Websocket Message ' + this.counterValue;
        this.socket.next(this.sentMessage);
      });
    }
  }

  haltCounter() {

    if (this.counterSubscription) {
      this.counterSubscription.unsubscribe();
    }

  }


}

