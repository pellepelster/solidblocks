import {ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {BaseFormComponent} from "../components/base-form.component";
import {ToastService} from "../utils/toast.service";
import {ConfigValueDefinition} from "../sevices/types";

@Component({
  selector: 'app-services-create',
  templateUrl: './services-create.component.html',
})
export class ServicesCreateComponent extends BaseFormComponent implements OnInit {

  controls: Array<ConfigValueDefinition> = []

  form = new FormGroup({});

  constructor(toastService: ToastService) {
    super(toastService);

    const element1: ConfigValueDefinition = {
      name: "xxx",
      type: "text"
    };

    const element2: ConfigValueDefinition = {
      name: "yyy",
      type: "text"
    };

    this.controls = [element1, element2]

    this.controls.forEach(control => {
      this.form.addControl(control.name, new FormControl('', [Validators.required]))
    })
  }

  ngOnInit(): void {
  }

  onSubmit() {
  }

}
