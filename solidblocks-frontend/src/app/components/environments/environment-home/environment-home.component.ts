import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Subscription} from "rxjs";
import {ToastService} from "../../../utils/toast.service";
import {EnvironmentsService} from "../../../sevices/environments.service";
import {Environment} from "../../../sevices/types";

@Component({
  selector: 'app-environment-home',
  templateUrl: './environment-home.component.html',
})
export class EnvironmentHomeComponent implements OnInit, OnDestroy {

  environment: Environment

  private subscription: Subscription;

  constructor(private route: ActivatedRoute, private environmentsService: EnvironmentsService, private toastsService: ToastService) {
  }

  ngOnInit(): void {
    this.subscription = this.route.params.subscribe(params => {
      this.environmentsService.get(params['id']).subscribe(
        (response) => {
          this.environment = response.environment
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
