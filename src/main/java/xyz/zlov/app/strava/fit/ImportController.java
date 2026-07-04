package xyz.zlov.app.strava.fit;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import xyz.zlov.app.strava.activity.dto.TrainerRideDto;

import java.io.IOException;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final FitImportService fitImportService;

    @PostMapping(value = "/fit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TrainerRideDto importFit(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "stravaId", required = false) Long stravaId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description) {

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Файл пустой");
        }
        long id = stravaId != null ? stravaId : parseIdFromFilename(file.getOriginalFilename());
        try {
            return fitImportService.importFit(file.getBytes(), id, name, description);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось прочитать файл", e);
        } catch (FitParser.FitParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /** Strava bulk-export names files {activityId}.fit(.gz); derive the id from the filename. */
    private long parseIdFromFilename(String filename) {
        if (filename == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нет имени файла");
        }
        String base = filename;
        if (base.toLowerCase().endsWith(".gz")) base = base.substring(0, base.length() - 3);
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        try {
            return Long.parseLong(base.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Имя файла должно быть идентификатором тренировки (например 19166201899.fit), либо передайте stravaId");
        }
    }
}
