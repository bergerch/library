import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';
import {Subject, Observable, Subscription} from 'rxjs/Rx';
import {ServiceProxy} from "../../bft/tom/ServiceProxy.service";
import {TOMConfiguration} from "../../bft/config/TOMConfiguration";
import {TOMMessage} from "../../bft/tom/messages/TOMMessage";
import {ReplyListener} from "../../bft/communication/ReplyListener.interface";


@Component({
  selector: 'editor',
  templateUrl: './editor.component.html',
  styleUrls: ['./editor.component.css'],
  providers: [ServiceProxy, TOMConfiguration]
})
export class Editor implements OnInit, ReplyListener {

  editor;

  editorObservable: Observable<any>;
  editorSubscription: Subscription;

  client_shadow;

  cursorPosition;

  range;
  subscribed: boolean;

  local_change;
  dmp;

  constructor(private editorProxy: ServiceProxy) {
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
      console.log(this.getCurrentCursorPosition('editor'));
    });

  }

  joinDocument() {
    this.editorObservable = Observable.interval(300);
    this.editorSubscription = this.editorObservable.subscribe((num) => {
      if (!this.subscribed) {
        // Subscribe to document changes
        this.editorProxy.invokeOrderedSubscribe({operation: 'subscribe', data: 'onDocChange'}, this, 'onDocChange');

        // Read initial document state
        this.editorProxy.invokeUnordered({operation: 'read'}, this);
        this.subscribed = true;
      }
    });
  }

  replyReceived(sm: TOMMessage) {

    // Fix cursor position
    let cursorPosition = this.getCurrentCursorPosition('editor');
    console.log(cursorPosition);
    if (cursorPosition != -1)
      this.cursorPosition = cursorPosition;

    // Read data
    let buff = new Buffer(sm.content.data);

    // Operation is one of: subscribe, unsubscribe, read, write
    switch (sm.content.operation) {
      case 'write':
        console.log('Writing...');

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
        console.log('Reading...');
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

}

