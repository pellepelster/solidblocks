import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConsoleHomeComponent } from './console-home.component';

describe('ConsoleHomeComponent', () => {
  let component: ConsoleHomeComponent;
  let fixture: ComponentFixture<ConsoleHomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ConsoleHomeComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ConsoleHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
