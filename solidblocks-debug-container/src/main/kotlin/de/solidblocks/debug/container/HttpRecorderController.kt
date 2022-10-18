package de.solidblocks.debug.container

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.*
import javax.servlet.http.HttpServletRequest

@Controller
class HttpRecorderController {

    @RequestMapping("/http-recorder/record")
    fun record(httpRequest: HttpServletRequest): ResponseEntity<String> {
        println(httpRequest.method)

        return ResponseEntity.of(Optional.empty())
    }

}