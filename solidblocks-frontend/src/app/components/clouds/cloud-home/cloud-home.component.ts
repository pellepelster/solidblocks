import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Subscription} from "rxjs";
import {CloudsService} from "../../../sevices/clouds.service";
import {Cloud} from "../../../sevices/types";
import {ToastService} from "../../../utils/toast.service";

@Component({
  selector: 'app-cloud-home',
  templateUrl: './cloud-home.component.html',
})
export class CloudHomeComponent implements OnInit, OnDestroy {

  cloud: Cloud

  private subscription: Subscription;

  constructor(private route: ActivatedRoute, private cloudsService: CloudsService, private toastsService: ToastService) {
  }

  ngOnInit(): void {
    this.subscription = this.route.params.subscribe(params => {
      this.cloudsService.get(params['id']).subscribe(
        (response) => {
          this.cloud = response.cloud
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
