import { Component, OnInit } from '@angular/core';
import { WebsocketService } from './websocket.service';
import { Subject, Observable, Subscription } from 'rxjs/Rx';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
    
    private title = 'Web-BFT Demo!';
    private socket: Subject<any>;
    private counterSubscription: Subscription;
    private message: string;
    private sentMessage: string;
    
    constructor(websocketService: WebsocketService) {
        this.socket = websocketService.createWebsocket();
    }
    
    ngOnInit() {
        this.socket.subscribe(message => this.message = message.data);
    }
    
    private launchCounter() { 
        //Counter already initialized
        if(this.counterSubscription) {
            this.counterSubscription.unsubscribe();
        }
        
        let counter = Observable.interval(1000);
        this.counterSubscription = counter.subscribe(num => {
            this.sentMessage = 'Websocket Message '+ num;
            this.socket.next(this.sentMessage);
        });
    }
}