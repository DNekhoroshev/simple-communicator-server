package ru.dnechoroshev.simplecommunicator.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.dnechoroshev.simplecommunicator.debug.DummyCaller;

@RestController
@RequestMapping("/connection-test")
@RequiredArgsConstructor
public class ConnectionTestController {

    private final DummyCaller dummyCaller;

    @GetMapping
    public void call(@RequestParam String callee) {
        dummyCaller.fireTestCall(callee);
    }

}
