package de.solidblocks.debug.container.environment

import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class EnvironmentVariablesController {

    @GetMapping("/environment-variables", produces = [MediaType.TEXT_HTML_VALUE])
    fun environmentHtml(model: Model): String {
        model.addAttribute("environmentVariables", System.getenv())
        return "environment-variables"
    }

    @GetMapping("/environment-variables", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun environmentJson(): EnvironmentVariablesResponse {
        return EnvironmentVariablesResponse(System.getenv())
    }

}