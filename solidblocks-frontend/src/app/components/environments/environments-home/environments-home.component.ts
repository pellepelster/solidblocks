import {Component, OnInit} from '@angular/core';
import {ToastService} from "../../../utils/toast.service";
import {EnvironmentsService} from "../../../sevices/environments.service";
import {Environment} from "../../../sevices/types";

@Component({
  selector: 'app-environments-home',
  templateUrl: './environments-home.component.html',
})
export class EnvironmentsHomeComponent implements OnInit {

  environments: Array<Environment> = []

  constructor(private environmentsService: EnvironmentsService, private toastsService: ToastService) {
  }

  ngOnInit(): void {
    this.environmentsService.list().subscribe(
      (response) => {
        this.environments = response.environments
      },
      (error) => {
        this.toastsService.handleErrorResponse(error)
      },
    )
  }
}
