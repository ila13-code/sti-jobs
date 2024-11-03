package unical.demacs.rdm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import unical.demacs.rdm.config.exception.MachineException;
import unical.demacs.rdm.config.exception.UserException;
import unical.demacs.rdm.persistence.dto.JsonDTO;
import unical.demacs.rdm.persistence.service.implementation.JsonServiceImpl;
import unical.demacs.rdm.persistence.service.interfaces.IJsonService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/json", produces = "application/json")
@CrossOrigin
@AllArgsConstructor
@Tag(name = "json-controller", description = "Operations related to JSON import and export.")
public class JsonController {

    private final JsonServiceImpl jsonServiceImpl;
    @Autowired
    private IJsonService jsonService;

    @Operation(summary = "Import data from JSON", description = "Import data into the system from JSON content.",
            tags = {"json-controller"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data imported successfully.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Invalid JSON format or content.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Server error. Please try again later.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/import")
    public ResponseEntity<Map<String, String>> importJson(@RequestBody JsonDTO jsonDTO) {
        try {
            System.out.println(jsonDTO);
            jsonService.processImport(jsonDTO);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Importazione completata con successo");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Errore durante l'importazione: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    @Operation(summary = "Export user-specific data to JSON file", description = "Download user-specific data as a JSON file.",
            tags = {"json-controller"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data exported successfully.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = JsonDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Server error. Please try again later.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/export")
    public ResponseEntity<Resource> exportJsonFile(@RequestParam("email") String email) {
        try {
            JsonDTO jsonDTO = jsonServiceImpl.processExport(email);
            byte[] data = jsonServiceImpl.convertToJson(jsonDTO);

            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"user_data_export.json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(resource);
        } catch (UserException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            throw new MachineException("Failed to export data: " + e.getMessage());
        }
    }


}
