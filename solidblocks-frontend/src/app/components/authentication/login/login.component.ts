import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {LoginService} from "../../../authentication/login.service";
import {ToastService} from "../../../utils/toast.service";
import {Router} from "@angular/router";
import {BaseFormComponent} from "../../base-form.component";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
})
export class LoginComponent extends BaseFormComponent implements OnInit {

  constructor(private loginService: LoginService, toastService: ToastService, private router: Router) {
    super(toastService);
  }

  form = new FormGroup({
    email: new FormControl('', [Validators.required]),
    password: new FormControl('', [Validators.required])
  });

  ngOnInit(): void {
  }

  onSubmit() {
    this.loginService.login(this.form.value.email, this.form.value.password).then(
      (user) => {
        switch (user.scope) {
          case "root":
            this.router.navigate(["/clouds/home"]);
            break
          case "cloud":
            this.router.navigate(["/environments/home"]);
            break
          case "environment":
            this.router.navigate(["/tenants/home"]);
            break
          case "tenant":
            this.router.navigate(["/services/home"]);
            break
        }
      },
      (error) => {
        this.handleErrorResponse(error)
      }
    )
  }

}
