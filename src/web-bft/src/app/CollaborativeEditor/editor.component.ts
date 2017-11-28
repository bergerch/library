import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';
import {Subject, Observable, Subscription} from 'rxjs/Rx';
import {ServiceProxy} from "../../bft/tom/ServiceProxy.service";
import {TOMConfiguration} from "../../bft/config/TOMConfiguration";
import {TOMMessage} from "../../bft/tom/messages/TOMMessage";
import {ReplyListener} from "../../bft/communication/ReplyListener.interface";
import {Router} from "@angular/router";


@Component({
  selector: 'editor',
  templateUrl: './editor.component.html',
  styleUrls: ['./editor.component.css'],
  providers: [ServiceProxy, TOMConfiguration]
})
export class Editor implements OnInit, ReplyListener {


  /** Editor Application member variables */
  editor;
  editorObservable: Observable<any>;
  editorSubscription: Subscription;
  client_shadow;
  cursorPosition;
  range;
  subscribed: boolean;
  local_change;
  dmp;
  id;

  /** Performance measurement fields, only used for evaluation purpose */
  numberOfOps: number = 200000; // How many operations each client executs e.g. the number of requests
  interval: number = 50; // Milliseconds a client waits before sending the next request
  readOnly: boolean = false; // If client should send read-only requests instead of ordered requests
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
  averageLatencyAll: number = -1;
  maxLatency: number = 0;
  minLatency: number = 0;
  averageLatency_1st_decile: number = 0;
  averageLatency_10nd_decile: number = 0;
  standard_deviation: number = 0;
  statisticComputed: boolean = false;
  sampleRate: number = 100;
  sampleCount: number = 0;
  startedAutoWrite = false;


  constructor(private editorProxy: ServiceProxy,  router: Router) {
    this.id = this.editorProxy.getId();
    let url = router.url.toString();
    this.measureLatency = url.charAt(url.length - 1) == 'l';
    console.log(this.measureLatency);
    console.log('sample Rate ', this.sampleRate);
  }


  ngOnInit() {
    // Init Diff Match Patch
    let DiffMatchPatch = require('diff-match-patch');
    this.dmp = new DiffMatchPatch();

    // Init Editor and copy
    this.editor = document.getElementById('editor');
    this.editor.focus();
    this.client_shadow = this.editor.innerHTML;

    // Create Listener for document changes
    this.createListener();

    // Get initial document state and subscribe for changes
    this.joinDocument();

  }

  createListener() {
    this.editor.addEventListener('input', (e) => {
      // part of Differential Synchronisation: create a Diff
      let write = e.target.innerHTML;
      let d = this.dmp.diff_main(this.client_shadow, write);
      this.dmp.diff_cleanupSemantic(d);
      this.client_shadow = write;

      // Send write command to replica set

      this.editorProxy.invokeOrdered({operation: 'write', data: d}, this);

    });

    this.editor.addEventListener('click', (e) => {
     // console.log(this.getCurrentCursorPosition('editor'));
    });

  }

  joinDocument() {
    this.editorObservable = Observable.interval(200);
    this.editorSubscription = this.editorObservable.subscribe((num) => {
      if (!this.subscribed) {
        // Subscribe to document changes
        this.editorProxy.invokeOrderedSubscribe({operation: 'subscribe', data: 'onDocChange'}, this, 'onDocChange');

        // Read initial document state
        this.editorProxy.invokeUnordered({operation: 'read'}, this);
        this.subscribed = true;
      } else {
        if (!this.startedAutoWrite && num > 2) {
          this.autoWritePerformanceMeasurement();
          this.startedAutoWrite = true;
        }
      }
    });
  }

