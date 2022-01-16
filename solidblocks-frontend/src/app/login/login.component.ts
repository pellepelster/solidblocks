import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {LoginService} from "../authentication/login.service";
import {ToastService} from "../utils/toast.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
})
export class LoginComponent implements OnInit {

  constructor(private loginService: LoginService, private toastService: ToastService, private router: Router) {

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
      (errors) => {
        for (let message of errors) {
          this.toastService.show(message.code, {classname: 'bg-danger text-light', delay: 15000})
        }
        this.loginForm.setValue({email: "", password: ""})
      },
    )
  }

}
