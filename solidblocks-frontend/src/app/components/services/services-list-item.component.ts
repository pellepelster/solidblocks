import {Component, Input} from '@angular/core';
import {Service} from "../../sevices/types";

@Component({
  selector: 'app-services-list-item',
  template: `
    <div class="card" style="width: 18rem;">
      <div class="card-body">
        <h5 class="card-title">Environment {{service.name}}</h5>
        <p class="card-text">
          {{service.id}}
        </p>
        <a routerLink="/services/{{service.id}}" routerLinkActive="active"
           class="btn btn-primary">{{service.name}}</a>
      </div>
    </div>
  `
})
export class ServicesListItemComponent {

  @Input()
  service: Service

}