  replyReceived(sm: TOMMessage) {


    // Fix cursor position
    let cursorPosition = this.getCurrentCursorPosition('editor');
    //console.log(cursorPosition);
    if (cursorPosition != -1)
      this.cursorPosition = cursorPosition;

    // Read data
    let buff = new Buffer(sm.content.data);

    // Operation is one of: subscribe, unsubscribe, read, write
    switch (sm.content.operation) {
      case 'write':
       // console.log('Writing...');

        /// IfDef Performance measurement
        if (sm.content.requester === this.id) {
          this.requestReceived++;
          this.sampleCount++;
          this.requestsReceivedTime.set(sm.sequence, window.performance.now());
          if (this.sampleCount % (1000 / this.interval) === 0 && this.measureLatency) {
            // console.log('compute Statistic');
            this.averageLatencyAll = this.computeStatistic(sm.sequence);
            this.averageLatencyAll = isNaN(this.averageLatencyAll) ? -1.0 : this.averageLatencyAll;
            this.editorProxy.invokeOrdered({operation:"latency-measurement", data: this.averageLatencyAll}, this);
          }
          /*
          // Last reply arrived
          if (sm.sequence == this.numberOfOps - 1) {
            console.log('compute Statistic');
            this.computeStatistic();
            this.editorProxy.invokeOrdered({operation:"latency-measurement", data: this.averageLatencyAll}, this);
          }
          */
        }
        /// EndIfDef

        let diffs = sm.content.data;

        // Create Patch relative to document version
        let patch_changed = this.dmp.patch_make(this.local_change, diffs);
        let patch = this.dmp.patch_make(this.client_shadow, diffs);
        let patch2 = this.dmp.patch_make(this.editor.innerHTML, diffs);

        this.local_change = this.dmp.patch_apply(patch_changed, this.local_change)[0];
        if (this.local_change != this.client_shadow) {
          // Apply Patch to document
          this.client_shadow = this.dmp.patch_apply(patch, this.client_shadow)[0];
          this.editor.innerHTML = this.dmp.patch_apply(patch2, this.editor.innerHTML)[0];
        }
        break;
      case 'read':
      case 'subscribe':
       // console.log('Reading...');
        let document = buff.toString('utf8');
        this.editor.innerHTML = document;
        this.client_shadow = document;
        this.local_change = document;
        break;
    }
    this.editor.focus();
    this.setCurrentCursorPosition(this.cursorPosition);
  }


  createRange(node, chars, range?) {
    if (!range) {
      range = document.createRange();
      range.selectNode(node);
      range.setStart(node, 0);
    }

    if (chars.count === 0) {
      range.setEnd(node, chars.count);
    } else if (node && chars.count > 0) {
      if (node.nodeType === Node.TEXT_NODE) {
        if (node.textContent.length < chars.count) {
          chars.count -= node.textContent.length;
        } else {
          range.setEnd(node, chars.count);
          chars.count = 0;
        }
      } else {
        for (var lp = 0; lp < node.childNodes.length; lp++) {
          range = this.createRange(node.childNodes[lp], chars, range);

          if (chars.count === 0) {
            break;
          }
        }
      }
    }
    return range;
  };


  setCurrentCursorPosition(chars) {
    if (chars >= 0) {
      var selection = window.getSelection();

      this.range = this.createRange(this.editor, {count: chars});

      if (this.range) {
        this.range.collapse(false);
        selection.removeAllRanges();
        selection.addRange(this.range);
      }
    }
  };


  isChildOf(node, parentId) {
    while (node !== null) {
      if (node.id === parentId) {
        return true;
      }
      node = node.parentNode;
    }

    return false;
  };


  getCurrentCursorPosition(parentId) {
    var selection = window.getSelection(),
      charCount = -1,
      node;

    if (selection.focusNode) {
      if (this.isChildOf(selection.focusNode, parentId)) {
        node = selection.focusNode;
        charCount = selection.focusOffset;

        while (node) {
          if (node.id === parentId) {
            break;
          }

          if (node.previousSibling) {
            node = node.previousSibling;
            charCount += node.textContent.length;
          } else {
            node = node.parentNode;
            if (node === null) {
              break
            }
          }
        }
      }
    }

    return charCount;
  };


