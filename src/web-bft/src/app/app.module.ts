import { MaterialModule } from '@angular/material';
import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { WebsocketService } from '../bft/communication/Websocket.service';
import { routing } from './app.routes';

import { AppComponent } from './app.component';
import {Counter} from "./CounterDemo/counter.component";
import {RouterModule} from "@angular/router";
import {Editor} from "./CollaborativeEditor/editor.component";
import {ThroughputLatency} from "./Microbenchmarks/throughputLatency.component";
import {Runner} from "./Microbenchmarks/runner.component";


@NgModule({
  declarations: [
    AppComponent,
    Counter,
    Editor,
    ThroughputLatency,
    Runner
  ],
  imports: [
    MaterialModule.forRoot(),
    BrowserModule,
    FormsModule,
    HttpModule,
    routing,
    RouterModule
  ],
  providers: [WebsocketService],
  bootstrap: [AppComponent]
})
export class AppModule { }
