package ru.dnechoroshev.simplecommunicator.debug;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.dnechoroshev.simplecommunicator.config.DebugConfiguration;
import ru.dnechoroshev.simplecommunicator.model.ConnectionDto;
import ru.dnechoroshev.simplecommunicator.server.CommunicationDispatcher;

@Service
@Slf4j
@RequiredArgsConstructor
public class DummyCallAcceptor {

    private final CommunicationDispatcher dispatcher;

    private final DebugConfiguration debugConfiguration;

    private final DummyConnectionHandler dummyConnectionHandler;

    @Scheduled(fixedRate = 3000)
    public void checkIncomingCall() {
        if (debugConfiguration.isEnableAutoResponse()) {
            String caller = dispatcher.checkIncomingCall(debugConfiguration.getAutoResponseCallee());
            if (StringUtils.isNotBlank(caller)) {
                log.info("Call from {} to {}", caller, debugConfiguration.getAutoResponseCallee());
                int port = dispatcher.acceptCall(debugConfiguration.getAutoResponseCallee());
                log.info("Port assigned: {}", port);
                dummyConnectionHandler.handleConnection(new ConnectionDto(port, caller));
            }
        }
    }


}
