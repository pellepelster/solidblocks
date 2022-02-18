import {Component, OnInit} from '@angular/core';
import {CloudsService} from "../../../sevices/clouds.service";
import {Cloud} from "../../../sevices/types";
import {ToastService} from "../../../utils/toast.service";

@Component({
  selector: 'app-clouds-home',
  templateUrl: './clouds-home.component.html'
})
export class CloudsHomeComponent implements OnInit {

  clouds: Array<Cloud> = []

  constructor(private cloudsService: CloudsService, private toastsService: ToastService) {
  }

  ngOnInit(): void {
    this.cloudsService.list().subscribe(
      (response) => {
        this.clouds = response.clouds
      },
      (error) => {
        this.toastsService.handleErrorResponse(error)
      },
    )
  }

}
