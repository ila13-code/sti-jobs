package unical.demacs.rdm.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.*;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicType;
import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig;
import org.optaplanner.core.config.localsearch.decider.acceptor.LocalSearchAcceptorConfig;
import org.optaplanner.core.config.localsearch.decider.forager.LocalSearchForagerConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.springframework.stereotype.Service;
import unical.demacs.rdm.config.ModelMapperExtended;
import unical.demacs.rdm.persistence.dto.ScheduleWithMachineDTO;
import unical.demacs.rdm.persistence.entities.*;
import unical.demacs.rdm.persistence.enums.MachineStatus;
import unical.demacs.rdm.persistence.enums.ScheduleStatus;
import unical.demacs.rdm.persistence.repository.*;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class Scheduler {
    private final ScheduleRepository scheduleRepository;
    private final MachineRepository machineRepository;
    private final ModelMapperExtended modelMapperExtended;
    private final ObjectMapper objectMapper;

    private static final long SECONDS_SPENT_LIMIT = 30L;

    public void scheduleByEveryType() {
        log.debug("Inizio schedulazione per ogni tipo");
        scheduleByPriority();
        scheduleByDueDate();
        scheduleByDuration();
    }

    public void scheduleByPriority() {
        log.debug("Schedulazione basata sulla priorità");
        List<Schedule> schedules = scheduleRepository.findAll();
        log.debug("Schedules recuperate: {}", schedules.size());
        List<Schedule> priorityResult = scheduleWithOptaPlanner(schedules, "priority");
        saveSchedulesToFile(priorityResult, "priority");
    }

    public void scheduleByDueDate() {
        log.debug("Schedulazione basata sulla data di scadenza");
        List<Schedule> schedules = scheduleRepository.findAll();
        log.debug("Schedules recuperate: {}", schedules.size());
        List<Schedule> dueDateResult = scheduleWithOptaPlanner(schedules, "due-date");
        saveSchedulesToFile(dueDateResult, "due-date");
    }

    public void scheduleByDuration() {
        log.debug("Schedulazione basata sulla durata");
        List<Schedule> schedules = scheduleRepository.findAll();
        log.debug("Schedules recuperate: {}", schedules.size());
        List<Schedule> durationResult = scheduleWithOptaPlanner(schedules, "duration");
        saveSchedulesToFile(durationResult, "duration");
    }

    // -------------------- SCHEDULE LOGIC 'NAGG RO' CAZZ --------------------

    private List<JobAssignment> createPossibleAssignments(List<Schedule> schedules, List<Machine> availableMachines) {
        log.debug("Creazione delle possibili assegnazioni");
        List<JobAssignment> assignments = new ArrayList<>();
        log.debug("Numero di schedules: {}, Macchine disponibili: {}", schedules.size(), availableMachines.size());

        for (Schedule schedule : schedules) {
            JobAssignment assignment = new JobAssignment(schedule);
            assignments.add(assignment);
            log.debug("Creata assegnazione per schedule {} con macchine disponibili", schedule.getId());
        }

        log.debug("Numero totale di assegnazioni: {}", assignments.size());
        return assignments;
    }

    private TimeWindow calculateTimeWindow(List<Schedule> schedules) {
        log.debug("Calcolo della finestra temporale");
        if (schedules.isEmpty()) {
            throw new IllegalArgumentException("Nessuna schedule fornita per il calcolo della finestra temporale");
        }

        LocalDateTime earliestStart = schedules.stream()
                .map(Schedule::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElseGet(LocalDateTime::now);
        log.debug("Earliest start time: {}", earliestStart);

        LocalDateTime latestDue = schedules.stream()
                .map(Schedule::getDueDate)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElseGet(() -> earliestStart.plusDays(7));
        log.debug("Latest due date: {}", latestDue);

        long startTime = earliestStart.toEpochSecond(ZoneOffset.UTC);
        long endTime = latestDue.toEpochSecond(ZoneOffset.UTC);

        log.info("Finestra temporale: {} - {}",
                earliestStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                latestDue.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return new TimeWindow(startTime, endTime);
    }

    private List<TimeGrain> createTimeGrains(TimeWindow timeWindow) {
        log.debug("Creazione dei TimeGrains");
        List<TimeGrain> timeGrains = new ArrayList<>();
        int grainIndex = 0;
        long currentTime = timeWindow.getStartTime();
        int grainLengthInSeconds = 60;
        log.debug("TimeWindow start: {}, end: {}", timeWindow.getStartTime(), timeWindow.getEndTime());

        while (currentTime <= timeWindow.getEndTime()) {
            timeGrains.add(new TimeGrain(grainIndex++, currentTime));
            log.debug("Creato TimeGrain index: {}, startTimeInSeconds: {}", grainIndex - 1, currentTime);
            currentTime += grainLengthInSeconds;
        }

        log.info("Creati {} TimeGrains", timeGrains.size());
        return timeGrains;
    }

    private List<Schedule> processSolution(ScheduleSolution solution) {
        if (solution == null) {
            log.error("Soluzione non valida");
            return Collections.emptyList();
        }

        log.info("Elaborazione della soluzione con score: {}", solution.getScore());
        List<Schedule> finalSchedules = new ArrayList<>();

        for (JobAssignment assignment : solution.getJobAssignments()) {
            Schedule schedule = assignment.getSchedule();
            
            if (assignment.getAssignedMachine() != null && assignment.getStartTimeGrain() != null) {
                LocalDateTime startTime = LocalDateTime.ofEpochSecond(
                        assignment.getStartTimeGrain().getStartTimeInSeconds(),
                        0,
                        ZoneOffset.UTC
                );

                schedule.setStartTime(startTime);
                schedule.setMachineType(assignment.getAssignedMachine().getMachine_type_id());
                schedule.setMachine(assignment.getAssignedMachine());
                schedule.setStatus(ScheduleStatus.SCHEDULED);
                finalSchedules.add(schedule);
                log.debug("Schedule {} assegnata alla macchina {} all'orario {}", 
                    schedule.getId(), assignment.getAssignedMachine().getId(), startTime);
            } else {
                log.warn("Job {} non assegnato, ritento con configurazione più permissiva", schedule.getId());
                // Qui potresti implementare una logica di fallback per i job non assegnati
            }
        }

        return finalSchedules;
    }

    private ScheduleConstraintConfiguration configureConstraints(String criterion) {
        ScheduleConstraintConfiguration config = new ScheduleConstraintConfiguration();

        switch (criterion) {
            case "priority":
                config.setHighPriorityJobsFirst(HardSoftScore.ofSoft(1000));
                config.setJobDueDate(HardSoftScore.ofHard(1));
                config.setMachineConflict(HardSoftScore.ofHard(1));
                config.setShortDurationJobsFirst(HardSoftScore.ofSoft(1));
                config.setBalanceMachineLoad(HardSoftScore.ofSoft(500));
                break;
            case "due-date":
                config.setJobDueDate(HardSoftScore.ofHard(1000));
                config.setHighPriorityJobsFirst(HardSoftScore.ofSoft(1));
                config.setMachineConflict(HardSoftScore.ofHard(1));
                config.setShortDurationJobsFirst(HardSoftScore.ofSoft(1));
                config.setBalanceMachineLoad(HardSoftScore.ofSoft(500));
                break;
            case "duration":
                config.setShortDurationJobsFirst(HardSoftScore.ofSoft(1000));
                config.setHighPriorityJobsFirst(HardSoftScore.ofSoft(1));
                config.setJobDueDate(HardSoftScore.ofHard(1));
                config.setMachineConflict(HardSoftScore.ofHard(1));
                config.setBalanceMachineLoad(HardSoftScore.ofSoft(500));
                break;
            default:
                throw new IllegalArgumentException("Unknown scheduling criterion: " + criterion);
        }

        return config;
    }

    private List<Schedule> scheduleWithOptaPlanner(List<Schedule> schedules, String criterion) {
        log.info("Starting scheduling process for criterion: {}", criterion);

        List<Schedule> validSchedules = schedules.stream()
                .filter(s -> s.getStartTime() != null)
                .filter(s -> s.getStatus() != ScheduleStatus.COMPLETED)
                .collect(Collectors.toList());

        if (validSchedules.isEmpty()) {
            log.warn("No valid schedules to process");
            return Collections.emptyList();
        }

        List<Machine> availableMachines = machineRepository.findAll().stream()
                .filter(m -> !(m.getStatus().equals(MachineStatus.BUSY)))
                .collect(Collectors.toList());

        if (availableMachines.isEmpty()) {
            log.error("No available machines found");
            return Collections.emptyList();
        }

        List<JobAssignment> jobAssignments = createPossibleAssignments(validSchedules, availableMachines);
        TimeWindow timeWindow = calculateTimeWindow(validSchedules);
        List<TimeGrain> timeGrainRange = createTimeGrains(timeWindow);
        ScheduleConstraintConfiguration constraintConfiguration = configureConstraints(criterion);

        ScheduleSolution solution = createAndSolveProblem(
                jobAssignments,
                availableMachines,
                timeGrainRange,
                constraintConfiguration
        );
        return processSolution(solution);
    }

    private SolverConfig createSolverConfig() {
        return new SolverConfig()
                .withSolutionClass(ScheduleSolution.class)
                .withEntityClasses(JobAssignment.class)
                .withConstraintProviderClass(ScheduleConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig()
                        .withBestScoreLimit("0hard/*soft")  // Modificato per accettare solo soluzioni valide
                        .withSecondsSpentLimit(120L))       // Aumentato a 2 minuti
                .withPhases(
                        new ConstructionHeuristicPhaseConfig()
                                .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT_DECREASING),
                        new LocalSearchPhaseConfig()
                                .withAcceptorConfig(new LocalSearchAcceptorConfig()
                                        .withLateAcceptanceSize(1000)    // Aumentato
                                        .withEntityTabuSize(10))         // Aumentato
                                .withForagerConfig(new LocalSearchForagerConfig()
                                        .withAcceptedCountLimit(8))      // Aumentato
                );
    }

    private ScheduleSolution createAndSolveProblem(
            List<JobAssignment> jobAssignments,
            List<Machine> machines,
            List<TimeGrain> timeGrainRange,
            ScheduleConstraintConfiguration constraintConfiguration) {

        log.debug("Creazione del problema di schedulazione");
        log.debug("Numero di jobAssignments: {}", jobAssignments.size());
        log.debug("Numero di macchine: {}", machines.size());
        log.debug("Numero di timeGrains: {}", timeGrainRange.size());
        log.debug("Configurazione vincoli: {}", constraintConfiguration);

        ScheduleSolution problem = new ScheduleSolution(
                jobAssignments,
                machines,
                timeGrainRange,
                constraintConfiguration
        );

        SolverFactory<ScheduleSolution> solverFactory = SolverFactory.create(createSolverConfig());
        Solver<ScheduleSolution> solver = solverFactory.buildSolver();

        try {
            log.info("Avvio del solver...");
            ScheduleSolution solution = solver.solve(problem);
            log.info("Solver terminato con score: {}", solution.getScore());
            return solution;
        } catch (Exception e) {
            log.error("Solver fallito con errore: {}", e.getMessage(), e);
            throw new RuntimeException("Impossibile risolvere il problema di schedulazione", e);
        }
    }

    private void saveSchedulesToFile(List<Schedule> schedules, String type) {
        if (schedules.isEmpty()) {
            log.warn("No schedules to save for type: {}", type);
            return;
        }

        List<ScheduleWithMachineDTO> scheduleDTOs = schedules.stream()
                .map(schedule -> {
                    ScheduleWithMachineDTO dto = modelMapperExtended.map(schedule, ScheduleWithMachineDTO.class);
                    dto.setJobId(schedule.getJob().getId());
                    dto.setMachineTypeId(schedule.getMachineType().getId());
                    if (schedule.getMachine() != null) {
                        dto.setMachineId(schedule.getMachine().getId());
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        String fileName = "./data/job-scheduled-by-" + type + ".json";        File dataDir = new File("./data");        if (!dataDir.exists() && !dataDir.mkdirs()) {            log.error("Failed to create directory: {}", dataDir.getAbsolutePath());            throw new RuntimeException("Unable to create data directory");        }        try {            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(fileName), scheduleDTOs);            log.info("Successfully saved {} schedules to {}", schedules.size(), fileName);        } catch (IOException e) {            log.error("Failed to write schedule to file: {}", fileName, e);            throw new RuntimeException("Failed to save schedule to file", e);        }    }}