import {Component, OnInit} from '@angular/core';
import {TenantsService} from "../../sevices/tenants.service";
import {Tenant} from "../../sevices/types";
import {ToastService} from "../../utils/toast.service";

@Component({
  selector: 'app-console-home',
  templateUrl: './console-home.component.html',
})
export class ConsoleHomeComponent implements OnInit {

  tenants: Array<Tenant> = []

  constructor(private tenantsService: TenantsService, private toastsService: ToastService) {
  }

  ngOnInit(): void {

    this.tenantsService.list().subscribe(
      (response) => {
        this.tenants = response.tenants
      },
      (error) => {
        this.toastsService.handleErrorResponse(error)
      },
    )
  }
}