  /**
   * Test purpose
   */
  autoWrite() {


    let shakespeare = 'FROM fairest creatures we desire increase,' +
      'That thereby beautys rose might never die, ' +
      'But as the riper should by time decease,' +
      'His tender heir might bear his memory: ' +
      'But thou, contracted to thine own bright eyes,' +
      'Feedst thy lightest flame with self-substantial fuel, ' +
      'Making a famine where abundance lies, ' +
      'Thyself thy foe, to thy sweet self too cruel. ' +
      'Thou that art now the worlds fresh ornament ' +
      'And only herald to the gaudy spring, ' +
      'Within thine own bud buriest thy content' +
      'And, tender churl, makest waste in niggarding. ' +
      'Pity the world, or else this glutton be, ' +
      'To eat the worlds due, by the grave and thee.';

    let observable = Observable.interval(200);
    let subscription = observable.subscribe((num) => {


      if (num > shakespeare.length) {
        subscription.unsubscribe();
        return;
      }

      this.editor.innerHTML = this.editor.innerHTML + shakespeare.charAt(num);

      let d = this.dmp.diff_main(this.client_shadow, this.editor.innerHTML);
      this.dmp.diff_cleanupSemantic(d);
      this.client_shadow = this.editor.innerHTML;

      // Send write command to replica set
      this.editorProxy.invokeOrdered({operation: 'write', data: d}, this);

    });

  }


  /**
   *  Evaluation purpose
   */
  autoWritePerformanceMeasurement() {

    let shakespeare = 'FROM fairest creatures we desire increase,' +
      'That thereby beautys rose might never die, ' +
      'But as the riper should by time decease,' +
      'His tender heir might bear his memory: ' +
      'But thou, contracted to thine own bright eyes,' +
      'Feedst thy lightest flame with self-substantial fuel, ' +
      'Making a famine where abundance lies, ' +
      'Thyself thy foe, to thy sweet self too cruel. ' +
      'Thou that art now the worlds fresh ornament ' +
      'And only herald to the gaudy spring, ' +
      'Within thine own bud buriest thy content' +
      'And, tender churl, makest waste in niggarding. ' +
      'Pity the world, or else this glutton be, ' +
      'To eat the worlds due, by the grave and thee.';

    this.requestReceived = 0;
    this.requestObservable = Observable.interval(this.interval);
    this.requestSubscription = this.requestObservable.subscribe((num) => {
      if (this.requestSent < this.numberOfOps) {
        this.requestSent++;
        let sequence;
        if (this.readOnly) {
          sequence = this.editorProxy.invokeUnordered({operation: 'read'}, this);
        } else {

          let currentDoc = this.editor.innerHTML;
          let randomPosition = Math.floor(Math.random() * currentDoc.length);
          let operationType = Math.random() > 0.49 ? 'INSERT' : 'DELETE';

          if (operationType === 'INSERT') {
            let pre = currentDoc.slice(0, randomPosition);
            let insertion = shakespeare.charAt(num % shakespeare.length);
            let post = currentDoc.slice(randomPosition);
            currentDoc = pre + insertion + post;
          }
          if (operationType === 'DELETE') {
            let pre = currentDoc.slice(0, randomPosition);
            let post = currentDoc.slice(randomPosition+1);
            currentDoc = pre + post;
          }
          this.editor.innerHTML = currentDoc;
          let d = this.dmp.diff_main(this.client_shadow, this.editor.innerHTML);
          this.dmp.diff_cleanupSemantic(d);
          this.client_shadow = this.editor.innerHTML;

          // Send write command to replica set
          sequence = this.editorProxy.invokeOrdered({operation: 'write', data: d}, this);

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

          if (this.measureLatency) {
             this.requestsSentTime.set(sequence, window.performance.now());
          }

      } else {
        this.requestSubscription.unsubscribe();
      }
    });

  }

  computeStatistic(index: number) {


    index = index - this.sampleRate > 0 ? index - this.sampleRate : 0;

    //console.log('index ', index);
    let latencies: Map<number, number> = new Map();
    // Compute all latencies
    let k = 0;
    //console.log(this.requestsReceivedTime);
    //console.log(this.requestsSentTime);
    for (let i = index; i < index + this.sampleRate; i++) {
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
    for (let i = 0; i < this.sampleRate; i++) {
      if (latencies.get(i) > 0)
        s += latencies.get(i);
    }
    this.averageLatencyAll = s / latencies.size;
    this.averageLatencyAll = Math.round(this.averageLatencyAll * 100) / 100;

    //console.log(this.averageLatencyAll);
    return this.averageLatencyAll;

/*
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

*/
    //this.statisticComputed = true;
  }

}

