import {Component, OnInit} from '@angular/core';
import {CloudsService} from "../../sevices/clouds.service";

@Component({
  selector: 'app-console-home',
  templateUrl: './console-home.component.html',
})
export class ConsoleHomeComponent implements OnInit {

  constructor(private cloudsService: CloudsService) {
  }

  ngOnInit(): void {

    this.cloudsService.list().subscribe(
      (next) => {
        console.log(next)
      },
      (errors) => {
        console.log(errors)
      },
    )
  }
}
