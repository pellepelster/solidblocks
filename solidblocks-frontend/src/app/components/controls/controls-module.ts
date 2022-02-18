import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {InputControlComponent} from "./input-control.component";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";

@NgModule({
  declarations: [
    InputControlComponent,
  ],
  exports: [
    InputControlComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
  ]
})
export class ControlsModule {
}
