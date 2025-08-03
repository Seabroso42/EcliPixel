package controller;

@RestController
public class ProcessamentoController {

     @PostMapping(value = "/processar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> processarImagem(@RequestParam("image") MultipartFile file) throws IOException {
        //  faz o processamento da imagem (por enquanto so reenvia a original)
        byte[] original = file.getBytes();

        // Exemplo: poderia aplicar filtro, converter etc.
        // TODO: adicionar lógica de processamento real

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(original);
    }
    
}
