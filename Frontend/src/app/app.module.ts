import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { ReactiveFormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './components/login/login.component';
import { HomeComponent } from './components/home/home.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { HeaderComponent } from './components/header/header.component';
import { NgOptimizedImage } from '@angular/common';
import { CreateComponent } from './components/create/create.component';
import { ScheduleComponent } from './components/schedule/schedule.component';
import { GraphsComponent } from './components/graphs/graphs.component';
import { PageCardComponent } from './components/page-card/page-card.component';
import { CreateImportJobComponent } from "./components/create-import-job/create-import-job.component";
import { CreateImportMachineComponent } from './components/create-import-machine/create-import-machine.component';
import { CreateImportMachineTypeComponent } from './components/create-import-machine-type/create-import-machine-type.component';
import { ViewExportDeleteJobsComponent } from './components/view-export-delete-jobs/view-export-delete-jobs.component';
import { ViewExportDeleteMachinesComponent } from './components/view-export-delete-machines/view-export-delete-machines.component';
import { ViewExportDeleteMachineTypesComponent } from './components/view-export-delete-machine-types/view-export-delete-machine-types.component';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import {MatIconModule} from "@angular/material/icon";
import { MatDialogModule } from '@angular/material/dialog';
import {MatMenuModule, MatMenuTrigger} from "@angular/material/menu";
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {MatButtonModule} from "@angular/material/button";
import { EditJobDialogComponent } from './components/edit-job-dialog/edit-job-dialog.component';
import { EditMachineTypesDialogComponent } from './components/edit-machine-types-dialog/edit-machine-types-dialog.component';
import { EditMachineDialogComponent } from './components/edit-machine-dialog/edit-machine-dialog.component';

import {
  MatDatepicker,
  MatDatepickerInput,
  MatDatepickerModule,
  MatDatepickerToggle
} from "@angular/material/datepicker";
import {MatNativeDateModule} from "@angular/material/core";
import {MatTableModule} from "@angular/material/table";
import {ShowJobComponent} from "./components/show-job/show-job.component";
import { GoogleChartsModule } from 'angular-google-charts';
import {
  JobControllerService, JsonControllerService, MachineControllerService,
  MachineTypeControllerService,
  ScheduleControllerService,
  UserControllerService,
  SchedulerEngineService
} from "./generated-api";
import {MatRadioButton, MatRadioGroup} from "@angular/material/radio";
import {MatProgressSpinner} from "@angular/material/progress-spinner";
import { ThemeToggleComponent } from './components/theme-toggle/theme-toggle.component';
import {ThemeService} from "./services/theme.service";

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    HomeComponent,
    NavbarComponent,
    HeaderComponent,
    CreateComponent,
    ScheduleComponent,
    GraphsComponent,
    PageCardComponent,
    CreateImportJobComponent,
    CreateImportMachineComponent,
    CreateImportMachineTypeComponent,
    ViewExportDeleteJobsComponent,
    ViewExportDeleteMachinesComponent,
    ViewExportDeleteMachineTypesComponent,
    ShowJobComponent,
    EditJobDialogComponent,
    EditMachineTypesDialogComponent,
    EditMachineDialogComponent,
    ThemeToggleComponent,
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    AppRoutingModule,
    NgOptimizedImage,
    CommonModule,
    FormsModule,
    BrowserAnimationsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatMenuModule,
    MatMenuTrigger,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
    MatDatepickerToggle,
    MatDatepicker,
    MatDatepickerInput,
    MatTableModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    GoogleChartsModule.forRoot(),
    MatRadioGroup,
    MatRadioButton,
    MatProgressSpinner,
  ],
  providers: [
    ThemeService,
    MachineTypeControllerService,
    UserControllerService,
    JobControllerService,
    ScheduleControllerService,
    JsonControllerService,
    MachineControllerService,
    SchedulerEngineService,
    provideAnimationsAsync()],
  bootstrap: [AppComponent]
})
export class AppModule { }
