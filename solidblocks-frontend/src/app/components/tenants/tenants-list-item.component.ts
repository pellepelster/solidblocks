import {Component, Input} from '@angular/core';
import {Tenant} from "../../sevices/types";

@Component({
  selector: 'app-tenants-list-item',
  template: `
    <div class="card" style="width: 18rem;">
      <div class="card-body">
        <h5 class="card-title">Tenant {{tenant.name}}</h5>
        <p class="card-text">
          {{tenant.id}}
        </p>
        <a routerLink="/tenants/{{tenant.id}}" routerLinkActive="active"
           class="btn btn-primary">{{tenant.name}}</a>
      </div>
    </div>
  `
})
export class TenantsListItemComponent {

  @Input()
  tenant: Tenant

}
