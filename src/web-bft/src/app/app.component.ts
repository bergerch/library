import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';
import {WebsocketService} from './websocket.service';
import {Subject, Observable, Subscription} from 'rxjs/Rx';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
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
  counter: Observable<any>;
  color = 'accent';
  bft_enabled = false;

  constructor(websocketService: WebsocketService) {
    this.socket = websocketService.createWebsocket('wss://echo.websocket.org');
  }

  ngOnInit() {
    this.socket.subscribe( (message) => {
      this.message = message.data;
    });
  }

  launchCounter() {
    if(this.bft_enabled) {
      // TODO call ServiceProxy invoke()
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
    if(this.bft_enabled) {

    } else {
      if (this.counterSubscription) {
        this.counterSubscription.unsubscribe();
      }
    }
  }



}

