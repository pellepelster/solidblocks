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

  loginForm = new FormGroup({
    email: new FormControl('', [Validators.required]),
    password: new FormControl('', [Validators.required])
  });

  ngOnInit(): void {
  }

  onSubmit() {
    this.loginService.login(this.loginForm.value.email, this.loginForm.value.password).then(
      () => {
        this.router.navigate(["/console/home"])
      },
      (error) => {
        this.handleErrorResponse(error)
        this.loginForm.setValue({email: "", password: ""})
      }
    )
  }

}
