import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {IDatabaseDocument} from 'abcmap-shared';

import * as filesize from 'filesize';
import {DatetimeHelper} from '../../lib/utils/DatetimeHelper';

@Component({
  selector: 'abc-document-list-item',
  templateUrl: './document-list-item.component.html',
  styleUrls: ['./document-list-item.component.scss']
})
export class DocumentListItemComponent implements OnInit {

  dhelper = DatetimeHelper;
  filesize = filesize;

  @Input()
  document?: IDatabaseDocument;

  @Output()
  addToMap = new EventEmitter<IDatabaseDocument>();

  @Output()
  delete = new EventEmitter<IDatabaseDocument>();

  @Output()
  preview = new EventEmitter<IDatabaseDocument>();

  @Output()
  download = new EventEmitter<IDatabaseDocument>();

  constructor() {
  }

  ngOnInit() {
  }

  onAddToMapClick($event: MouseEvent) {
    this.addToMap.emit(this.document);
  }

  onDeleteClick($event: MouseEvent) {
    this.delete.emit(this.document);
  }

  onDownloadClick($event: MouseEvent) {
    this.download.emit(this.document);
  }

  onPreviewClick($event: MouseEvent) {
    this.preview.emit(this.document);
  }
}
