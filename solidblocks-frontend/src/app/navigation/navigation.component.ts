import { Component, OnInit } from '@angular/core';
import {LoginService} from "../authentication/login.service";

@Component({
  selector: 'app-navigation',
  templateUrl: './navigation.component.html',
})
export class NavigationComponent implements OnInit {

  constructor(private loginService: LoginService) { }

  hasValidUser() {
    return this.loginService.hasValidUser()
  }

  ngOnInit(): void {
  }

}
