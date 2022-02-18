import {ServicesHomeComponent} from "./services-home/services-home.component";
import {NgModule} from "@angular/core";
import {ServicesCreateComponent} from "./services-create/services-create.component";
import {CommonModule} from "@angular/common";
import {ServicesRoutingModule} from "./services-routing-module";
import {ServicesListItemComponent} from "./services-list-item.component";
import {ControlsModule} from "../controls/controls-module";
import {ReactiveFormsModule} from "@angular/forms";
import {ServicesComponent} from "./services.component";
import {ServicesCreateCatalogComponent} from "./services-create-catalog/services-create-catalog.component";
import {ServicesCreateConfigComponent} from "./services-create-config/services-create-config.component";
import {ServicesCatalogItemComponent} from "./services-create-catalog-item/services-catalog-item.component";

@NgModule({
  declarations: [
    ServicesComponent,
    ServicesHomeComponent,
    ServicesCreateComponent,
    ServicesCreateCatalogComponent,
    ServicesCreateConfigComponent,
    ServicesListItemComponent,
    ServicesCatalogItemComponent
  ],
  exports: [
    ServicesListItemComponent
  ],
  imports: [
    CommonModule,
    ControlsModule,
    ReactiveFormsModule,
    ServicesRoutingModule
  ]
})
export class ServicesModule {
}
