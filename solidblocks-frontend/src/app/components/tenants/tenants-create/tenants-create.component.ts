import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {TenantsService} from "../../../sevices/tenants.service";
import {BaseFormComponent} from "../../base-form.component";
import {ToastService} from "../../../utils/toast.service";

@Component({
  selector: 'app-tenants-create',
  templateUrl: './tenants-create.component.html',
})
export class TenantsCreateComponent extends BaseFormComponent implements OnInit {

  form = new FormGroup({
    email: new FormControl('', [
      Validators.required
    ]),
    tenant: new FormControl('', [Validators.required])
  });

  constructor(private tenantsService: TenantsService, toastService: ToastService) {
    super(toastService);
  }

  ngOnInit(): void {
  }

  onSubmit() {
    this.tenantsService.create(this.form.value.email, this.form.value.tenant).subscribe(
      (data) => {
      },
      (error) => {
        this.handleErrorResponse(error)
      },
    )
  }
}
