import { Component, OnInit } from '@angular/core';
import { JsonControllerService } from "../../generated-api";
import { MachineDTO, MachineTypeDTO } from '../../generated-api';
import { MachineTypeControllerService } from "../../generated-api";
import Swal from 'sweetalert2';
import { NgForm } from '@angular/forms';

@Component({
  selector: 'app-create-import-machine',
  templateUrl: './create-import-machine.component.html',
  styleUrls: ['./create-import-machine.component.css']
})
export class CreateImportMachineComponent implements OnInit {
  showJsonInput: boolean = false;

  machine: Omit<MachineDTO, 'id'> = {
    name: '',
    description: '',
    status: "AVAILABLE",
    typeId: 0,
  };

  statuses = ['AVAILABLE', 'BUSY', 'MAINTENANCE', 'OUT_OF_SERVICE'];

  jsonInputContent: string = '';
  jsonExample: string = '';

  machineTypes: MachineTypeDTO[] = [];

  jsonError: string = '';

  constructor(
    private jsonService: JsonControllerService,
    private machineTypeService: MachineTypeControllerService
  ) {}

  ngOnInit(): void {
    this.jsonExample =
      `[
        {
          "name": "Machine 1",
          "description": "First machine",
          "status": "AVAILABLE",
          "typeId": 1
        }
      ]`;
    this.jsonInputContent = this.jsonExample;

    this.machineTypeService.getAllMachineTypes().subscribe({
      next: (data: MachineTypeDTO[]) => {
        console.log('Data:', data);
        this.machineTypes = data;
      },
      error: (error) => {
        console.error('Errore durante il recupero dei Machine Types:', error);
        Swal.fire({
          icon: 'error',
          title: 'Errore',
          text: 'Non è stato possibile recuperare i Machine Types.',
        });
      }
    });
  }

  toggleJsonInput() {
    this.showJsonInput = !this.showJsonInput;
    this.jsonError = '';
    if (!this.showJsonInput) {
      this.resetForm();
    }
  }

  validateJsonInput() {
    try {
      JSON.parse(this.jsonInputContent);
      this.jsonError = '';
    } catch (e) {
      this.jsonError = 'Il JSON inserito non è valido.';
    }
  }

  submitMachine(form?: NgForm) {
    console.log('submitMachine called');

    if (!this.showJsonInput && form) {
      if (!form.valid) {
        Object.keys(form.controls).forEach(field => {
          const control = form.controls[field];
          control.markAsTouched({ onlySelf: true });
        });

        Swal.fire({
          icon: 'error',
          title: 'Errore',
          text: 'Per favore, compila tutti i campi obbligatori.'
        });
        return;
      }
    }

    let machinesToSubmit: Array<Omit<MachineDTO, 'id'>>;

    if (this.showJsonInput) {
      try {
        machinesToSubmit = JSON.parse(this.jsonInputContent);

        if (!Array.isArray(machinesToSubmit)) {
          Swal.fire({
            icon: 'error',
            title: 'Errore',
            text: 'Il JSON inserito deve essere un array di Machines.',
          });
          return;
        }

      } catch (error) {
        Swal.fire({
          icon: 'error',
          title: 'Errore',
          text: 'Il JSON inserito non è valido.',
        });
        return;
      }
    } else {
      machinesToSubmit = [this.machine];
    }

    const machinesToSubmitWithId: MachineDTO[] = machinesToSubmit.map(machine => ({
      ...machine,
      id: 0
    }));

    this.jsonService.importMachine(machinesToSubmitWithId).subscribe({
      next: (response: any) => {
        const message = typeof response === 'string' ? response : 'Elemento importato con successo';
        Swal.fire({
          icon: 'success',
          title: message,
          showConfirmButton: false,
          timer: 1000
        });
        this.resetForm();
        if (form) {
          form.resetForm();
        }
      },
      error: (error) => {
        if (error.status === 200 && error.error && error.error.text) {
          Swal.fire({
            icon: 'success',
            title: error.error.text,
            showConfirmButton: false,
            timer: 1000
          });
          this.resetForm();
          if (form) {
            form.resetForm();
          }
          return;
        }

        console.error("Errore durante l'importazione delle Machines:", error);
        Swal.fire({
          icon: 'error',
          title: 'Errore',
          text: `Errore durante l'importazione delle Machines: ${error.message || JSON.stringify(error)}`
        });
      },
    });
  }

  resetForm() {
    this.machine = {
      name: '',
      description: '',
      status: "AVAILABLE",
      typeId: 0,
    };
    this.jsonInputContent = this.jsonExample;
    this.jsonError = '';
  }
}
