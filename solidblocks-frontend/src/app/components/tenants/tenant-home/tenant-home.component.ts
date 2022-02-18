import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Subscription} from "rxjs";
import {ToastService} from "../../../utils/toast.service";
import {Tenant} from "../../../sevices/types";
import {TenantsService} from "../../../sevices/tenants.service";

@Component({
  selector: 'app-tenant-home',
  templateUrl: './tenant-home.component.html',
})
export class TenantHomeComponent implements OnInit, OnDestroy {

  tenant: Tenant

  private subscription: Subscription;

  constructor(private route: ActivatedRoute, private tenantsService: TenantsService, private toastsService: ToastService) {
  }

  ngOnInit(): void {
    this.subscription = this.route.params.subscribe(params => {
      this.tenantsService.get(params['id']).subscribe(
        (response) => {
          this.tenant = response.tenant
        },
        (error) => {
          this.toastsService.handleErrorResponse(error)
        },
      )
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

}
