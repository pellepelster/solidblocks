import {Component, OnInit} from '@angular/core';
import {AuthService} from "../sevices/auth.service";
import {Router} from "@angular/router";
import {WhoAmIResponse} from "../sevices/types";

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
})
export class HomeComponent implements OnInit {

  whoAmI: WhoAmIResponse | undefined;

  constructor(private authService: AuthService, private router: Router) {
  }

  ngOnInit(): void {

    /*
    this.authService.whoAmI().subscribe((data: WhoAmIResponse) => {
      console.log(data)
    }, error => {
      if (error.status == 401) {
        this.router.navigate(["/login"])
      }
    })
     */
  }
}
