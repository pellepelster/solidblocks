import {Component, OnInit} from '@angular/core';
import {ServicesService} from "../../../sevices/services.service";
import {ToastService} from "../../../utils/toast.service";
import {ServiceCatalogResponse} from "../../../sevices/types";

@Component({
  selector: 'app-services-create-catalog',
  templateUrl: './services-create-catalog.component.html',
})
export class ServicesCreateCatalogComponent implements OnInit {

  catalog: ServiceCatalogResponse

  selectedType: string

  constructor(private servicesService: ServicesService, private toastsService: ToastService) {
  }

  ngOnInit(): void {
    this.servicesService.catalog().subscribe(
      (response) => {
        this.catalog = response
      },
      (error) => {
        this.toastsService.handleErrorResponse(error)
      },
    )
  }

  selectItem(event: string) {
    this.selectedType = event
  }
}
