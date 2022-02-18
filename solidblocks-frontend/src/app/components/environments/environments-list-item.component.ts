import {Component, Input} from '@angular/core';
import {Environment} from "../../sevices/types";

@Component({
  selector: 'app-environments-list-item',
  template: `
    <div class="card" style="width: 18rem;">
      <div class="card-body">
        <h5 class="card-title">Environment {{environment.name}}</h5>
        <p class="card-text">
          {{environment.id}}
        </p>
        <a routerLink="/environments/{{environment.id}}" routerLinkActive="active"
           class="btn btn-primary">{{environment.name}}</a>
      </div>
    </div>
  `
})
export class EnvironmentsListItemComponent {

  @Input()
  environment: Environment

}
