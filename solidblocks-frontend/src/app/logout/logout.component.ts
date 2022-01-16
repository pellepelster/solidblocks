import {Component, OnInit} from '@angular/core';
import {LoginService} from "../authentication/login.service";

@Component({
  selector: 'app-logout',
  templateUrl: './logout.component.html',
})
export class LogoutComponent implements OnInit {

  constructor(private loginService: LoginService) {
  }

  ngOnInit(): void {
    this.loginService.logout()
  }

}
