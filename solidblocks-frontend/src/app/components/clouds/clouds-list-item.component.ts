import {Component, Input} from '@angular/core';
import {Cloud} from "../../sevices/types";

@Component({
  selector: 'app-clouds-list-item',
  template: `
    <div class="card" style="width: 18rem;">
      <div class="card-body">
        <h5 class="card-title">Cloud {{cloud.name}}</h5>
        <p class="card-text">
          {{cloud.id}}
        </p>
        <a routerLink="/clouds/{{cloud.id}}" routerLinkActive="active" class="btn btn-primary">{{cloud.name}}</a>
      </div>
    </div>
  `
})
export class CloudsListItemComponent {

  @Input()
  cloud: Cloud

}
