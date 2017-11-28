import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';
import {Subject, Observable, Subscription} from 'rxjs/Rx';
import {ServiceProxy} from "../../bft/tom/ServiceProxy.service";
import {TOMConfiguration} from "../../bft/config/TOMConfiguration";

import {TOMMessage} from "../../bft/tom/messages/TOMMessage";
import {ReplyListener} from "../../bft/communication/ReplyListener.interface";

@Component({
  selector: 'counter',
  templateUrl: './counter.component.html',
  styleUrls: ['./counter.component.css'],
  providers: [ServiceProxy, TOMConfiguration]
})
export class Counter implements OnInit, ReplyListener {
  @Input() count = 0;
  @Output() countChange = new EventEmitter<number>();

  title = 'Web-BFT Demo!';
  color = 'accent';

  counterSubscription: Subscription;

  message: string;
  sentMessage: string;

  counterValue: number = 0;
  counter: Observable<any>;


  constructor(private counterProxy: ServiceProxy) {

  }

  ngOnInit() {

  }

  replyReceived(sm: TOMMessage) {
    this.count = sm.content;
    this.message = "" + this.count;
  }

  launchCounter() {

    // Counter already initialized
    if (this.counterSubscription) {
      this.counterSubscription.unsubscribe();
      this.counterSubscription = null;
      return;
    }

    this.counter = Observable.interval();
    this.counterSubscription = this.counter.subscribe((num) => {
      this.countChange.emit(this.counterValue);
      this.sentMessage = "" + this.counterValue;

      if (this.counterValue == 0) {
        this.counterProxy.invokeUnordered(this.counterValue, this)
      } else {
        this.counterProxy.invokeOrdered(this.counterValue, this);
      }

    });


  }

  haltCounter() {

    if (this.counterSubscription) {
      this.counterSubscription.unsubscribe();
    }

  }


}
