import {RouterModule, Routes} from "@angular/router";
import {NgModule} from "@angular/core";
import {ServicesCreateComponent} from "./services-create/services-create.component";
import {ServicesComponent} from "./services.component";
import {ServicesCreateCatalogComponent} from "./services-create-catalog/services-create-catalog.component";
import {ServicesCreateConfigComponent} from "./services-create-config/services-create-config.component";

const routes: Routes = [
  {
    path: '',
    component: ServicesComponent,
    children: [
      {
        path: 'create',
        component: ServicesCreateComponent,
        children: [
          {
            path: 'catalog',
            component: ServicesCreateCatalogComponent
          },
          {
            path: 'config',
            component: ServicesCreateConfigComponent
          },
        ]
      },
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ServicesRoutingModule {
}
