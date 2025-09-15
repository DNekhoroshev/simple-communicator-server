package ru.dnechoroshev.simplecommunicator.debug;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.dnechoroshev.simplecommunicator.config.DebugConfiguration;
import ru.dnechoroshev.simplecommunicator.model.ConnectionDto;
import ru.dnechoroshev.simplecommunicator.server.CommunicationDispatcher;


@Service
@Slf4j
@RequiredArgsConstructor
public class DummyCaller {

    private final CommunicationDispatcher dispatcher;

    private final DebugConfiguration debugConfiguration;

    private final DummyConnectionHandler dummyConnectionHandler;

    public void fireTestCall(String callee) {
        ConnectionDto dto = new ConnectionDto(
                dispatcher.startCall(debugConfiguration.getAutoResponseCallee(), callee),
                callee);

        dummyConnectionHandler.handleConnection(dto);
    }

}
