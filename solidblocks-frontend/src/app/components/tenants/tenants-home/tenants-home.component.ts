import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";

@Component({
  selector: 'app-tenants-home',
  templateUrl: './tenants-home.component.html',
})
export class TenantsHomeComponent implements OnInit, OnDestroy {

  private subscription: any;

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
