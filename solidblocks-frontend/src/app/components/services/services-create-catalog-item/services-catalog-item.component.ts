import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ServiceCatalogItem} from "../../../sevices/types";

@Component({
  selector: 'app-services-catalog-item',
  styleUrls: ['./services-catalog.component.css'],
  template: `
    <div (click)="selectItem()" [class.card-selected]="selected" class="card" style="width: 18rem;">
      <div class="card-body">
        <h5 class="card-title">Service {{item.type}}</h5>
        <p class="card-text">
          {{item.description}}
        </p>
      </div>
    </div>
  `
})
export class ServicesCatalogItemComponent {

  @Input()
  item: ServiceCatalogItem

  @Input()
  selected: boolean

  @Output()
  onSelect = new EventEmitter<string>();

  selectItem() {
    this.onSelect.emit(this.item.type)
  }
}
