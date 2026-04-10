package ru.izpz.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.izpz.web.service.ProjectDirectoryFacade;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebPageController {

    private final ProjectDirectoryFacade projectDirectoryFacade;

    @GetMapping("/")
    public String index(Model model) {
        try {
            model.addAttribute("projectNames", projectDirectoryFacade.getProjectNames());
        } catch (Exception ex) {
            log.warn("Не удалось загрузить список проектов для главной страницы", ex);
            model.addAttribute("projectNames", List.of());
            model.addAttribute("loadError", "Список проектов временно недоступен.");
        }
        return "index";
    }
}
