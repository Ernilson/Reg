//package br.com.menu.ragController;
//
//
//import br.com.menu.repository.VectorRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//import br.com.menu.VectorTestService;
//
//
//import java.util.List;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/vector")
//public class VectorController {
//
//    private final VectorTestService service;
//
//    @PostMapping("/seed")
//    public String seed() {
//        service.seed();
//        return "ok";
//    }
//
//    @GetMapping("/search")
//    public List<VectorRepository.SearchResult> search(@RequestParam String q) {
//        return service.search(q);
//    }
//}
