/**
 * OpenAPI definition
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: v0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

export interface ScheduleDTO { 
    id?: number;
    jobId?: number;
    machineTypeId?: number;
    dueDate?: Date;
    startTime?: Date;
    duration?: number;
    status?: ScheduleDTO.StatusEnum;
    assignedMachineId?: number;
    assignedMachineName?: string;
}
export namespace ScheduleDTO {
    export type StatusEnum = 'PENDING' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
    export const StatusEnum = {
        PENDING: 'PENDING' as StatusEnum,
        SCHEDULED: 'SCHEDULED' as StatusEnum,
        INPROGRESS: 'IN_PROGRESS' as StatusEnum,
        COMPLETED: 'COMPLETED' as StatusEnum,
        CANCELLED: 'CANCELLED' as StatusEnum
    };
}