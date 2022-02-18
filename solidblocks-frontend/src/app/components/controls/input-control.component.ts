import {Component, Input} from "@angular/core";

import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from "@angular/forms";
import {MessageResponse} from "../../sevices/types";

@Component({
  selector: 'input-control',
  template: `
    <div class="mb-4">
      <label class="form-label" [for]="formControlName">{{formControlName}}</label>
      <input
        [id]="formControlName"
        [disabled]="disabled"
        [(ngModel)]="value"
        (ngModelChange)="handleChange($event)"
        [type]="type"
        class="form-control form-control-lg"/>

      <div *ngIf="control?.invalid && (control?.dirty || control?.touched)"
           class="alert alert-danger">

        <div *ngIf="control?.errors?.['required']">
          {{formControlName}} is required.
        </div>

        <div *ngIf="control?.errors?.['validationMessage']">
          {{ control?.errors?.['validationMessage']['code'] }}
        </div>
      </div>

    </div>`,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: InputControlComponent
    },
    {
      provide: NG_VALIDATORS,
      multi: true,
      useExisting: InputControlComponent
    }
  ]
})
export class InputControlComponent implements ControlValueAccessor, Validator {

  @Input()
  formControlName: string = "";

  @Input()
  form: any;

  @Input()
  type: string = "text";

  _messages: Array<MessageResponse> = []

  @Input()
  set messages(messages: any) {
    this._messages = messages;
    if (this.control != null) {
      this.form.get(this.formControlName).updateValueAndValidity()
    }
  }

  value: string = "";

  private onTouched!: Function;

  private onChanged!: Function;

  touched = false;

  disabled = false;

  handleChange(event: any) {
    this.onChanged(this.value)
    this.onTouched();
  }

  writeValue(value: string): void {
    this.value = value ?? "";
  }

  registerOnChange(fn: any): void {
    this.onChanged = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
  }

  validate(control: AbstractControl): ValidationErrors | null {
    for (let message of this._messages) {
      if (message.attribute == this.formControlName) {

        this._messages = this._messages.filter((f) => {
          return f.attribute !== this.formControlName
        })

        return {validationMessage: {code: message.code}}
      }
    }

    return null
  }

  get control() {
    return this.form.get(this.formControlName);
  }

}
