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

  differ;

  dmp;

  constructor(private editorProxy: ServiceProxy) {
  }


  ngOnInit() {

    this.differ = this.editor = document.getElementById('differ');

    let DiffMatchPatch = require('diff-match-patch');
    this.dmp = new DiffMatchPatch();


    this.editor = document.getElementById('editor');
    this.editor.focus();

    this.client_shadow = this.editor.innerHTML;

    this.editor.addEventListener('input', (e) => {
      let write = e.target.innerHTML;


      let d = this.dmp.diff_main(this.client_shadow, write);
      this.dmp.diff_cleanupSemantic(d);
      let ds = this.dmp.diff_prettyHtml(d);
      this.differ.innerHTML = ds;

      this.client_shadow = write;

      console.log('diff ', d);
      this.editorProxy.invokeOrdered({operation: 'write', data: d}, this);
    });

    this.editorObservable = Observable.interval(300);
    this.editorSubscription = this.editorObservable.subscribe((num) => {

      if (!this.subscribed) {
        this.editorProxy.invokeOrderedSubscribe({operation: 'subscribe', data: 'onDocChange'}, this, 'onDocChange');
        this.editorProxy.invokeUnordered({operation: 'read'}, this);
        this.subscribed = true;
      }

    });

  }


  replyReceived(sm: TOMMessage) {

    let cursorPosition = this.getCurrentCursorPosition('editor');
    console.log(cursorPosition);

    if (cursorPosition != -1)
      this.cursorPosition = cursorPosition;
    let buff = new Buffer(sm.content.data);
    switch (sm.content.operation) {

      case 'write':
        if (this.editor.innerHTML != buff.toString('utf8')) {
          let diffs = buff.toString('utf8');
          console.log('Received Diffs ...', diffs);
          let patch = this.dmp.patch_make(this.editor.innerHTML, diffs);
          console.log('Applying Patch ', patch);
          this.editor.innerHTML = this.dmp.patch_apply(patch, this.editor.innerHTML)[0];
          this.client_shadow = this.editor.innerHTML;
        }
        break;
      case 'read':
        let document = buff.toString('utf8');
        this.editor.innerHTML = document;
        this.client_shadow = document;
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
}

