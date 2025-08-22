package coop.bancocredicoop.omnited.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/audio")
public class AudioController {

  private final Path audioBasePath = Paths.get("/outputs/");

  @GetMapping("/{fileName}")
  public ResponseEntity<Resource> getAudio(@PathVariable String fileName) throws MalformedURLException {
    Path filePath = audioBasePath.resolve(fileName).normalize();
    Resource resource = new UrlResource(filePath.toUri());

    if (!resource.exists()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
        .body(resource);
  }
}

