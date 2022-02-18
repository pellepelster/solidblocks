import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Subscription} from "rxjs";

@Component({
  selector: 'app-tenants-home',
  templateUrl: './tenants-home.component.html',
})
export class TenantsHomeComponent implements OnInit, OnDestroy {

  private subscription: Subscription;

  constructor(private route: ActivatedRoute) {
  }

  ngOnInit() {
    this.subscription = this.route.params.subscribe(params => {
      console.log(params['id'])
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

}
