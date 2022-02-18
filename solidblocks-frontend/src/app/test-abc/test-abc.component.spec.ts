import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TestAbcComponent } from './test-abc.component';

describe('TestAbcComponent', () => {
  let component: TestAbcComponent;
  let fixture: ComponentFixture<TestAbcComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ TestAbcComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TestAbcComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
