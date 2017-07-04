import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';
import {Subject, Observable, Subscription} from 'rxjs/Rx';
import {ServiceProxy} from "../bft/tom/ServiceProxy.service";
import {TOMConfiguration} from "../bft/config/TOMConfiguration";
import {WebsocketService} from '../bft/communication/Websocket.service';
import {TOMMessage} from "../bft/tom/messages/TOMMessage";
import {ReplyListener} from "../bft/communication/ReplyListener.interface";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  providers: [ServiceProxy, TOMConfiguration]
})
export class AppComponent implements OnInit, ReplyListener {
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
  reply: any;

  constructor(websocketService: WebsocketService, private counterProxy: ServiceProxy) {

  }

  ngOnInit() {

  }

  replyReceived(sm: TOMMessage) {
    this.count = sm.content;
    this.message = "" + sm.content;
    // console.log(sm);
  }

  launchCounter() {


    // Counter already initialized
    if (this.counterSubscription) {
      this.counterSubscription.unsubscribe();
    }
    this.counter = Observable.interval(10);
    this.counterSubscription = this.counter.subscribe((num) => {
      this.countChange.emit(this.counterValue);
      this.sentMessage = "" + this.counterValue;
      // console.log('application called counterProxy.invokeOrdered() with ', {
      //  counter: this.counterValue
      // });
      this.reply = this.counterProxy.invokeOrdered(this.counterValue, this);
      // console.log(this.message);
    });


  }

  haltCounter() {

    if (this.counterSubscription) {
      this.counterSubscription.unsubscribe();
    }

  }


}

