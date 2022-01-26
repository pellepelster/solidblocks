import {Component, OnInit} from '@angular/core';
import {TenantsService} from "../../sevices/tenants.service";
import {Tenant} from "../../sevices/types";

@Component({
  selector: 'app-console-home',
  templateUrl: './console-home.component.html',
})
export class ConsoleHomeComponent implements OnInit {

  tenants: Array<Tenant> = []

  constructor(private tenantsService: TenantsService) {
  }

  ngOnInit(): void {

    this.tenantsService.list().subscribe(
      (response) => {
        this.tenants = response.tenants
      },
      (errors) => {
        console.log(errors)
      },
    )
  }
}
