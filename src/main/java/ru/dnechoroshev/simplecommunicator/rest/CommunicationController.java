package ru.dnechoroshev.simplecommunicator.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.dnechoroshev.simplecommunicator.model.ConnectionDto;
import ru.dnechoroshev.simplecommunicator.server.CommunicationDispatcher;

@RestController
@RequestMapping("/connection")
@Slf4j
@RequiredArgsConstructor
public class CommunicationController {

    private final CommunicationDispatcher dispatcher;

    @GetMapping("/requestCall")
    public ConnectionDto startCall(@RequestParam String caller, @RequestParam String callee) {
        return new ConnectionDto(dispatcher.startCall(caller, callee), callee);
    }

    @GetMapping("/checkInvocation")
    public ResponseEntity<ConnectionDto> checkInvocation(@RequestParam String callee) {
        String caller = dispatcher.checkIncomingCall(callee);
        if (caller != null) {
            return new ResponseEntity<>(new ConnectionDto(dispatcher.acceptCall(callee), callee), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/terminate-call")
    public void killConnection(String caller) {
        dispatcher.endCall(caller, null);
    }

    @DeleteMapping("/terminate-all")
    public void killAllConnections() {
        dispatcher.stopAllCommunications();
    }

}
